package group10.persistence.dao;

import group10.persistence.model.SessionRecord;

import java.util.List;
import java.util.UUID;

public interface SessionDao {
    UUID createSession(UUID userId);
    boolean touchHeartbeat(UUID sessionId);
    boolean invalidate(UUID sessionId);

    boolean isActive(UUID sessionId, long timeoutMs);

    UUID getUserId(UUID sessionId);
    SessionRecord findLatestSessionByUser(UUID userId);
    List<SessionRecord> findActiveSessionsByUser(UUID userId, int limit, int offset);
}
