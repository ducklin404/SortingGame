package group10.server.handlers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import group10.common.dto.Envelope;
import group10.common.net.LengthPrefixedIO;
import group10.common.protocol.ProtocolConstants;
import group10.common.util.JsonUtil;
import group10.persistence.dao.MatchDao;
import group10.persistence.dao.PlayerDao;
import group10.persistence.model.MatchHistoryItem;
import group10.server.router.MessageHandler;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class HistoryHandlers {

    public static MessageHandler getMatchHistory(MatchDao matchDao, PlayerDao playerDao) {

        return (env, sock) -> {

            if (env.getPayload() == null || !env.getPayload().has("username")) {
                System.out.println("GET_MATCH_HISTORY missing username");
                return;
            }

            String username = env.getPayload().get("username").asText();

            // tìm playerId
            var player = playerDao.findByUsername(username);
            if (player == null) {
                System.out.println("Player not found: " + username);
                return;
            }

            UUID playerId = player.getId();

            // lấy lịch sử 20 trận
            List<MatchHistoryItem> list = matchDao.getMatchHistory(playerId, 20);

            ObjectNode payload = JsonUtil.MAPPER.createObjectNode();
            payload.set("items", JsonUtil.MAPPER.valueToTree(list));

            Envelope resp = new Envelope(
                    ProtocolConstants.MATCH_HISTORY_DATA,
                    payload,
                    env.getSessionId()
            );

            try {
                LengthPrefixedIO.writeObject(sock, resp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }
}
