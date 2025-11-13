package group10.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameMatch {
    private UUID id;
    private UUID inviteId;
    private UUID player1Id;
    private UUID player2Id;
    private UUID winner;
    private String status; // WAITING, PLAYING, FINISHED
    private Timestamp startedAt;
    private Timestamp endedAt;

    // Transient fields (not in DB)
    @Builder.Default
    private int player1Score = 0;

    @Builder.Default
    private int player2Score = 0;

    @Builder.Default
    private long player1TotalTime = 0;

    @Builder.Default
    private long player2TotalTime = 0;

    @Builder.Default
    private int currentRound = 0;

    public void incrementPlayer1Score() {
        this.player1Score++;
    }

    public void incrementPlayer2Score() {
        this.player2Score++;
    }

    public void addPlayer1Time(long time) {
        this.player1TotalTime += time;
    }

    public void addPlayer2Time(long time) {
        this.player2TotalTime += time;
    }

    public boolean isPlayer(UUID playerId) {
        return playerId != null && (playerId.equals(this.player1Id) || playerId.equals(this.player2Id));
    }

    public UUID getOpponentId(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        if (playerId.equals(this.player1Id)) {
            return player2Id;
        }
        if (playerId.equals(this.player2Id)) {
            return player1Id;
        }
        return null;
    }
}

