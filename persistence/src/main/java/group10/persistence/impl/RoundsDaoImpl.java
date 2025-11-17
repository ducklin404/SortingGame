package group10.persistence.impl;

import group10.persistence.dao.RoundsDao;
import group10.persistence.model.Round;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RoundsDaoImpl implements RoundsDao {
    private final DataSource ds;

    public RoundsDaoImpl(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public UUID createRound(UUID matchId, short roundNumber, String payload, String orderType, Instant deadline) {
        String sql = "INSERT INTO rounds (match_id, round_number, payload, order_type, created_at, deadline) " +
                "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?) RETURNING id";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, matchId);
            ps.setShort(2, roundNumber);
            ps.setString(3, payload);
            ps.setString(4, orderType);
            if (deadline != null) ps.setTimestamp(5, Timestamp.from(deadline));
            else ps.setNull(5, Types.TIMESTAMP);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return (UUID) rs.getObject(1);
                throw new SQLException("Failed to insert round");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Round findById(UUID roundId) {
        String sql = "SELECT id, match_id, round_number, payload, order_type, created_at, deadline FROM rounds WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, roundId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRound(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Round> findByMatch(UUID matchId, int limit, int offset) {
        String sql = "SELECT id, match_id, round_number, payload, order_type, created_at, deadline " +
                "FROM rounds WHERE match_id = ? ORDER BY round_number ASC LIMIT ? OFFSET ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, matchId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                List<Round> out = new ArrayList<>();
                while (rs.next()) out.add(mapRound(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean updateRound(UUID roundId, String payload, String orderType, Instant deadline) {
        String sql = "UPDATE rounds SET payload = ?, order_type = ?, deadline = ? WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, payload);
            ps.setString(2, orderType);
            if (deadline != null) ps.setTimestamp(3, Timestamp.from(deadline));
            else ps.setNull(3, Types.TIMESTAMP);
            ps.setObject(4, roundId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean deleteRound(UUID roundId) {
        String sql = "DELETE FROM rounds WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, roundId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Round mapRound(ResultSet rs) throws SQLException {
        UUID id = (UUID) rs.getObject("id");
        UUID matchId = (UUID) rs.getObject("match_id");
        short roundNumber = rs.getShort("round_number");
        String payload = rs.getString("payload");
        String orderType = rs.getString("order_type");
        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp deadlineTs = rs.getTimestamp("deadline");
        Instant createdAt = createdTs == null ? null : createdTs.toInstant();
        Instant deadline = deadlineTs == null ? null : deadlineTs.toInstant();
        return new Round(id, matchId, roundNumber, payload, orderType, createdAt, deadline);
    }
}
