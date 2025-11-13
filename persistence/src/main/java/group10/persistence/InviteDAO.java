package group10.persistence;

import group10.common.Invite;
import lombok.extern.slf4j.Slf4j;
import group10.persistence.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class InviteDAO {
    private final Database dbConnection;

    public InviteDAO() {
        this.dbConnection = Database.getInstance();
    }

    /**
     * Create new invite
     */
    public UUID createInvite(UUID fromPlayerId, UUID toPlayerId, long expirationMs) {
        String sql = "INSERT INTO invites (from_player_id, to_player_id, status, expires_at) " +
                "VALUES (?, ?, 'PENDING', ?) RETURNING id";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, fromPlayerId, Types.OTHER);
            stmt.setObject(2, toPlayerId, Types.OTHER);

            Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + expirationMs);
            stmt.setTimestamp(3, expiresAt);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    UUID inviteId = getUuid(rs, "id");
                    log.info("Invite created: id={}, from={}, to={}", inviteId, fromPlayerId, toPlayerId);
                    return inviteId;
                }
            }
            return null;
        } catch (SQLException e) {
            log.error("Error creating invite", e);
            return null;
        }
    }

    /**
     * Get invite by ID
     */
    public Invite getInviteById(UUID inviteId) {
        String sql = "SELECT i.*, " +
                "p1.display_name as from_name, " +
                "p2.display_name as to_name " +
                "FROM invites i " +
                "JOIN players p1 ON i.from_player_id = p1.id " +
                "JOIN players p2 ON i.to_player_id = p2.id " +
                "WHERE i.id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, inviteId, Types.OTHER);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToInvite(rs);
            }
            return null;
        } catch (SQLException e) {
            log.error("Error getting invite by ID", e);
            return null;
        }
    }

    /**
     * Get pending invite for player
     */
    public Invite getPendingInviteForPlayer(UUID toPlayerId) {
        String sql = "SELECT i.*, " +
                "p1.display_name as from_name, " +
                "p2.display_name as to_name " +
                "FROM invites i " +
                "JOIN players p1 ON i.from_player_id = p1.id " +
                "JOIN players p2 ON i.to_player_id = p2.id " +
                "WHERE i.to_player_id = ? AND i.status = 'PENDING' " +
                "ORDER BY i.created_at DESC LIMIT 1";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, toPlayerId, Types.OTHER);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToInvite(rs);
            }
            return null;
        } catch (SQLException e) {
            log.error("Error getting pending invite", e);
            return null;
        }
    }

    /**
     * Update invite status
     */
    public boolean updateInviteStatus(UUID inviteId, String newStatus) {
        String sql = "UPDATE invites SET status = ?, updated_at = NOW() WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus);
            stmt.setObject(2, inviteId, Types.OTHER);

            int affected = stmt.executeUpdate();

            if (affected > 0) {
                log.info("Invite status updated: id={}, status={}", inviteId, newStatus);
                return true;
            }
            return false;
        } catch (SQLException e) {
            log.error("Error updating invite status", e);
            return false;
        }
    }

    /**
     * Get expired invites
     */
    public List<Invite> getExpiredInvites() {
        String sql = "SELECT i.*, " +
                "p1.display_name AS from_name, " +
                "p2.display_name AS to_name " +
                "FROM invites i " +
                "JOIN players p1 ON i.from_player_id = p1.id " +
                "JOIN players p2 ON i.to_player_id = p2.id " +
                "WHERE i.status = 'PENDING' AND i.expires_at < NOW()";


        List<Invite> expiredInvites = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                expiredInvites.add(mapResultSetToInvite(rs));
            }

            log.debug("Found {} expired invites", expiredInvites.size());
            return expiredInvites;
        } catch (SQLException e) {
            log.error("Error getting expired invites", e);
            return expiredInvites;
        }
    }

    /**
     * Mark expired invites
     */
    public int markExpiredInvites() {
        String sql = "UPDATE invites SET status = 'EXPIRED', updated_at = NOW() " +
                "WHERE status = 'PENDING' AND expires_at < NOW()";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int affected = stmt.executeUpdate();

            if (affected > 0) {
                log.info("Marked {} invites as expired", affected);
            }
            return affected;
        } catch (SQLException e) {
            log.error("Error marking expired invites", e);
            return 0;
        }
    }

    /**
     * Delete old invites (cleanup)
     */
    public int deleteOldInvites(int daysOld) {
        String sql = "DELETE FROM invites WHERE created_at < NOW() - (? * INTERVAL '1 day')";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, daysOld);
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                log.info("Deleted {} old invites", affected);
            }
            return affected;
        } catch (SQLException e) {
            log.error("Error deleting old invites", e);
            return 0;
        }
    }

    /**
     * Check if player has pending invite
     */
    public boolean hasPendingInvite(UUID toPlayerId) {
        String sql = "SELECT COUNT(*) FROM invites WHERE to_player_id = ? AND status = 'PENDING'";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, toPlayerId, Types.OTHER);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            log.error("Error checking pending invite", e);
            return false;
        }
    }

    /**
     * Map ResultSet to Invite
     */
    private Invite mapResultSetToInvite(ResultSet rs) throws SQLException {
        return Invite.builder()
                .id(getUuid(rs, "id"))
                .fromPlayerId(getUuid(rs, "from_player_id"))
                .toPlayerId(getUuid(rs, "to_player_id"))
                .status(rs.getString("status"))
                .createdAt(rs.getTimestamp("created_at"))
                .updatedAt(rs.getTimestamp("updated_at"))
                .expiresAt(rs.getTimestamp("expires_at"))
                .fromPlayerName(rs.getString("from_name"))
                .toPlayerName(rs.getString("to_name"))
                .build();
    }

    private UUID getUuid(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String str) {
            return UUID.fromString(str);
        }
        throw new SQLException("Unexpected UUID type for column " + column + ": " + value);
    }
}
