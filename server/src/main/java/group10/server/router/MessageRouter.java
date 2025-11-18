package group10.server.router;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import group10.common.dto.Envelope;
import group10.common.net.LengthPrefixedIO;
import group10.common.protocol.ProtocolConstants;
import group10.common.util.JsonUtil;
import group10.server.net.ConnectionRegistry;
import group10.server.session.SessionManager;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;


public class MessageRouter implements Runnable {
    private final Socket clientSocket;
    private final Map<String, MessageHandler> handlers;
    private final SessionManager sessionManager;
    private final ExecutorService handlerExecutor;
    private final ConnectionRegistry connectionRegistry;

    // remember the last valid sessionId observed on this connection so we can unregister on close
    private UUID lastSeenSessionId = null;

    public MessageRouter(Socket clientSocket,
                         Map<String, MessageHandler> handlers,
                         SessionManager sessionManager,
                         ExecutorService handlerExecutor,
                         ConnectionRegistry connectionRegistry) {
        this.clientSocket = clientSocket;
        this.handlers = handlers;
        this.sessionManager = sessionManager;
        this.handlerExecutor = handlerExecutor;
        this.connectionRegistry = connectionRegistry;
    }

    @Override
    public void run() {
        try {
            while (!clientSocket.isClosed()) {
                String json;
                try {
                    json = LengthPrefixedIO.readJson(clientSocket);
                } catch (EOFException e) {
                    // client closed connection
                    break;
                }

                Envelope env;
                try {
                    env = JsonUtil.MAPPER.readValue(json, Envelope.class);
                } catch (Exception e) {
                    sendError(ProtocolConstants.ERROR, "INVALID_JSON");
                    continue;
                }

                if (env == null || env.getType() == null) {
                    sendError(ProtocolConstants.ERROR, "INVALID_ENVELOPE");
                    continue;
                }

                // Validate session for protected messages
                if (requiresAuth(env.getType())) {
                    UUID sid = env.getSessionId();
                    if (sid == null || !sessionManager.isActive(sid)) {
                        sendError(ProtocolConstants.ERROR, "NOT_AUTHENTICATED");
                        continue;
                    }

                         else {
                        // touch heartbeat and register socket to session
                        sessionManager.touchHeartbeat(sid);

                        // register connection
                        connectionRegistry.register(sid, clientSocket);
                        lastSeenSessionId = sid;
                    }
                }

                System.out.println(env.getType());
                MessageHandler handler = handlers.get(env.getType());
                if (handler == null) {
                    sendError(ProtocolConstants.ERROR, "UNKNOWN_TYPE");
                    continue;
                }

                // run handler async so single slow message does not block reading loop
                UUID finalLastSeen = lastSeenSessionId;
                handlerExecutor.submit(() -> {
                    try {
                        handler.handle(env, clientSocket);
                    } catch (Exception ex) {
                        // IN STACKTRACE RA ĐỂ XEM LỖI THẬT
                        ex.printStackTrace();
                        try {
                            sendError(ProtocolConstants.ERROR, "HANDLER_ERROR");
                        } catch (IOException ioe) {
                            // ignore, client likely disconnected
                        }
                    }
                });
            }
        } catch (IOException ex) {
            // socket error
        } finally {
            // cleanup
            if (lastSeenSessionId != null) {
                connectionRegistry.unregister(lastSeenSessionId, clientSocket);
            }
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
        }
    }

    private boolean requiresAuth(String type) {
        // extend ProtocolConstants if needed and use constants for safety
        switch (type) {
            case ProtocolConstants.LOGIN:
            case ProtocolConstants.PING:
            case ProtocolConstants.PONG:
            case ProtocolConstants.GET_LEADERBOARD:
                return false;
            default:
                return true;
        }
    }

    private void sendError(String type, String code) throws IOException {
        ObjectNode payload = JsonUtil.MAPPER.createObjectNode().put("error", code);
        Envelope resp = new Envelope(type, payload, null);
        LengthPrefixedIO.writeObject(clientSocket, resp);
    }
}
