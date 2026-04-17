package net.trellisframework.mcp.autoconfigure;

import io.modelcontextprotocol.spec.McpStreamableServerSession;
import io.modelcontextprotocol.spec.McpStreamableServerTransportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class McpSessionCleanupScheduler {
    private static final Logger log = LoggerFactory.getLogger(McpSessionCleanupScheduler.class);
    private final Duration maxAge;
    private final Duration idleTimeout;
    private final ConcurrentHashMap<String, Instant> sessionFirstSeen = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> sessionLastSeen = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, McpStreamableServerSession> sessions;

    @SuppressWarnings({"unchecked", "resource"})
    public McpSessionCleanupScheduler(McpStreamableServerTransportProvider transportProvider, Duration maxAge, Duration idleTimeout, Duration cleanupInterval) {
        this.maxAge = maxAge;
        this.idleTimeout = idleTimeout;
        this.sessions = extractSessionsMap(transportProvider);
        if (sessions == null) {
            log.error("Failed to access sessions map. Session cleanup disabled.");
            return;
        }
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "mcp-session-cleanup");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::cleanup, cleanupInterval.toMillis(), cleanupInterval.toMillis(), TimeUnit.MILLISECONDS);
        log.info("MCP session cleanup enabled: maxAge={}, idleTimeout={}, cleanupInterval={}", maxAge, idleTimeout, cleanupInterval);
    }

    public void touch(String sessionId) {
        if (sessionId == null || sessionId.isBlank() || sessions == null || !sessions.containsKey(sessionId))
            return;
        Instant now = Instant.now();
        sessionFirstSeen.putIfAbsent(sessionId, now);
        sessionLastSeen.put(sessionId, now);
    }

    void cleanup() {
        if (sessions == null)
            return;
        Instant now = Instant.now();
        for (String id : sessions.keySet()) {
            sessionFirstSeen.putIfAbsent(id, now);
            sessionLastSeen.putIfAbsent(id, now);
        }

        int evicted = 0;
        for (String id : sessionFirstSeen.keySet()) {
            boolean orphan = !sessions.containsKey(id);
            boolean expired = sessions.containsKey(id) && Duration.between(sessionFirstSeen.get(id), now).compareTo(maxAge) > 0;
            boolean idle = idleTimeout != null && sessions.containsKey(id) && Duration.between(sessionLastSeen.getOrDefault(id, sessionFirstSeen.get(id)), now).compareTo(idleTimeout) > 0;
            if (expired || orphan || idle) {
                McpStreamableServerSession session = sessions.remove(id);
                sessionFirstSeen.remove(id);
                sessionLastSeen.remove(id);
                if (session != null) {
                    try { session.close(); } catch (Exception ignored) {}
                    evicted++;
                }
            }
        }
        sessionLastSeen.keySet().removeIf(id -> !sessions.containsKey(id));
        if (evicted > 0)
            log.info("MCP session cleanup: evicted={}, remaining={}", evicted, sessions.size());
    }

    int sessionCount() {
        return sessions != null ? sessions.size() : 0;
    }

    Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("active", sessionCount());
        result.put("maxAge", maxAge.toString());
        if (idleTimeout != null)
            result.put("idleTimeout", idleTimeout.toString());
        if (sessions != null) {
            Instant now = Instant.now();
            for (Map.Entry<String, Instant> entry : sessionFirstSeen.entrySet()) {
                Duration age = Duration.between(entry.getValue(), now);
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("age", age.toSeconds() + "s");
                info.put("expiresIn", maxAge.minus(age).toSeconds() + "s");
                Instant lastSeen = sessionLastSeen.get(entry.getKey());
                if (lastSeen != null)
                    info.put("idleSeconds", Duration.between(lastSeen, now).toSeconds());
                result.put(entry.getKey(), info);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    static ConcurrentHashMap<String, McpStreamableServerSession> extractSessionsMap(Object transportProvider) {
        Class<?> clazz = transportProvider.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField("sessions");
                field.setAccessible(true);
                return (ConcurrentHashMap<String, McpStreamableServerSession>) field.get(transportProvider);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                log.error("Failed to extract sessions map: {}", e.getMessage());
                return null;
            }
        }
        return null;
    }
}
