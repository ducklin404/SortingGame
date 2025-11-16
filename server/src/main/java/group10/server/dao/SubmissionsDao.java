package group10.server.dao;

import java.util.List;
import java.util.UUID;

public interface SubmissionsDao {
    // Returns true if accepted (first), false if duplicate (ignored)
    boolean insertSubmission(UUID playerId, UUID matchId, UUID roundId, String submissionJson, int timeMs);
}
