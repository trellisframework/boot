package net.trellisframework.data.redis.ratelimit;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.redisson.client.codec.StringCodec;

import java.util.ArrayList;
import java.util.List;

final class RateLimiterScript {

    enum Op {
        ACQUIRE("acquire"), CHECK("check"), RELEASE("release"), COOL_OFF("cooloff");

        final String mode;

        Op(String mode) {
            this.mode = mode;
        }
    }

    record Limited(String key, RateLimit limits) {
    }

    static final long DEFAULT_TTL_MILLIS = 86400_000L;
    static final long TTL_GRACE_MILLIS = 60_000L;

    static final String LUA = """
            local mode = ARGV[1]
            local now = tonumber(ARGV[2])

            local function load(key, limits)
              local raw = redis.call('GET', key)
              local stored = raw and cjson.decode(raw) or {}
              local oldWindows = stored.rates or {}
              local windows = {}
              for _, rate in ipairs(limits.rates or {}) do
                local window = nil
                for _, w in ipairs(oldWindows) do
                  if tonumber(w.duration) == tonumber(rate.duration) then window = w break end
                end
                if window == nil or now - tonumber(window.startAt) >= tonumber(window.duration) then
                  window = {duration = tonumber(rate.duration), startAt = now, used = 0}
                end
                windows[#windows + 1] = {window = window, maxRequests = tonumber(rate.maxRequests)}
              end
              local permits = {}
              local maxConcurrent = tonumber(limits.maxConcurrent) or 0
              local permitTimeout = tonumber(limits.permitTimeout) or 0
              for _, ts in ipairs(stored.acquiredTimestamps or {}) do
                if maxConcurrent <= 0 or permitTimeout <= 0 or now - tonumber(ts) < permitTimeout then
                  permits[#permits + 1] = tonumber(ts)
                end
              end
              local coolOffUntil = stored.coolOffUntil
              if coolOffUntil == cjson.null then coolOffUntil = nil end
              return {windows = windows, permits = permits, coolOffUntil = coolOffUntil and tonumber(coolOffUntil) or nil}
            end

            local function canAcquire(state, limits)
              if state.coolOffUntil and now < state.coolOffUntil then return false end
              local maxConcurrent = tonumber(limits.maxConcurrent) or 0
              if maxConcurrent > 0 and #state.permits >= maxConcurrent then return false end
              for _, entry in ipairs(state.windows) do
                if entry.window.used >= entry.maxRequests then return false end
              end
              return true
            end

            local function recordAcquire(state, limits)
              state.coolOffUntil = nil
              if (tonumber(limits.maxConcurrent) or 0) > 0 then state.permits[#state.permits + 1] = now end
              for _, entry in ipairs(state.windows) do entry.window.used = entry.window.used + 1 end
            end

            local function save(key, state, limits)
              local out = {}
              if #state.windows > 0 then
                local rates = {}
                for i, entry in ipairs(state.windows) do rates[i] = entry.window end
                out.rates = rates
              end
              if #state.permits > 0 then out.acquiredTimestamps = state.permits end
              if state.coolOffUntil then out.coolOffUntil = state.coolOffUntil end
              if next(out) == nil then
                redis.call('DEL', key)
              else
                redis.call('SET', key, cjson.encode(out), 'PX', tonumber(limits.ttl))
              end
            end

            local limits, states = {}, {}
            for i = 1, #KEYS do
              limits[i] = cjson.decode(ARGV[3 + i])
              states[i] = load(KEYS[i], limits[i])
            end

            if mode == 'acquire' or mode == 'check' then
              for i = 1, #KEYS do
                if not canAcquire(states[i], limits[i]) then return 0 end
              end
              if mode == 'acquire' then
                for i = 1, #KEYS do
                  recordAcquire(states[i], limits[i])
                  save(KEYS[i], states[i], limits[i])
                end
              end
              return 1
            elseif mode == 'release' then
              for i = 1, #KEYS do
                table.remove(states[i].permits, 1)
                save(KEYS[i], states[i], limits[i])
              end
              return 1
            elseif mode == 'cooloff' then
              for i = 1, #KEYS do
                states[i].coolOffUntil = now + tonumber(ARGV[3])
                save(KEYS[i], states[i], limits[i])
              end
              return 1
            end
            return redis.error_reply('unknown mode ' .. tostring(mode))
            """;

    private static volatile String sha;

    private RateLimiterScript() {
    }

    static boolean execute(RedissonClient client, Op op, List<Limited> targets, long now, long coolOffMillis) {
        Call call = Call.of(op, now, coolOffMillis);
        targets.forEach(call::limited);
        return call.run(client);
    }

    static final class Call {
        private final List<Object> keys = new ArrayList<>(2);
        private final List<Object> args = new ArrayList<>(5);

        static Call of(Op op, long now, long coolOffMillis) {
            var call = new Call();
            call.args.add(op.mode);
            call.args.add(Long.toString(now));
            call.args.add(Long.toString(coolOffMillis));
            return call;
        }

        Call limited(Limited target) {
            keys.add(target.key());
            args.add(limitsJson(target.limits()));
            return this;
        }

        boolean run(RedissonClient client) {
            RScript script = client.getScript(StringCodec.INSTANCE);
            Object[] argv = args.toArray();
            Long result;
            try {
                result = script.evalSha(RScript.Mode.READ_WRITE, digest(script), RScript.ReturnType.INTEGER, keys, argv);
            } catch (RedisException e) {
                if (e.getMessage() == null || !e.getMessage().contains("NOSCRIPT"))
                    throw e;
                sha = null;
                result = script.evalSha(RScript.Mode.READ_WRITE, digest(script), RScript.ReturnType.INTEGER, keys, argv);
            }
            return result != null && result == 1L;
        }
    }

    private static String digest(RScript script) {
        String current = sha;
        if (current == null) {
            current = script.scriptLoad(LUA);
            sha = current;
        }
        return current;
    }

    static long ttlMillis(RateLimit limits) {
        return limits.getRates().stream().mapToLong(r -> r.getDuration().toMillis()).max().orElse(DEFAULT_TTL_MILLIS) + TTL_GRACE_MILLIS;
    }

    static long permitTimeoutMillis(RateLimit limits) {
        return limits.getPermitTimeout() == null ? 0 : limits.getPermitTimeout().toMillis();
    }

    static String limitsJson(RateLimit limits) {
        StringBuilder sb = new StringBuilder(96).append("{\"rates\":[");
        boolean first = true;
        for (RateLimit.Rate rate : limits.getRates()) {
            if (!first) sb.append(',');
            first = false;
            sb.append("{\"duration\":").append(rate.getDuration().toMillis())
                    .append(",\"maxRequests\":").append(rate.getMaxRequests()).append('}');
        }
        return sb.append("],\"maxConcurrent\":").append(limits.getMaxConcurrent())
                .append(",\"permitTimeout\":").append(permitTimeoutMillis(limits))
                .append(",\"ttl\":").append(ttlMillis(limits))
                .append('}').toString();
    }
}
