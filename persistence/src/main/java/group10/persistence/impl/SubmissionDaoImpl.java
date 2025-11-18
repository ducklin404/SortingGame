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
    public UUID createSubmission(UUID playerId, UUID matchId, UUID roundId, String submissionPayload, Long timeMs) {
        // use RETURNING id to get generated uuid
        String sql = "INSERT INTO submissions (player_id, match_id, round_id, submission_payload, submitted_at, time_ms, is_correct, score) " +
                "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?, FALSE, 0) RETURNING id";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, playerId);
            ps.setObject(2, matchId);
            ps.setObject(3, roundId);
            ps.setString(4, submissionPayload);
            if (timeMs != null) ps.setLong(5, timeMs);
            else ps.setNull(5, Types.INTEGER);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return (UUID) rs.getObject(1);
                throw new SQLException("Failed to insert submission");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    @Override
    public long getTotalTimeForPlayerInMatch(UUID playerId, UUID matchId, boolean onlyCorrect) {
        String sql = "SELECT SUM(time_ms) AS total_ms FROM submissions WHERE player_id = ? AND match_id = ?";
        if (onlyCorrect) {
            sql += " AND is_correct = TRUE";
        }

        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, playerId);
            ps.setObject(2, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return 0L;
                long total = rs.getLong("total_ms");
                return rs.wasNull() ? 0L : total;
            }
        } catch (SQLException e) {
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
    public long getTotalElapsedTimeForPlayerInMatch(UUID playerId, UUID matchId, boolean onlyCorrect, boolean firstSubmissionPerRound) {
        // Choose SQL based on firstSubmissionPerRound flag
        final String sqlFirstPerRound =
                "SELECT COALESCE(SUM((EXTRACT(EPOCH FROM (t.submitted_at - r.created_at)) * 1000))::bigint, 0) AS total_ms " +
                        "FROM ( " +
                        "  SELECT round_id, MIN(submitted_at) AS submitted_at " +
                        "  FROM submissions " +
                        "  WHERE player_id = ? AND match_id = ? " +
                        (onlyCorrect ? " AND is_correct = TRUE " : "") +
                        "  GROUP BY round_id " +
                        ") t " +
                        "JOIN rounds r ON t.round_id = r.id";

        final String sqlAllSubs =
                "SELECT COALESCE(SUM((EXTRACT(EPOCH FROM (s.submitted_at - r.created_at)) * 1000))::bigint, 0) AS total_ms " +
                        "FROM submissions s " +
                        "JOIN rounds r ON s.round_id = r.id " +
                        "WHERE s.player_id = ? AND s.match_id = ? " +
                        (onlyCorrect ? " AND s.is_correct = TRUE " : "");

        final String sql = firstSubmissionPerRound ? sqlFirstPerRound : sqlAllSubs;

        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, playerId);
            ps.setObject(2, matchId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return 0L;
                long total = rs.getLong("total_ms");
                return rs.wasNull() ? 0L : total;
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
        long time = rs.getLong("time_ms");
        Long timeMs = rs.wasNull() ? null : time;
        boolean isCorrect = rs.getBoolean("is_correct");
        short score = rs.getShort("score");
        return new Submission(id, playerId, matchId, roundId, payload, submittedAt, timeMs, isCorrect, score);
    }
}
