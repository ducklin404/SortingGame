package group10.persistence.impl;

import group10.persistence.dao.PlayerDao;
import group10.persistence.model.Player;
import group10.persistence.model.PlayerStats;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

public class PlayerDaoImpl implements PlayerDao {
    private final DataSource ds;

    public PlayerDaoImpl(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public UUID createPlayer(String username, String hashedPassword, String displayName) {
        String insertPlayer = "INSERT INTO players (username, display_name, hashed_password, created_at) " +
                "VALUES (?, ?, ?, CURRENT_TIMESTAMP) RETURNING id";
        String insertStats = "INSERT INTO player_stats (player_id, total_points, wins, losses, draws, matches_played, last_updated) " +
                "VALUES (?, 0, 0, 0, 0, 0, CURRENT_TIMESTAMP)";

        try (Connection c = ds.getConnection()) {
            // transactional: create player and initialize stats
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(insertPlayer)) {
                ps.setString(1, username);
                ps.setString(2, displayName);
                ps.setString(3, hashedPassword);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        c.rollback();
                        throw new SQLException("Failed to create player");
                    }
                    UUID id = (UUID) rs.getObject(1);

                    try (PreparedStatement ps2 = c.prepareStatement(insertStats)) {
                        ps2.setObject(1, id);
                        ps2.executeUpdate();
                    }

                    c.commit();
                    return id;
                }
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            // Convert to unchecked so callers aren't forced into try/catch forests
            throw new RuntimeException(e);
        }
    }

    @Override
    public Player findById(UUID id) {
        String sql = "SELECT id, username, display_name, hashed_password, created_at FROM players WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapPlayer(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Player findByUsername(String username) {
        String sql = "SELECT id, username, display_name, hashed_password, created_at FROM players WHERE username = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapPlayer(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean updateDisplayName(UUID playerId, String displayName) {
        String sql = "UPDATE players SET display_name = ? WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, displayName);
            ps.setObject(2, playerId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean updatePassword(UUID playerId, String newHashedPassword) {
        String sql = "UPDATE players SET hashed_password = ? WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, newHashedPassword);
            ps.setObject(2, playerId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean deletePlayer(UUID playerId) {
        String sql = "DELETE FROM players WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, playerId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Player> listPlayers(int limit, int offset) {
        String sql = "SELECT id, username, display_name, hashed_password, created_at FROM players ORDER BY created_at DESC LIMIT ? OFFSET ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, limit);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                List<Player> out = new ArrayList<>();
                while (rs.next()) out.add(mapPlayer(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    //  Stats methods 

    @Override
    public PlayerStats getPlayerStats(UUID playerId) {
        String sql = "SELECT player_id, total_points, wins, losses, draws, matches_played, last_updated FROM player_stats WHERE player_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                UUID pid = (UUID) rs.getObject("player_id");
                BigDecimal total = rs.getBigDecimal("total_points");
                int wins = rs.getInt("wins");
                int losses = rs.getInt("losses");
                int draws = rs.getInt("draws");
                int matches = rs.getInt("matches_played");
                Timestamp ts = rs.getTimestamp("last_updated");
                Instant lastUpdated = ts == null ? null : ts.toInstant();
                return new PlayerStats(pid, total == null ? BigDecimal.ZERO : total, wins, losses, draws, matches, lastUpdated);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean upsertPlayerStats(PlayerStats stats) {
        String sql = "INSERT INTO player_stats (player_id, total_points, wins, losses, draws, matches_played, last_updated) " +
                "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (player_id) DO UPDATE SET " +
                "total_points = EXCLUDED.total_points, wins = EXCLUDED.wins, losses = EXCLUDED.losses, draws = EXCLUDED.draws, matches_played = EXCLUDED.matches_played, last_updated = CURRENT_TIMESTAMP";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, stats.getPlayerId());
            ps.setBigDecimal(2, stats.getTotalPoints());
            ps.setInt(3, stats.getWins());
            ps.setInt(4, stats.getLosses());
            ps.setInt(5, stats.getDraws());
            ps.setInt(6, stats.getMatchesPlayed());
            return ps.executeUpdate() >= 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean incrementPlayerStats(UUID playerId, BigDecimal pointsDelta, int winsDelta, int lossesDelta, int drawsDelta, int matchesDelta) {
        // Use a single UPDATE that increments numeric fields; handle NULL total_points with COALESCE.
        String sql = "UPDATE player_stats SET " +
                "total_points = COALESCE(total_points, 0) + ?, " +
                "wins = COALESCE(wins, 0) + ?, " +
                "losses = COALESCE(losses, 0) + ?, " +
                "draws = COALESCE(draws, 0) + ?, " +
                "matches_played = COALESCE(matches_played, 0) + ?, " +
                "last_updated = CURRENT_TIMESTAMP " +
                "WHERE player_id = ?";

        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setBigDecimal(1, pointsDelta == null ? BigDecimal.ZERO : pointsDelta);
            ps.setInt(2, winsDelta);
            ps.setInt(3, lossesDelta);
            ps.setInt(4, drawsDelta);
            ps.setInt(5, matchesDelta);
            ps.setObject(6, playerId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    //  helpers 

    private Player mapPlayer(ResultSet rs) throws SQLException {
        UUID id = (UUID) rs.getObject("id");
        String username = rs.getString("username");
        String display = rs.getString("display_name");
        String hashed = rs.getString("hashed_password");
        Timestamp ts = rs.getTimestamp("created_at");
        Instant created = ts == null ? null : ts.toInstant();
        return new Player(id, username, display, hashed, created);
    }
    @Override
    public List<PlayerStats> getLeaderboard(int limit) {
        String sql = """
        SELECT 
            p.id AS player_id,
            s.total_points,
            s.wins,
            s.losses,
            s.draws,
            s.matches_played,
            s.last_updated
        FROM player_stats s
        JOIN players p ON p.id = s.player_id
        ORDER BY s.total_points DESC
        LIMIT ?
    """;

        List<PlayerStats> list = new ArrayList<>();

        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    UUID playerId = (UUID) rs.getObject("player_id");
                    BigDecimal points = rs.getBigDecimal("total_points");
                    int wins = rs.getInt("wins");
                    int losses = rs.getInt("losses");
                    int draws = rs.getInt("draws");
                    int matches = rs.getInt("matches_played");

                    Timestamp ts = rs.getTimestamp("last_updated");
                    Instant lastUpdated = ts == null ? null : ts.toInstant();

                    PlayerStats stats = new PlayerStats(
                            playerId,
                            points == null ? BigDecimal.ZERO : points,
                            wins,
                            losses,
                            draws,
                            matches,
                            lastUpdated
                    );

                    list.add(stats);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }
}
