package group10.persistence.dao;

import java.util.List;
import java.util.UUID;

public interface SessionDao {
    UUID createSession(UUID userId);
    boolean touchHeartbeat(UUID sessionId);
    boolean invalidate(UUID sessionId);
    boolean isActive(UUID sessionId);
    UUID getUserId(UUID sessionId);
}
