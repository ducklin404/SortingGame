package group10.server.handlers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import group10.common.dto.Envelope;
import group10.common.net.LengthPrefixedIO;
import group10.common.util.JsonUtil;
import group10.common.protocol.ProtocolConstants;
import group10.server.router.MessageHandler;
import group10.server.session.SessionManager;

import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

public class MatchHandlers {
    public static MessageHandler login(SessionManager sessionManager) {
        return (env, sock) -> {
            // dummy login example
            UUID fakeUser = UUID.randomUUID();
            UUID sid = sessionManager.createSession(fakeUser);

            ObjectNode payload = JsonUtil.MAPPER.createObjectNode();
            payload.put("sessionId", sid.toString());
            payload.put("userId", fakeUser.toString());

            Envelope resp = new Envelope(ProtocolConstants.LOGIN_SUCCESS, payload, sid);
            LengthPrefixedIO.writeObject(sock, resp);
        };
    }
}
