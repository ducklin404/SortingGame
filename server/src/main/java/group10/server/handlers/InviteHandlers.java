package group10.server.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import group10.common.dto.Envelope;
import group10.common.net.LengthPrefixedIO;
import group10.common.protocol.ProtocolConstants;
import group10.persistence.dao.InvitesDao;
import group10.server.net.ConnectionRegistry;
import group10.common.util.JsonUtil;
import group10.server.router.MessageHandler;
import group10.server.session.SessionManager;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;
import java.util.UUID;

public class InviteHandlers {

    // handle incoming response
    public static MessageHandler invite(SessionManager sessionManager) {
        return (env, sock) -> {
            JsonNode p = env.getPayload();
            if (p == null || !p.has("toPlayerId")) {
                sendError(sock, "INVALID_INVITE_PAYLOAD");
                return;
            }

            UUID fromUser = sessionManager.getUserId(env.getSessionId());
            UUID toUser = UUID.fromString(p.get("toPlayerId").asText());

            // TODO: implements matchmaking logic
            // For now, send fake "INVITE_SENT" response back to sender.

            ObjectNode payload = JsonUtil.MAPPER.createObjectNode();
            payload.put("fromPlayerId", fromUser.toString());
            payload.put("toPlayerId", toUser.toString());

            Envelope resp = new Envelope("INVITE_SENT", payload, env.getSessionId());
            LengthPrefixedIO.writeObject(sock, resp);
        };
    }


    public static MessageHandler inviteResponse(InvitesDao invitesDao, SessionManager sessionManager, ConnectionRegistry connectionRegistry) {
        return (env, sock) -> {
            JsonNode p = env.getPayload();
            if (p == null || !p.has("inviteId") || !p.has("response")) {
                sendError(sock, "INVALID_INVITE_RESPONSE_PAYLOAD");
                return;
            }

            UUID playerId = sessionManager.getUserId(env.getSessionId());
            UUID inviteId = UUID.fromString(p.get("inviteId").asText());
            String response = p.get("response").asText(); // "OK" or "REJECT"
            Optional<InvitesDao.InviteRecord> invite = invitesDao.getInvite(inviteId);
            System.out.println(playerId);
            if (invite.isEmpty()){
                sendError(sock,"No invite exist");
                return;
            }

            InvitesDao.InviteRecord record = invite.get();
            System.out.println(record.getExpiresAt());
            System.out.println(System.currentTimeMillis());
            System.out.println(record.getExpiresAt() >= System.currentTimeMillis());
            if (record.getExpiresAt() >= System.currentTimeMillis()){
                if (response.equals("OK")){
                    invitesDao.acceptInviteAtomically(inviteId);
                }
                else{
                    invitesDao.rejectInvite(inviteId);
                }


                ObjectNode payload = JsonUtil.MAPPER.createObjectNode();
                payload.put("inviteId", inviteId.toString());
                payload.put("playerId", playerId.toString());
                payload.put("response", response);

                Envelope resp = new Envelope("INVITE_RESPONSE_ACK", payload, env.getSessionId());
                LengthPrefixedIO.writeObject(sock, resp);
            }



        };
    }

    private static void sendError(Socket sock, String code) {
        try {
            ObjectNode payload = JsonUtil.MAPPER.createObjectNode();
            payload.put("error", code);
            Envelope env = new Envelope("ERROR", payload);
            LengthPrefixedIO.writeObject(sock, env);
        } catch (IOException ignored) {}
    }
}
