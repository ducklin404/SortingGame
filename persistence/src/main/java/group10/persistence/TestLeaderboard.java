package group10.persistence;

import java.util.List;

public class TestLeaderboard {
    public static void main(String[] args) {
        LeaderboardDAO dao = new LeaderboardDAO();
        List<PlayerStat> leaderboard = dao.getLeaderboard();

        System.out.println("===== 🏆 BẢNG XẾP HẠNG =====");
        System.out.printf("%-15s | %-10s | %-5s | %-5s | %-5s | %-5s%n",
                "Người chơi", "Điểm", "Thắng", "Thua", "Hòa", "Trận");

        for (PlayerStat p : leaderboard) {
            System.out.printf("%-15s | %-10.2f | %-5d | %-5d | %-5d | %-5d%n",
                    p.getUsername(), p.getTotalPoints(), p.getWins(), p.getLosses(), p.getDraws(), p.getMatchesPlayed());
        }
    }
}
