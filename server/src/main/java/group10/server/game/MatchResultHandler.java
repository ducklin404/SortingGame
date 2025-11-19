package group10.server.game;

import group10.persistence.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class MatchResultHandler {

    private static final String UPDATE_SQL =
            "UPDATE player_stats ps " +
                    "SET total_points = total_points + ?, " +
                    "    wins = wins + ?, " +
                    "    losses = losses + ?, " +
                    "    draws = draws + ?, " +
                    "    matches_played = matches_played + 1, " +
                    "    last_updated = CURRENT_TIMESTAMP " +
                    "FROM players p " +
                    "WHERE ps.player_id = p.id AND p.username = ?";

    public static void updatePlayerStats(String playerA, String playerB, String result) {
        try (Connection conn = Database.getInstance().getConnection()) {
            // If your connection is not in auto-commit mode, uncomment the next line
            // conn.setAutoCommit(true); or call conn.commit() after updates

            if (result.equalsIgnoreCase("A_WIN")) {
                updateOne(conn, playerA, 10.0, 1, 0, 0);
                updateOne(conn, playerB, 0.0, 0, 1, 0);
            } else if (result.equalsIgnoreCase("B_WIN")) {
                updateOne(conn, playerA, 0.0, 0, 1, 0);
                updateOne(conn, playerB, 10.0, 1, 0, 0);
            } else if (result.equalsIgnoreCase("DRAW")) {
                updateOne(conn, playerA, 5.0, 0, 0, 1);
                updateOne(conn, playerB, 5.0, 0, 0, 1);
            }

            System.out.println("Result updated: " + result);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateOne(Connection conn, String username, double points,
                                  int win, int lose, int draw) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {
            ps.setDouble(1, points);
            ps.setInt(2, win);
            ps.setInt(3, lose);
            ps.setInt(4, draw);
            ps.setString(5, username != null ? username.trim() : null);

            int rows = ps.executeUpdate();
            if (rows == 0) {
                System.err.println("No rows updated for username: '" + username + "'");
                // optional: throw or handle this case
            } else {
                System.out.println("Updated rows: " + rows + " for username: " + username);
            }
        }
    }
}
