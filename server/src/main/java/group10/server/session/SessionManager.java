package group10.server.session;

import group10.persistence.dao.SessionDao;
import group10.persistence.model.SessionRecord;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DB-backed session manager. Delegates storage to SessionDao and applies
 * timeout-based freshness rules (heartbeat staleness) as business logic.
 */
public class SessionManager {
    private final SessionDao sessionDao;
    private final long timeoutMs;

    public SessionManager(SessionDao sessionDao, long timeoutMs) {
        this.sessionDao = sessionDao;
        this.timeoutMs = timeoutMs;
    }

    // Create a session for a given user and return the session id.
    public UUID createSession(UUID userId) {
        if (userId == null) throw new IllegalArgumentException("userId must not be null");
        return sessionDao.createSession(userId);
    }

    // Check whether the session is active, considering last heartbeat and timeout.
    public boolean isActive(UUID sessionId) {
        if (sessionId == null) return false;
        return sessionDao.isActive(sessionId, timeoutMs);
    }

    // Update the heartbeat timestamp for the session. Returns true if update succeeded.
    public boolean touchHeartbeat(UUID sessionId) {
        if (sessionId == null) return false;
        return sessionDao.touchHeartbeat(sessionId);
    }

    // Mark the session as inactive immediately.
    public boolean invalidate(UUID sessionId) {
        if (sessionId == null) return false;
        return sessionDao.invalidate(sessionId);
    }

    // Get the user id for this session, or null if not found.
    public UUID getUserId(UUID sessionId) {
        if (sessionId == null) return null;
        return sessionDao.getUserId(sessionId);
    }

    /**
     * Return a paginated list of sessions for the given user that are considered active.
     * @param userId user whose sessions to list
     * @param limit  maximum number of rows to return (pass a reasonable upper bound)
     * @param offset pagination offset
     * @return list of SessionRecord where each record's active flag reflects both DB flag and heartbeat freshness
     */
    public List<SessionRecord> getActiveSessionsForUser(UUID userId, int limit, int offset) {
        if (userId == null) return List.of();
        if (limit <= 0) return List.of();

        List<SessionRecord> rows = sessionDao.findActiveSessionsByUser(userId, limit, offset);
        Instant threshold = Instant.now().minusMillis(timeoutMs);

        // Rebuild each record with adjusted active flag based on heartbeat staleness
        return rows.stream()
                .filter(r -> r.getLastHeartbeat() != null && r.getLastHeartbeat().isAfter(threshold))
                .map(r -> new SessionRecord(
                        r.getId(),
                        r.getUserId(),
                        r.getLastHeartbeat(),
                        true // guaranteed fresh by filter, so active == true
                ))
                .collect(Collectors.toList());
    }


    public SessionRecord getLatestActiveSession(UUID userId) {
        if (userId == null) return null;

        SessionRecord rec = sessionDao.findLatestSessionByUser(userId);
        if (rec == null) return null;

        if (!rec.isActive() || rec.getLastHeartbeat() == null) return null;

        Instant threshold = Instant.now().minusMillis(timeoutMs);
        if (!rec.getLastHeartbeat().isAfter(threshold)) return null;

        // Return a record that reflects it is active (both DB and heartbeat)
        return new SessionRecord(
                rec.getId(),
                rec.getUserId(),
                rec.getLastHeartbeat(),
                true
        );
    }
}
