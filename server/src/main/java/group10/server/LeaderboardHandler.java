package group10.server;

import group10.common.Message;
import group10.common.MessageType;
import group10.persistence.LeaderboardDAO;
import group10.common.PlayerStat;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class LeaderboardHandler {

    /**
     * Lấy danh sách BXH từ DB và trả về message JSON để gửi cho client.
     */
    public static Message getLeaderboardMessage() {
        try {
            LeaderboardDAO dao = new LeaderboardDAO();
            List<PlayerStat> leaderboard = dao.getLeaderboard();

            // Chuyển danh sách PlayerStat sang JSONArray
            JSONArray arr = new JSONArray();
            for (PlayerStat ps : leaderboard) {
                JSONObject obj = new JSONObject();
                obj.put("username", ps.getUsername());
                obj.put("totalPoints", ps.getTotalPoints());
                obj.put("wins", ps.getWins());
                obj.put("losses", ps.getLosses());
                obj.put("draws", ps.getDraws());
                obj.put("matchesPlayed", ps.getMatchesPlayed());
                arr.put(obj);
            }

            // Gửi dưới dạng JSON string thật
            return new Message(MessageType.LEADERBOARD_DATA, arr.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(MessageType.ERROR, "Không thể lấy dữ liệu BXH: " + e.getMessage());
        }
    }
}
