package group10.persistence.impl;

import group10.persistence.dao.MatchesDao;

import javax.sql.DataSource;
import java.sql.*;
import java.util.UUID;

public class MatchesDaoImpl implements MatchesDao {
    private final DataSource ds;

    public MatchesDaoImpl(DataSource ds) { this.ds = ds; }

    @Override
    public UUID createMatch(UUID playerA, UUID playerB) {
        String insertMatch = "INSERT INTO matches(created_at, started_at) VALUES (now(), now()) RETURNING id";
        String insertPlayers = "INSERT INTO match_players(match_id, player_a, player_b) VALUES (?, ?, ?)";
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement pm = c.prepareStatement(insertMatch);
                 PreparedStatement pp = c.prepareStatement(insertPlayers)) {
                UUID matchId;
                try (ResultSet rs = pm.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Failed to create match");
                    matchId = (UUID) rs.getObject(1);
                }
                pp.setObject(1, matchId);
                pp.setObject(2, playerA);
                pp.setObject(3, playerB);
                pp.executeUpdate();
                c.commit();
                return matchId;
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void finalizeMatch(UUID matchId, int aPoints, int bPoints, String result) {
        String sql = "UPDATE matches SET player_a_points = ?, player_b_points = ?, result = ?, ended_at = now() WHERE id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, aPoints);
            ps.setInt(2, bPoints);
            ps.setString(3, result);
            ps.setObject(4, matchId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
