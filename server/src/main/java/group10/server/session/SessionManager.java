package group10.server.session;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class SessionManager {
    private static class Session {
        UUID userId;
        Instant lastHeartbeat;
        boolean active;
        Session(UUID userId) { this.userId = userId; this.lastHeartbeat = Instant.now(); this.active = true; }
    }

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final long timeoutMs;

    public SessionManager(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public UUID createSession(UUID userId) {
        UUID sid = UUID.randomUUID();
        sessions.put(sid, new Session(userId));
        return sid;
    }

    public boolean isActive(UUID sessionId) {
        Session s = sessions.get(sessionId);
        if (s == null) return false;
        if (!s.active) return false;
        if (Instant.now().toEpochMilli() - s.lastHeartbeat.toEpochMilli() > timeoutMs) {
            s.active = false;
            return false;
        }
        return true;
    }

    public void touchHeartbeat(UUID sessionId) {
        Session s = sessions.get(sessionId);
        if (s != null) {
            s.lastHeartbeat = Instant.now();
            s.active = true;
        }
    }

    public void invalidate(UUID sessionId) {
        Session s = sessions.get(sessionId);
        if (s != null) s.active = false;
    }

    public UUID getUserId(UUID sessionId) {
        Session s = sessions.get(sessionId);
        return s == null ? null : s.userId;
    }
}
