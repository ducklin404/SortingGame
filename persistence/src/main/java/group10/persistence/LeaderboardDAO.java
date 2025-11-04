package group10.persistence;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardDAO {

    /**
     * 📊 Lấy danh sách bảng xếp hạng (top 20 người chơi)
     * Sắp xếp theo: tổng điểm giảm dần, sau đó theo số trận thắng giảm dần.
     */
    public List<PlayerStat> getLeaderboard() {
        List<PlayerStat> list = new ArrayList<>();
        String sql = """
                SELECT p.username, ps.total_points, ps.wins, ps.losses, ps.draws, ps.matches_played
                FROM player_stats ps
                JOIN players p ON p.id = ps.player_id
                ORDER BY ps.total_points DESC, ps.wins DESC
                LIMIT 20;
                """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(new PlayerStat(
                        rs.getString("username"),
                        rs.getDouble("total_points"),
                        rs.getInt("wins"),
                        rs.getInt("losses"),
                        rs.getInt("draws"),
                        rs.getInt("matches_played")
                ));
            }

        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi lấy dữ liệu bảng xếp hạng:");
            e.printStackTrace();
        }

        return list;
    }
}
