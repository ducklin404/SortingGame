package group10.persistence;

import group10.common.Player;
import group10.persistence.Database;
import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.sql.*;
import java.util.UUID;

@Slf4j
public class PlayerDAO {
    private final Database dbConnection;

    public PlayerDAO() {
        this.dbConnection = Database.getInstance();
    }

    /**
     * Verify player login
     */
    public Player verifyLogin(String username, String password) {
        String sql = "SELECT id, username, display_name, password_hash " +
                "FROM players WHERE username = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (storedHash.equals(hashPassword(password))) {
                    return Player.builder()
                            .id(getUuid(rs, "id"))
                            .username(rs.getString("username"))
                            .displayName(rs.getString("display_name"))
                            .build();
                }
            }
            return null;
        } catch (SQLException e) {
            log.error("Error verifying login", e);
            return null;
        }
    }

    /**
     * Get player by ID
     */
    public Player getPlayerById(UUID playerId) {
        String sql = "SELECT id, username, display_name FROM players WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, playerId, Types.OTHER);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Player.builder()
                        .id(getUuid(rs, "id"))
                        .username(rs.getString("username"))
                        .displayName(rs.getString("display_name"))
                        .build();
            }
            return null;
        } catch (SQLException e) {
            log.error("Error getting player by ID", e);
            return null;
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
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
}