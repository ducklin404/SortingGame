package group10.server.dao;

import java.util.UUID;

public interface MatchesDao {
    UUID createMatch(UUID playerA, UUID playerB);
    void setMatchStarted(UUID matchId);
    void setMatchEnded(UUID matchId, String result, int aPoints, int bPoints);
}
