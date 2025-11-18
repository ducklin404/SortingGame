package group10.persistence.model;

import java.time.Instant;
import java.util.UUID;

public final class SessionRecord {
    private final UUID id;
    private final UUID userId;
    private final Instant lastHeartbeat;
    private final boolean active;

    public SessionRecord(UUID id, UUID userId, Instant lastHeartbeat, boolean active) {
        this.id = id;
        this.userId = userId;
        this.lastHeartbeat = lastHeartbeat;
        this.active = active;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public Instant getLastHeartbeat() { return lastHeartbeat; }
    public boolean isActive() { return active; }
}
