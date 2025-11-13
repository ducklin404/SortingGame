package group10.server;

import group10.persistence.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class MatchResultHandler {

    // Cập nhật điểm sau trận
    public static void updatePlayerStats(String playerA, String playerB, String result) {
        String sql =
                "UPDATE player_stats SET " +
                        "total_points = total_points + ?, " +
                        "wins = wins + ?, " +
                        "losses = losses + ?, " +
                        "draws = draws + ?, " +
                        "matches_played = matches_played + 1, " +
                        "last_updated = CURRENT_TIMESTAMP " +
                        "WHERE player_id = (SELECT id FROM players WHERE username = ?);";

        try (Connection conn = Database.getInstance().getConnection()) {
            PreparedStatement ps = conn.prepareStatement(sql);

            // Nếu A thắng
            if (result.equalsIgnoreCase("A_WIN")) {
                updateOne(ps, conn, playerA, 10, 1, 0, 0);
                updateOne(ps, conn, playerB, 0, 0, 1, 0);
            }
            // Nếu B thắng
            else if (result.equalsIgnoreCase("B_WIN")) {
                updateOne(ps, conn, playerA, 0, 0, 1, 0);
                updateOne(ps, conn, playerB, 10, 1, 0, 0);
            }
            // Nếu hòa
            else if (result.equalsIgnoreCase("DRAW")) {
                updateOne(ps, conn, playerA, 5, 0, 0, 1);
                updateOne(ps, conn, playerB, 5, 0, 0, 1);
            }

            System.out.println(" Đã cập nhật kết quả trận: " + result);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateOne(PreparedStatement ps, Connection conn,
                                  String username, double points,
                                  int win, int lose, int draw) throws Exception {
        ps.setDouble(1, points);
        ps.setInt(2, win);
        ps.setInt(3, lose);
        ps.setInt(4, draw);
        ps.setString(5, username);
        ps.executeUpdate();
    }
}
