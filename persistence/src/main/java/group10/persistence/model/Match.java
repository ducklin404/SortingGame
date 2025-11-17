package group10.persistence.model;

import java.time.Instant;
import java.util.UUID;

public final class Match {
    private final UUID id;
    private final Instant createdAt;
    private final Instant startedAt;
    private final Instant endedAt;
    private final int playerAPoints;
    private final int playerBPoints;
    private final String result; // "A_WIN", "B_WIN", "DRAW" or null

    public Match(UUID id, Instant createdAt, Instant startedAt, Instant endedAt,
                 int playerAPoints, int playerBPoints, String result) {
        this.id = id;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.playerAPoints = playerAPoints;
        this.playerBPoints = playerBPoints;
        this.result = result;
    }

    public UUID getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public int getPlayerAPoints() { return playerAPoints; }
    public int getPlayerBPoints() { return playerBPoints; }
    public String getResult() { return result; }
}
