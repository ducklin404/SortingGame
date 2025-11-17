package group10.persistence.dao;

import java.util.UUID;
import java.util.Optional;

public interface InvitesDao {

    // Create a new invite record
    UUID createInvite(UUID fromPlayerId, UUID toPlayerId, long expiresAtMillis);

    // Fetch invite
    Optional<InviteRecord> getInvite(UUID inviteId);

    // Set status = REJECTED (single writer)
    boolean rejectInvite(UUID inviteId);

    // Atomic accept: PENDING -> ACCEPTED
    boolean acceptInviteAtomically(UUID inviteId);

    // Set status = EXPIRED (server cron)
    boolean expireInvite(UUID inviteId);

    // Simple record container
    class InviteRecord {
        private final UUID inviteId;
        private final UUID fromPlayerId;
        private final UUID toPlayerId;
        private final String status;
        private final long createdAt;
        private final long expiresAt;

        public InviteRecord(UUID inviteId,
                            UUID fromPlayerId,
                            UUID toPlayerId,
                            String status,
                            long createdAt,
                            long expiresAt) {
            this.inviteId = inviteId;
            this.fromPlayerId = fromPlayerId;
            this.toPlayerId = toPlayerId;
            this.status = status;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }

        public UUID getInviteId() { return inviteId; }
        public UUID getFromPlayerId() { return fromPlayerId; }
        public UUID getToPlayerId() { return toPlayerId; }
        public String getStatus() { return status; }
        public long getCreatedAt() { return createdAt; }
        public long getExpiresAt() { return expiresAt; }
    }
}
