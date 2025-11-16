package group10.client;

import group10.common.dto.Envelope;
import group10.common.net.LengthPrefixedIO;
import group10.common.util.JsonUtil;

import java.io.IOException;
import java.net.Socket;
import java.util.UUID;
import java.util.function.Consumer;


public class ClientConnection {
    private final String host;
    private final int port;
    private Socket socket;
    private UUID sessionId;
    private final Consumer<Envelope> messageHandler;

    public ClientConnection(String host, int port, Consumer<Envelope> messageHandler) {
        this.host = host;
        this.port = port;
        this.messageHandler = messageHandler;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        // start read loop on new thread
        Thread t = new Thread(this::readLoop, "client-read-loop");
        t.setDaemon(true);
        t.start();
    }

    private void readLoop() {
        try {
            while (!socket.isClosed()) {
                String json = LengthPrefixedIO.readJson(socket);
                Envelope env = JsonUtil.MAPPER.readValue(json, Envelope.class);
                messageHandler.accept(env);
            }
        } catch (IOException e) {
            // handle disconnect
        }
    }

    public synchronized void send(Envelope env) throws IOException {
        LengthPrefixedIO.writeObject(socket, env);
    }

    public void close() throws IOException {
        if (socket != null) socket.close();
    }

    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }
    public UUID getSessionId() { return sessionId; }
}
