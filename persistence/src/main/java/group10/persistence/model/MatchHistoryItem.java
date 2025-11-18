package group10.persistence.model;

import java.time.Instant;
import java.util.UUID;

public class MatchHistoryItem {
    private UUID matchId;
    private String opponent;    // tên đối thủ
    private int myPoints;
    private int opponentPoints;
    private String result;      // A_WIN / B_WIN / DRAW
    private Instant createdAt;

    public MatchHistoryItem(UUID matchId, String opponent, int myPoints, int opponentPoints, String result, Instant createdAt) {
        this.matchId = matchId;
        this.opponent = opponent;
        this.myPoints = myPoints;
        this.opponentPoints = opponentPoints;
        this.result = result;
        this.createdAt = createdAt;
    }

    public UUID getMatchId() { return matchId; }
    public String getOpponent() { return opponent; }
    public int getMyPoints() { return myPoints; }
    public int getOpponentPoints() { return opponentPoints; }
    public String getResult() { return result; }
    public Instant getCreatedAt() { return createdAt; }
}
