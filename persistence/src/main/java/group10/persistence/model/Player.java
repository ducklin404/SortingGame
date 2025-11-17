package group10.persistence.model;

import java.time.Instant;
import java.util.UUID;

public final class Player {
    private final UUID id;
    private final String username;
    private final String displayName;
    private final String hashedPassword;
    private final Instant createdAt;

    public Player(UUID id, String username, String displayName, String hashedPassword, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.hashedPassword = hashedPassword;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getHashedPassword() { return hashedPassword; }
    public Instant getCreatedAt() { return createdAt; }
}
