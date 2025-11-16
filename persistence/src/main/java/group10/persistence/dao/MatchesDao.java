package group10.persistence.dao;

import java.util.UUID;

public interface MatchesDao {
    // atomic create match + match_players inside transaction
    UUID createMatch(UUID playerA, UUID playerB);

    // update match end/result
    void finalizeMatch(UUID matchId, int aPoints, int bPoints, String result);
}
