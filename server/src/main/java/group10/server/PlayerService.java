package group10.server;

import group10.persistence.Database;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.sql.*;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class PlayerService {
    private final String LOGIN_USER = "SELECT id, display_name, hashed_password FROM players WHERE username=?";
    private final String REGISTER_USER = "INSERT INTO players(username, display_name, hashed_password) VALUES (?, ?, ?)";
    private final String UPDATE_PASSWORD_USER= "UPDATE players SET hashed_password = ? WHERE username = ?";
    private final String CHECK_USER = "SELECT 1 FROM players WHERE username=?";


    public boolean register(String username, String displayName, String password) {
        try (Connection conn = Database.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(REGISTER_USER);
            ps.setString(1, username);
            ps.setString(2, displayName);
            ps.setString(3, hashSHA256(password));
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public Optional<JSONObject> login(String username, String password) {
        try (Connection conn = Database.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(LOGIN_USER);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getString("hashed_password").equals(hashSHA256(password))) {
                UUID userId = UUID.fromString(rs.getString("id"));

                upsertSession(userId);
                JSONObject result = new JSONObject();
                result.put("displayName", rs.getString("display_name"));
                result.put("id", rs.getString("id"));
                return Optional.of(result);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<String> forgotPassword(String username){
        try(Connection conn = Database.getConnection()){
            PreparedStatement check = conn.prepareStatement(CHECK_USER);
            check.setString(1, username);
            ResultSet rs = check.executeQuery();

            if(rs.next()){
                String tempPassword = generateTempPassword(1);
                String hashed = hashSHA256(tempPassword);

                PreparedStatement ps = conn.prepareStatement(UPDATE_PASSWORD_USER);
                ps.setString(1, hashed);
                ps.setString(2, username);
                ps.executeUpdate();

                return Optional.of(tempPassword);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public JSONArray getActivePlayer(String userId){
        try(Connection conn = Database.getConnection()) {
            JSONArray res = new JSONArray();
            String query = """
                    SELECT p.display_name,
                    CASE
                        WHEN EXISTS (
                            SELECT 1 FROM match_players mp
                            JOIN matches m ON mp.match_id = m.id
                            WHERE (m.started_at IS NOT NULL AND m.ended_at IS NULL)
                              AND (mp.player_a = p.id OR mp.player_b = p.id)
                        ) THEN 'Đang trong trận'
                        ELSE 'Online'
                    END AS status
                    FROM players p
                    JOIN sessions s
                    ON p.id = s.user_id
                    WHERE s.is_active = TRUE
                        AND p.id <> ?;
                    """;
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setObject(1, UUID.fromString(userId));

            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                JSONObject player = new JSONObject();
                player.put("display_name", rs.getString("display_name"));
                player.put("status", rs.getString("status"));
                res.put(player);
            }
            return  res;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void upsertSession(UUID userId) {
        try(Connection conn = Database.getConnection()){
            PreparedStatement check = conn.prepareStatement("SELECT id FROM sessions WHERE user_id = ?");
            check.setObject(1, userId);
            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                // Cập nhật lại session có sẵn
                PreparedStatement update = conn.prepareStatement("""
                        UPDATE sessions
                        SET is_active = TRUE,
                            started_at = CURRENT_TIMESTAMP,
                            last_heartbeat = CURRENT_TIMESTAMP
                        WHERE user_id = ?;
                    """);
                update.setObject(1, userId);
                update.executeUpdate();
            } else {
                // Tạo session mới
                PreparedStatement insert = conn.prepareStatement("""
                        INSERT INTO sessions (user_id, started_at, last_heartbeat, is_active)
                        VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE);
                    """);
                insert.setObject(1, userId);
                insert.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void logout(UUID userId){
        try(Connection conn = Database.getConnection()){
            PreparedStatement ps = conn.prepareStatement("""
                        UPDATE sessions
                        SET is_active = FALSE,
                            last_heartbeat = CURRENT_TIMESTAMP
                        WHERE user_id = ?;
                    """);
            ps.setObject(1, userId);
            ps.executeUpdate();
            System.out.println("👋 User " + userId + " logged out (set inactive).");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static String generateTempPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static String hashSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes("UTF-8"));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
