package net.trellisframework.data.redis.ratelimit;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.redisson.client.codec.StringCodec;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class RateLimiterScript {

    enum Op {
        ACQUIRE, CHECK, RELEASE, COOL_OFF
    }

    record Limited(String key, RateLimit limits) {
    }

    private static final long DEFAULT_TTL_MILLIS = 86_400_000L;
    private static final long TTL_GRACE_MILLIS = 60_000L;

    private static final String LUA = """
            local mode, now, arg = ARGV[1], tonumber(ARGV[2]), ARGV[3]

            local function livePermits(key, limits, prune)
              if limits.permitTimeout < 0 then return redis.call('ZCARD', key .. '#p') end
              local expired = now - limits.permitTimeout
              if not prune then return redis.call('ZCOUNT', key .. '#p', string.format('(%d', expired), '+inf') end
              redis.call('ZREMRANGEBYSCORE', key .. '#p', '-inf', expired)
              return redis.call('ZCARD', key .. '#p')
            end

            local limits = {}
            for i = 1, #KEYS do limits[i] = cjson.decode(ARGV[3 + i]) end

            if mode == 'RELEASE' then
              for i, key in ipairs(KEYS) do
                livePermits(key, limits[i], true)
                redis.call('ZPOPMIN', key .. '#p')
              end
              return 1
            end
            if mode == 'COOL_OFF' then
              for _, key in ipairs(KEYS) do redis.call('SET', key .. '#c', '1', 'PX', arg) end
              return 1
            end
            for i, key in ipairs(KEYS) do
              local l = limits[i]
              if redis.call('EXISTS', key .. '#c') == 1 then return 0 end
              if l.maxConcurrent > 0 and livePermits(key, l, mode ~= 'CHECK') >= l.maxConcurrent then return 0 end
              for _, rate in ipairs(l.rates) do
                if tonumber(redis.call('GET', key .. '#w:' .. rate.duration) or 0) >= rate.maxRequests then return 0 end
              end
            end
            if mode == 'ACQUIRE' then
              for i, key in ipairs(KEYS) do
                local l = limits[i]
                if l.maxConcurrent > 0 then
                  redis.call('ZADD', key .. '#p', now, arg)
                  redis.call('PEXPIRE', key .. '#p', l.ttl)
                end
                for _, rate in ipairs(l.rates) do
                  local window = key .. '#w:' .. rate.duration
                  if redis.call('INCR', window) == 1 then redis.call('PEXPIRE', window, rate.duration) end
                end
              end
            end
            return 1
            """;

    private static volatile String sha;

    private RateLimiterScript() {
    }

    static boolean execute(RedissonClient client, Op op, List<Limited> targets, long now, long coolOffMillis) {
        RScript script = client.getScript(StringCodec.INSTANCE);
        List<Object> keys = targets.stream().<Object>map(Limited::key).toList();
        String arg = op == Op.ACQUIRE ? Long.toUnsignedString(ThreadLocalRandom.current().nextLong(), 36) : Long.toString(coolOffMillis);
        Object[] args = Stream.concat(Stream.of(op.name(), Long.toString(now), arg),
                targets.stream().map(target -> limitsJson(target.limits()))).toArray();
        try {
            return evalSha(script, keys, args);
        } catch (RedisException e) {
            if (e.getMessage() == null || !e.getMessage().contains("NOSCRIPT"))
                throw e;
            sha = null;
            return evalSha(script, keys, args);
        }
    }

    private static boolean evalSha(RScript script, List<Object> keys, Object[] args) {
        String digest = sha;
        if (digest == null)
            sha = digest = script.scriptLoad(LUA);
        Long result = script.evalSha((String) keys.get(0), RScript.Mode.READ_WRITE, digest, RScript.ReturnType.INTEGER, keys, args);
        return result != null && result == 1L;
    }

    private static long permitTimeoutMillis(RateLimit limits) {
        return limits.getPermitTimeout() == null ? -1 : limits.getPermitTimeout().toMillis();
    }

    private static long ttlMillis(RateLimit limits) {
        long window = limits.getRates().stream().mapToLong(rate -> rate.getDuration().toMillis()).max().orElse(DEFAULT_TTL_MILLIS);
        return Math.max(window, permitTimeoutMillis(limits)) + TTL_GRACE_MILLIS;
    }

    private static String limitsJson(RateLimit limits) {
        String rates = limits.getRates().stream()
                .map(rate -> "{\"duration\":" + rate.getDuration().toMillis() + ",\"maxRequests\":" + rate.getMaxRequests() + "}")
                .collect(Collectors.joining(","));
        return "{\"rates\":[" + rates + "],\"maxConcurrent\":" + limits.getMaxConcurrent()
                + ",\"permitTimeout\":" + permitTimeoutMillis(limits) + ",\"ttl\":" + ttlMillis(limits) + "}";
    }
}
