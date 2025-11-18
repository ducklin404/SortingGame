package group10.persistence.impl;

import group10.persistence.dao.MatchDao;
import group10.persistence.model.Match;
import group10.persistence.model.MatchHistoryItem;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MatchDaoImpl implements MatchDao {
    private final DataSource ds;

    public MatchDaoImpl(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public UUID createMatch(UUID playerAId, UUID playerBId) {
        final String insertMatch = "INSERT INTO matches (created_at, started_at, ended_at, player_a_points, player_b_points, result) " +
                "VALUES (CURRENT_TIMESTAMP, NULL, NULL, 0, 0, NULL) RETURNING id";
        final String insertPlayers = "INSERT INTO match_players (match_id, player_a, player_b) VALUES (?, ?, ?)";

        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(insertMatch)) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        c.rollback();
                        throw new SQLException("Failed to insert match");
                    }
                    UUID matchId = (UUID) rs.getObject(1);

                    try (PreparedStatement ps2 = c.prepareStatement(insertPlayers)) {
                        ps2.setObject(1, matchId);
                        ps2.setObject(2, playerAId);
                        ps2.setObject(3, playerBId);
                        ps2.executeUpdate();
                    }

                    c.commit();
                    return matchId;
                }
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Match findById(UUID matchId) {
        String sql = "SELECT id, created_at, started_at, ended_at, player_a_points, player_b_points, result FROM matches WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, matchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapMatch(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Match> findByPlayer(UUID playerId, int limit, int offset) {
        // Joins match_players to find matches where the player participates.
        String sql = "SELECT m.id, m.created_at, m.started_at, m.ended_at, m.player_a_points, m.player_b_points, m.result " +
                "FROM matches m " +
                "JOIN match_players mp ON mp.match_id = m.id " +
                "WHERE mp.player_a = ? OR mp.player_b = ? " +
                "ORDER BY m.created_at DESC LIMIT ? OFFSET ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, playerId);
            ps.setObject(2, playerId);
            ps.setInt(3, limit);
            ps.setInt(4, offset);
            try (ResultSet rs = ps.executeQuery()) {
                List<Match> out = new ArrayList<>();
                while (rs.next()) out.add(mapMatch(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean startMatch(UUID matchId) {
        String sql = "UPDATE matches SET started_at = CURRENT_TIMESTAMP WHERE id = ? AND started_at IS NULL";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, matchId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean finishMatch(UUID matchId, String result) {
        String sql = "UPDATE matches SET ended_at = CURRENT_TIMESTAMP, result = ? WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, result);
            ps.setObject(2, matchId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    @Override
    public boolean updatePoints(UUID matchId, int playerAPoints, int playerBPoints) {
        String sql = "UPDATE matches SET player_a_points = ?, player_b_points = ? WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, playerAPoints);
            ps.setInt(2, playerBPoints);
            ps.setObject(3, matchId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean deleteMatch(UUID matchId) {
        String sql = "DELETE FROM matches WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, matchId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Match mapMatch(ResultSet rs) throws SQLException {
        UUID id = (UUID) rs.getObject("id");
        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp startedTs = rs.getTimestamp("started_at");
        Timestamp endedTs = rs.getTimestamp("ended_at");
        int aPts = rs.getInt("player_a_points");
        int bPts = rs.getInt("player_b_points");
        String result = rs.getString("result");
        Instant createdAt = createdTs == null ? null : createdTs.toInstant();
        Instant startedAt = startedTs == null ? null : startedTs.toInstant();
        Instant endedAt = endedTs == null ? null : endedTs.toInstant();
        return new Match(id, createdAt, startedAt, endedAt, aPts, bPts, result);
    }
    @Override
    public List<MatchHistoryItem> getMatchHistory(UUID playerId, int limit) {

        String sql =
                "SELECT m.id, m.created_at, " +
                        "       m.player_a_points AS player_a_points, " +
                        "       m.player_b_points AS player_b_points, " +
                        "       m.result, " +
                        "       p1.username AS playerAName, p2.username AS playerBName, " +
                        "       mp.player_a, mp.player_b " +
                        "FROM matches m " +
                        "JOIN match_players mp ON mp.match_id = m.id " +
                        "JOIN players p1 ON p1.id = mp.player_a " +
                        "JOIN players p2 ON p2.id = mp.player_b " +
                        "WHERE mp.player_a = ? OR mp.player_b = ? " +
                        "ORDER BY m.created_at DESC " +
                        "LIMIT ?";

        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, playerId);
            ps.setObject(2, playerId);
            ps.setInt(3, limit);

            try (ResultSet rs = ps.executeQuery()) {

                List<MatchHistoryItem> list = new ArrayList<>();

                while (rs.next()) {

                    UUID matchId = (UUID) rs.getObject("id");

                    Timestamp t = rs.getTimestamp("created_at");
                    Instant createdAt = (t == null ? null : t.toInstant());

                    int aPts = rs.getInt("player_a_points");
                    int bPts = rs.getInt("player_b_points");
                    String result = rs.getString("result");

                    UUID aId = (UUID) rs.getObject("player_a");
                    UUID bId = (UUID) rs.getObject("player_b");

                    String opponent =
                            aId.equals(playerId)
                                    ? rs.getString("playerBName")
                                    : rs.getString("playerAName");

                    int myPts = aId.equals(playerId) ? aPts : bPts;
                    int oppPts = aId.equals(playerId) ? bPts : aPts;

                    list.add(new MatchHistoryItem(
                            matchId,
                            opponent,
                            myPts,
                            oppPts,
                            result,
                            createdAt
                    ));
                }

                return list;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
