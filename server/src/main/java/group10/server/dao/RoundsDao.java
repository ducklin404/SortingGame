package group10.server.dao;

import java.util.UUID;

public interface RoundsDao {
    UUID insertRound(UUID matchId, int roundNumber, String payloadJson, String orderType, long deadlineEpochMs);
}
