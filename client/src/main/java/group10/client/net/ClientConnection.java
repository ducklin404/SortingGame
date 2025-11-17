package group10.client.net;

import com.fasterxml.jackson.databind.JsonNode;
import group10.common.dto.Envelope;
import group10.common.net.LengthPrefixedIO;
import group10.common.util.JsonUtil;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientConnection {
    private final String host;
    private final int port;
    private Socket socket;
    private UUID sessionId; // set after LOGIN_OK
    private Thread readerThread;

    // handlers: messageType -> (envelope, rawSocket)
    private final Map<String, MessageHandler> handlers = new ConcurrentHashMap<>();

    public ClientConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public synchronized void connect() throws IOException {
        if (socket != null && socket.isConnected() && !socket.isClosed()) return;
        socket = new Socket(host, port);
        startReader();
    }

    public synchronized void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
        if (readerThread != null) {
            readerThread.interrupt();
            readerThread = null;
        }
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sid) { this.sessionId = sid; }

    // Register handler for a message type. Handler runs on the network thread; use SwingUtilities.invokeLater inside.
    public void on(String messageType, MessageHandler handler) {
        handlers.put(messageType, handler);
    }

    public void onUi(String messageType, MessageHandler handler) {
        handlers.put(messageType, (env, sock) -> {
            // marshal to EDT
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    handler.handle(env, sock);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }


    // send any object that LengthPrefixedIO.writeObject can handle (Envelope is fine)
    public synchronized void sendEnvelope(Envelope env) throws IOException {
        if (sessionId != null && env.getSessionId() == null) env.setSessionId(sessionId);
        LengthPrefixedIO.writeObject(socket, env);
    }

    // convenience: build Envelope with payload node
    public synchronized void send(String type, JsonNode payload) throws IOException {
        Envelope env = new Envelope(type, payload, sessionId);
        sendEnvelope(env);
    }

    private void startReader() {
        readerThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted() && socket != null && !socket.isClosed()) {
                    String json;
                    try {
                        json = LengthPrefixedIO.readJson(socket);
                    } catch (EOFException eof) {
                        System.out.println("Server closed connection.");
                        break;
                    }
                    // parse envelope
                    Envelope env = JsonUtil.MAPPER.readValue(json, Envelope.class);

                    // dispatch (non-blocking)
                    MessageHandler handler = handlers.get(env.getType());
                    if (handler != null) {
                        try {
                            System.out.println(env.getType());
                            System.out.println(env.getPayload());
                            System.out.println(env.getSessionId());
                            handler.handle(env, socket);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("Unhandled message: " + env.getType());
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                try { if (socket != null) socket.close(); } catch (IOException ignored) {}
            }
        }, "Client-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }
}
