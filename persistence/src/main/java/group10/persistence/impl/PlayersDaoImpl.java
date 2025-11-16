package group10.persistence.impl;

import group10.persistence.dao.PlayersDao;
import org.mindrot.jbcrypt.BCrypt;

import javax.sql.DataSource;
import java.sql.*;
import java.util.UUID;

public class PlayersDaoImpl implements PlayersDao {
    private final DataSource ds;

    public PlayersDaoImpl(DataSource ds) { this.ds = ds; }

    @Override
    public UUID authenticate(String username, String plaintextPassword) {
        String sql = "SELECT id, hashed_password FROM players WHERE username = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                UUID id = (UUID) rs.getObject("id");
                String hashed = rs.getString("hashed_password");
                if (BCrypt.checkpw(plaintextPassword, hashed)) return id;
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UUID findByUsername(String username) {
        String sql = "SELECT id FROM players WHERE username = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return (UUID) rs.getObject("id");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
