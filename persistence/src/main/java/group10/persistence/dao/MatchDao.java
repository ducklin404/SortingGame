package group10.persistence.dao;

import group10.persistence.model.Match;
import group10.persistence.model.MatchHistoryItem;
import java.util.List;
import java.util.UUID;

public interface MatchDao {

    UUID createMatch(UUID playerAId, UUID playerBId);
    Match findById(UUID matchId);
    List<Match> findByPlayer(UUID playerId, int limit, int offset);
    List<MatchHistoryItem> getMatchHistory(UUID playerId, int limit);
    boolean startMatch(UUID matchId);
    boolean finishMatch(UUID matchId, String result);
    boolean updatePoints(UUID matchId, int playerAPoints, int playerBPoints);

    boolean deleteMatch(UUID matchId);


}
