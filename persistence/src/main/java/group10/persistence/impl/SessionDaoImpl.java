package group10.persistence.impl;

import group10.persistence.dao.SessionDao;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import group10.persistence.model.SessionRecord;

public class SessionDaoImpl implements SessionDao {
    private final DataSource ds;

    public SessionDaoImpl(DataSource ds) { this.ds = ds; }

    @Override
    public UUID createSession(UUID userId) {
        String sql = "INSERT INTO sessions(user_id, last_heartbeat, is_active) VALUES (?, now(), TRUE) RETURNING id";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return (UUID) rs.getObject(1);
            }
            throw new SQLException("Failed to insert session");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean touchHeartbeat(UUID sessionId) {
        String sql = "UPDATE sessions SET last_heartbeat = now(), is_active = TRUE WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, sessionId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public List<SessionRecord> findActiveSessionsByUser(UUID userId, int limit, int offset) {
        String sql = "SELECT id, user_id, last_heartbeat, is_active " +
                "FROM sessions " +
                "WHERE user_id = ? AND is_active = TRUE " +
                "ORDER BY last_heartbeat DESC " +
                "LIMIT ? OFFSET ?";

        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, userId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);

            try (ResultSet rs = ps.executeQuery()) {
                List<SessionRecord> out = new ArrayList<>();
                while (rs.next()) {
                    UUID id = (UUID) rs.getObject("id");
                    UUID uid = (UUID) rs.getObject("user_id");
                    Timestamp ts = rs.getTimestamp("last_heartbeat");
                    boolean active = rs.getBoolean("is_active");

                    out.add(new SessionRecord(
                            id,
                            uid,
                            ts == null ? null : ts.toInstant(),
                            active
                    ));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public boolean invalidate(UUID sessionId) {
        String sql = "UPDATE sessions SET is_active = FALSE WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, sessionId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SessionRecord findLatestSessionByUser(UUID userId) {
        String sql = "SELECT id, user_id, last_heartbeat, is_active " +
                "FROM sessions " +
                "WHERE user_id = ? " +
                "ORDER BY last_heartbeat DESC " +
                "LIMIT 1";

        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                UUID id = (UUID) rs.getObject("id");
                UUID uid = (UUID) rs.getObject("user_id");
                Timestamp ts = rs.getTimestamp("last_heartbeat");
                boolean active = rs.getBoolean("is_active");

                return new SessionRecord(id, uid, ts.toInstant(), active);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }



    // Use a timestamp threshold to check staleness server-side.

    @Override
    public boolean isActive(UUID sessionId, long timeoutMs) {
        String sql = "SELECT is_active, last_heartbeat FROM sessions WHERE id = ?";
        Instant threshold = Instant.now().minusMillis(timeoutMs);
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                boolean isActive = rs.getBoolean("is_active");
                Timestamp lastHeartbeatTs = rs.getTimestamp("last_heartbeat");
                if (lastHeartbeatTs == null) return false;
                Instant lastHeartbeat = lastHeartbeatTs.toInstant();
                if (!isActive) return false;
                return lastHeartbeat.isAfter(threshold);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UUID getUserId(UUID sessionId) {
        String sql = "SELECT user_id FROM sessions WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return (UUID) rs.getObject(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
