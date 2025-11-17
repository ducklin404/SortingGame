package group10.persistence.dao;

import group10.persistence.model.Match;

import java.util.List;
import java.util.UUID;

public interface MatchDao {

    UUID createMatch(UUID playerAId, UUID playerBId);
    Match findById(UUID matchId);
    List<Match> findByPlayer(UUID playerId, int limit, int offset);
    boolean startMatch(UUID matchId);
    boolean finishMatch(UUID matchId, String result);
    boolean updatePoints(UUID matchId, int playerAPoints, int playerBPoints);

    boolean deleteMatch(UUID matchId);
}
