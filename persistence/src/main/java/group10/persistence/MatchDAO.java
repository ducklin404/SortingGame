package group10.persistence;

import group10.common.GameMatch;
import lombok.extern.slf4j.Slf4j;
import group10.persistence.Database;

import java.sql.*;
import java.util.UUID;

@Slf4j
public class MatchDAO {
    private final Database dbConnection;

    public MatchDAO() {
        this.dbConnection = Database.getInstance();
    }

    /**
     * Create new match from accepted invite
     */
    public UUID createMatch(UUID inviteId, UUID player1Id, UUID player2Id) {
        String sql = "INSERT INTO matches (invite_id, player1_id, player2_id, status) " +
                "VALUES (?, ?, ?, 'WAITING') RETURNING id";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, inviteId, Types.OTHER);
            stmt.setObject(2, player1Id, Types.OTHER);
            stmt.setObject(3, player2Id, Types.OTHER);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    UUID matchId = getUuid(rs, "id");
                    log.info("Match created: id={}, invite={}, player1={}, player2={}",
                            matchId, inviteId, player1Id, player2Id);
                    return matchId;
                }
            }
            return null;
        } catch (SQLException e) {
            log.error("Error creating match", e);
            return null;
        }
    }

    /**
     * Get match by ID
     */
    public GameMatch getMatchById(UUID matchId) {
        String sql = "SELECT * FROM matches WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, matchId, Types.OTHER);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToMatch(rs);
            }
            return null;
        } catch (SQLException e) {
            log.error("Error getting match by ID", e);
            return null;
        }
    }

    /**
     * Update match status
     */
    public boolean updateMatchStatus(UUID matchId, String newStatus) {
        String sql = "UPDATE matches SET status = ? WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus);
            stmt.setObject(2, matchId, Types.OTHER);

            int affected = stmt.executeUpdate();

            if (affected > 0) {
                log.info("Match status updated: id={}, status={}", matchId, newStatus);
                return true;
            }
            return false;
        } catch (SQLException e) {
            log.error("Error updating match status", e);
            return false;
        }
    }

    /**
     * Finish match
     */
    public boolean finishMatch(UUID matchId, UUID winnerId) {
        String sql = "UPDATE matches SET status = 'FINISHED', winner = ?, ended_at = NOW() " +
                "WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (winnerId != null) {
                stmt.setObject(1, winnerId, Types.OTHER);
            } else {
                stmt.setNull(1, Types.OTHER);
            }
            stmt.setObject(2, matchId, Types.OTHER);

            int affected = stmt.executeUpdate();

            if (affected > 0) {
                log.info("Match finished: id={}, winner={}", matchId, winnerId);
                return true;
            }
            return false;
        } catch (SQLException e) {
            log.error("Error finishing match", e);
            return false;
        }
    }

    /**
     * Update player stats after match
     */
    public void updatePlayerStats(UUID playerId, int roundsWon, int totalRounds,
                                  double pointsEarned, boolean isWinner) {
        String sql = "UPDATE player_stats SET " +
                "total_score = total_score + ?, " +
                "total_matches_R = total_matches_R + 1, " +
                "total_R_points = total_R_points + ?, " +
                "total_wins = total_wins + ?, " +
                "total_R = total_R + ? " +
                "WHERE player_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roundsWon);
            stmt.setDouble(2, pointsEarned);
            stmt.setInt(3, isWinner ? 1 : 0);
            stmt.setInt(4, totalRounds);
            stmt.setObject(5, playerId, Types.OTHER);

            stmt.executeUpdate();

            log.info("Player stats updated: player={}, points={}, isWinner={}",
                    playerId, pointsEarned, isWinner);
        } catch (SQLException e) {
            log.error("Error updating player stats", e);
        }
    }

    /**
     * Map ResultSet to GameMatch
     */
    private GameMatch mapResultSetToMatch(ResultSet rs) throws SQLException {
        return GameMatch.builder()
                .id(getUuid(rs, "id"))
                .inviteId(getUuid(rs, "invite_id"))
                .player1Id(getUuid(rs, "player1_id"))
                .player2Id(getUuid(rs, "player2_id"))
                .winner(getUuidNullable(rs, "winner"))
                .status(rs.getString("status"))
                .startedAt(rs.getTimestamp("started_at"))
                .endedAt(rs.getTimestamp("ended_at"))
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

    private UUID getUuidNullable(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String str) {
            return UUID.fromString(str);
        }
        throw new SQLException("Unexpected UUID type for column " + column + ": " + value);
    }
}