package group10.server.handlers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import group10.common.dto.Envelope;
import group10.common.net.LengthPrefixedIO;
import group10.common.util.JsonUtil;
import group10.common.protocol.ProtocolConstants;
import group10.server.game.MatchInstance;
import group10.server.game.MatchManager;
import group10.server.router.MessageHandler;
import group10.server.session.SessionManager;

import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

public class MatchHandlers {
    public static MessageHandler start_match_ack(SessionManager sessionManager, MatchManager matchManager) {
        return (env, sock) -> {
            UUID sessionId = env.getSessionId();
            if (sessionId == null) {
                // ignore or send error
                return;
            }

            boolean started = matchManager.recordAck(sessionId);
        };
    }

    public static MessageHandler submit_round(SessionManager sessionManager, MatchManager matchManager) {
        return (env, sock) -> {
            UUID sessionId = env.getSessionId();
            if (sessionId == null) return;

            // payload should include matchId and roundId
            ObjectNode payload = (ObjectNode) env.getPayload();
            if (payload == null) return;

            String matchIdStr = payload.path("matchId").asText();
            if (matchIdStr == null) return;
            UUID matchId = UUID.fromString(matchIdStr);

            MatchInstance inst = matchManager.getActiveInstance(matchId);
            if (inst == null) {
                System.out.println("maybe match hasn't started yet or timed out");
                return;
            }

            // hand off to match instance
            inst.handleSubmission(sessionId, payload);
        };
    }



}
