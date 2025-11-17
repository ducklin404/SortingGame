package group10.persistence.model;

import java.time.Instant;
import java.util.UUID;
import java.math.BigDecimal;

public final class PlayerStats {
    private final UUID playerId;
    private final BigDecimal totalPoints;
    private final int wins;
    private final int losses;
    private final int draws;
    private final int matchesPlayed;
    private final Instant lastUpdated;

    public PlayerStats(UUID playerId, BigDecimal totalPoints, int wins, int losses, int draws, int matchesPlayed, Instant lastUpdated) {
        this.playerId = playerId;
        this.totalPoints = totalPoints;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        this.matchesPlayed = matchesPlayed;
        this.lastUpdated = lastUpdated;
    }

    public UUID getPlayerId() { return playerId; }
    public BigDecimal getTotalPoints() { return totalPoints; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getDraws() { return draws; }
    public int getMatchesPlayed() { return matchesPlayed; }
    public Instant getLastUpdated() { return lastUpdated; }
}
