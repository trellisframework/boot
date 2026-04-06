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
    private static final Duration CLEANUP_INTERVAL = Duration.ofMinutes(5);
    private final Duration maxAge;
    private final ConcurrentHashMap<String, Instant> sessionFirstSeen = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, McpStreamableServerSession> sessions;

    @SuppressWarnings({"unchecked", "resource"})
    public McpSessionCleanupScheduler(McpStreamableServerTransportProvider transportProvider, Duration maxAge) {
        this.maxAge = maxAge;
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
        scheduler.scheduleAtFixedRate(this::cleanup, CLEANUP_INTERVAL.toMillis(), CLEANUP_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
        log.info("MCP session cleanup enabled: maxAge={}", maxAge);
    }

    void cleanup() {
        if (sessions == null)
            return;
        Instant now = Instant.now();
        for (String id : sessions.keySet())
            sessionFirstSeen.putIfAbsent(id, now);

        int evicted = 0;
        for (String id : sessionFirstSeen.keySet()) {
            boolean expired = sessions.containsKey(id) && Duration.between(sessionFirstSeen.get(id), now).compareTo(maxAge) > 0;
            boolean orphan = !sessions.containsKey(id);
            if (expired || orphan) {
                McpStreamableServerSession session = sessions.remove(id);
                sessionFirstSeen.remove(id);
                if (session != null) {
                    try { session.close(); } catch (Exception ignored) {}
                    evicted++;
                }
            }
        }
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
        if (sessions != null) {
            Instant now = Instant.now();
            for (Map.Entry<String, Instant> entry : sessionFirstSeen.entrySet()) {
                Duration age = Duration.between(entry.getValue(), now);
                result.put(entry.getKey(), Map.of("age", age.toSeconds() + "s", "expiresIn", maxAge.minus(age).toSeconds() + "s"));
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
