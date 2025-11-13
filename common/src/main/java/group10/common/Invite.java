package group10.common;

import lombok.*;
import java.sql.Timestamp;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invite {
    private UUID id;
    private UUID fromPlayerId;
    private UUID toPlayerId;
    private String status; // PENDING, ACCEPTED, REJECTED, EXPIRED
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp expiresAt;

    // Transient fields for display
    @ToString.Exclude
    private String fromPlayerName;

    @ToString.Exclude
    private String toPlayerName;

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isExpired() {
        return expiresAt != null &&
                System.currentTimeMillis() > expiresAt.getTime();
    }

    public long getRemainingSeconds() {
        if (expiresAt == null) return 0;
        long remaining = expiresAt.getTime() - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }
}
