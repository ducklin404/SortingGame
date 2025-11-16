package group10.persistence.impl;

import group10.persistence.dao.SessionDao;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    public boolean isActive(UUID sessionId) {
        String sql = "SELECT is_active, last_heartbeat FROM sessions WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                return rs.getBoolean("is_active");
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
