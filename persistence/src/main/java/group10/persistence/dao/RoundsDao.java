package group10.persistence.dao;

import group10.persistence.model.Round;

import java.util.List;
import java.util.UUID;

public interface RoundsDao {
    UUID createRound(UUID matchId, short roundNumber, String payload, String orderType, java.time.Instant deadline);
    Round findById(UUID roundId);
    List<Round> findByMatch(UUID matchId, int limit, int offset);
    boolean updateRound(UUID roundId, String payload, String orderType, java.time.Instant deadline);
    boolean deleteRound(UUID roundId);
}
