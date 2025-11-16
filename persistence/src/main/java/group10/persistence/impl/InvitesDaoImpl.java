package group10.persistence.impl;

import group10.persistence.dao.InvitesDao;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public class InvitesDaoImpl implements InvitesDao {

    private final DataSource ds;

    public InvitesDaoImpl(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public UUID createInvite(UUID fromPlayerId, UUID toPlayerId, long expiresAtMillis) {
        String insertInvite =
                "INSERT INTO invites(status, expires_at) " +
                        "VALUES ('PENDING', to_timestamp(? / 1000.0)) RETURNING id";

        String insertPlayers =
                "INSERT INTO invites_players(invite_id, from_player_id, to_player_id) " +
                        "VALUES (?, ?, ?)";

        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);

            UUID inviteId;

            // Insert into invites
            try (PreparedStatement ps = c.prepareStatement(insertInvite)) {
                ps.setLong(1, expiresAtMillis);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Failed to insert invite");
                    inviteId = (UUID) rs.getObject("id");
                }
            }

            // Insert players
            try (PreparedStatement ps2 = c.prepareStatement(insertPlayers)) {
                ps2.setObject(1, inviteId);
                ps2.setObject(2, fromPlayerId);
                ps2.setObject(3, toPlayerId);
                ps2.executeUpdate();
            }

            c.commit();
            return inviteId;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<InviteRecord> getInvite(UUID inviteId) {
        String sql =
                "SELECT i.id AS invite_id, " +
                        "       ip.from_player_id, " +
                        "       ip.to_player_id, " +
                        "       i.status, " +
                        "       extract(epoch from i.created_at) * 1000 AS created_ms, " +
                        "       extract(epoch from i.expires_at) * 1000 AS expires_ms " +
                        "FROM invites i " +
                        "JOIN invites_players ip ON i.id = ip.invite_id " +
                        "WHERE i.id = ?";

        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, inviteId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                UUID invId = (UUID) rs.getObject("invite_id");
                UUID from = (UUID) rs.getObject("from_player_id");
                UUID to = (UUID) rs.getObject("to_player_id");
                String status = rs.getString("status");
                long created = rs.getLong("created_ms");
                long expires = rs.getLong("expires_ms");

                InviteRecord record =
                        new InviteRecord(invId, from, to, status, created, expires);

                return Optional.of(record);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean rejectInvite(UUID inviteId) {
        String sql = "UPDATE invites SET status = 'REJECTED' WHERE id = ? AND status = 'PENDING'";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, inviteId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean acceptInviteAtomically(UUID inviteId) {
        String sql = "UPDATE invites SET status = 'ACCEPTED' " +
                "WHERE id = ? AND status = 'PENDING'";

        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, inviteId);

            // if row_count == 1, success; if 0, someone else processed it
            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean expireInvite(UUID inviteId) {
        String sql = "UPDATE invites SET status = 'EXPIRED' " +
                "WHERE id = ? AND status = 'PENDING'";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, inviteId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
