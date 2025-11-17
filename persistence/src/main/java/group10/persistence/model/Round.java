package group10.persistence.model;

import java.time.Instant;
import java.util.UUID;

public final class Round {
    private final UUID id;
    private final UUID matchId;
    private final short roundNumber;
    private final String payload;
    private final String orderType;
    private final Instant createdAt;
    private final Instant deadline;

    public Round(UUID id, UUID matchId, short roundNumber, String payload, String orderType, Instant createdAt, Instant deadline) {
        this.id = id;
        this.matchId = matchId;
        this.roundNumber = roundNumber;
        this.payload = payload;
        this.orderType = orderType;
        this.createdAt = createdAt;
        this.deadline = deadline;
    }

    public UUID getId() { return id; }
    public UUID getMatchId() { return matchId; }
    public short getRoundNumber() { return roundNumber; }
    public String getPayload() { return payload; }
    public String getOrderType() { return orderType; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDeadline() { return deadline; }
}
