package group10.server.handlers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import group10.common.dto.Envelope;
import group10.common.net.LengthPrefixedIO;
import group10.common.protocol.ProtocolConstants;
import group10.common.util.JsonUtil;
import group10.persistence.dao.PlayerDao;
import group10.persistence.model.Player;
import group10.persistence.model.PlayerStats;
import group10.server.router.MessageHandler;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class LeaderboardHandlers {

    public static MessageHandler getLeaderboard(PlayerDao playerDao) {

        return (env, sock) -> {

            UUID sessionId = env.getSessionId();

            // 1. Lấy top 20 người chơi
            List<PlayerStats> items = playerDao.getLeaderboard(20);

            // 2. Tạo JSON thủ công
            ArrayNode arr = JsonUtil.MAPPER.createArrayNode();

            for (PlayerStats s : items) {

                ObjectNode row = arr.addObject();

                // Lấy username từ database
                Player p = playerDao.findById(s.getPlayerId());

                row.put("playerId", s.getPlayerId().toString());
                row.put("username", (p != null ? p.getUsername() : "Unknown"));

                row.put("totalPoints", s.getTotalPoints());
                row.put("wins", s.getWins());
                row.put("losses", s.getLosses());
                row.put("draws", s.getDraws());
                row.put("matchesPlayed", s.getMatchesPlayed());
            }

            ObjectNode payload = JsonUtil.MAPPER.createObjectNode();
            payload.set("items", arr);

            Envelope resp = new Envelope(
                    ProtocolConstants.LEADERBOARD_DATA,
                    payload,
                    sessionId
            );

            try {
                LengthPrefixedIO.writeObject(sock, resp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }
}
