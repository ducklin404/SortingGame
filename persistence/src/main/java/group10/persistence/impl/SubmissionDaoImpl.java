package group10.persistence.impl;

import group10.persistence.dao.SubmissionDao;
import group10.persistence.model.Submission;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SubmissionDaoImpl implements SubmissionDao {
    private final DataSource ds;

    public SubmissionDaoImpl(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public UUID createSubmission(UUID playerId, UUID matchId, UUID roundId, String submissionPayload, Integer timeMs) {
        // use RETURNING id to get generated uuid
        String sql = "INSERT INTO submissions (player_id, match_id, round_id, submission_payload, submitted_at, time_ms, is_correct, score) " +
                "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?, FALSE, 0) RETURNING id";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, playerId);
            ps.setObject(2, matchId);
            ps.setObject(3, roundId);
            ps.setString(4, submissionPayload);
            if (timeMs != null) ps.setInt(5, timeMs);
            else ps.setNull(5, Types.INTEGER);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return (UUID) rs.getObject(1);
                throw new SQLException("Failed to insert submission");
            }
        } catch (SQLException e) {
            // Note: will throw if unique constraint violated; caller can catch and treat as conflict
            throw new RuntimeException(e);
        }
    }

    @Override
    public Submission findById(UUID submissionId) {
        String sql = "SELECT id, player_id, match_id, round_id, submission_payload, submitted_at, time_ms, is_correct, score FROM submissions WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, submissionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapSubmission(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Submission findByPlayerMatchRound(UUID playerId, UUID matchId, UUID roundId) {
        String sql = "SELECT id, player_id, match_id, round_id, submission_payload, submitted_at, time_ms, is_correct, score " +
                "FROM submissions WHERE player_id = ? AND match_id = ? AND round_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, playerId);
            ps.setObject(2, matchId);
            ps.setObject(3, roundId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapSubmission(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Submission> listByMatchAndRound(UUID matchId, UUID roundId, int limit, int offset) {
        String sql = "SELECT id, player_id, match_id, round_id, submission_payload, submitted_at, time_ms, is_correct, score " +
                "FROM submissions WHERE match_id = ? AND round_id = ? ORDER BY submitted_at ASC LIMIT ? OFFSET ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, matchId);
            ps.setObject(2, roundId);
            ps.setInt(3, limit);
            ps.setInt(4, offset);
            try (ResultSet rs = ps.executeQuery()) {
                List<Submission> out = new ArrayList<>();
                while (rs.next()) out.add(mapSubmission(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Submission> listByPlayer(UUID playerId, int limit, int offset) {
        String sql = "SELECT id, player_id, match_id, round_id, submission_payload, submitted_at, time_ms, is_correct, score " +
                "FROM submissions WHERE player_id = ? ORDER BY submitted_at DESC LIMIT ? OFFSET ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, playerId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                List<Submission> out = new ArrayList<>();
                while (rs.next()) out.add(mapSubmission(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean markCorrectAndSetScore(UUID submissionId, boolean isCorrect, short score) {
        String sql = "UPDATE submissions SET is_correct = ?, score = ? WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, isCorrect);
            ps.setShort(2, score);
            ps.setObject(3, submissionId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean deleteSubmission(UUID submissionId) {
        String sql = "DELETE FROM submissions WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, submissionId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Submission mapSubmission(ResultSet rs) throws SQLException {
        UUID id = (UUID) rs.getObject("id");
        UUID playerId = (UUID) rs.getObject("player_id");
        UUID matchId = (UUID) rs.getObject("match_id");
        UUID roundId = (UUID) rs.getObject("round_id");
        String payload = rs.getString("submission_payload");
        Timestamp submittedTs = rs.getTimestamp("submitted_at");
        Instant submittedAt = submittedTs == null ? null : submittedTs.toInstant();
        int time = rs.getInt("time_ms");
        Integer timeMs = rs.wasNull() ? null : time;
        boolean isCorrect = rs.getBoolean("is_correct");
        short score = rs.getShort("score");
        return new Submission(id, playerId, matchId, roundId, payload, submittedAt, timeMs, isCorrect, score);
    }
}
