package group10.client;

import group10.common.Message;
import group10.common.MessageType;
import group10.common.Protocol;
import group10.common.TCPProtocol;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.*;
import java.io.*;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class GameClient {
    private Socket socket;
    private InputStream in;
    private OutputStream out;

    @Getter
    private String sessionId;

    @Getter
    private UUID playerId;

    @Getter
    private String username;

    private Thread listenerThread;
    private Thread heartbeatThread;
    private volatile boolean running = false;

    // Message queue for UI thread
    private final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();

    @Setter
    private MessageHandler messageHandler;

    /**
     * Connect to server
     */
    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            socket.setSoTimeout(Protocol.SOCKET_TIMEOUT);
            in = socket.getInputStream();
            out = socket.getOutputStream();

            running = true;

            // Start message listener thread
            listenerThread = new Thread(this::listenForMessages);
            listenerThread.setDaemon(true);
            listenerThread.start();

            log.info("Connected to server: {}:{}", host, port);
            return true;
        } catch (IOException e) {
            log.error("Failed to connect to server", e);
            return false;
        }
    }

    /**
     * Login to server
     */
    public boolean login(String username, String password) {
        try {
            Message loginMsg = new Message(MessageType.LOGIN)
                    .put("username", username)
                    .put("password", password);

            sendMessage(loginMsg);
            // Wait for response (synchronous for login)
            Message response = receiveMessage();

            if (response == null) {
                log.warn("No response received for login attempt");
                return false;
            }

            String status = response.optString("status");
            boolean successType = response.getType() == MessageType.LOGIN_SUCCESS;
            boolean successStatus = Protocol.STATUS_SUCCESS.equalsIgnoreCase(status);

            if (successType || successStatus) {
                this.sessionId = response.optString("sessionId");
                this.playerId = response.optUUID("playerId");
                this.username = response.optString("username");

                // Start heartbeat
                startHeartbeat();

                log.info("Login successful: {}", username);
                return true;
            } else {
                String message = response.optString("message");
                log.warn("Login failed: {}", message != null ? message : "unknown reason");
                return false;
            }
        } catch (Exception e) {
            log.error("Login error", e);
            return false;
        }
    }

    /**
     * Send message to server
     */
    public void sendMessage(Message msg) {
        try {
            synchronized (out) {
                TCPProtocol.sendMessage(out, msg);
            }
        } catch (IOException e) {
            log.error("Error sending message", e);
            disconnect();
        }
    }

    /**
     * Receive message from server (blocking)
     */
    private Message receiveMessage() throws IOException {
        return TCPProtocol.receiveMessage(in);
    }

    /**
     * Listen for messages from server
     */
    private void listenForMessages() {
        try {
            while (running) {
                Message msg = receiveMessage();

                // Add to queue for UI thread
                messageQueue.offer(msg);

                // Also notify handler directly
                if (messageHandler != null) {
                    messageHandler.onMessageReceived(msg);
                }
            }
        } catch (IOException e) {
            if (running) {
                log.error("Connection lost", e);
                disconnect();
            }
        }
    }

    /**
     * Start heartbeat sender
     */
    private void startHeartbeat() {
        heartbeatThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(5000); // Send every 5 seconds

                    Message heartbeat = new Message(MessageType.HEARTBEAT)
                            .put("sessionId", sessionId);
                    sendMessage(heartbeat);

                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    /**
     * Disconnect from server
     */
    public void disconnect() {
        running = false;

        try {
            if (sessionId != null) {
                Message logoutMsg = new Message(MessageType.LOGOUT)
                        .put("sessionId", sessionId);
                sendMessage(logoutMsg);
            }

            if (heartbeatThread != null) {
                heartbeatThread.interrupt();
            }

            TCPProtocol.closeSocket(socket);

            log.info("Disconnected from server");
        } catch (Exception e) {
            log.error("Error during disconnect", e);
        }
    }

    /**
     * Get next message from queue (non-blocking)
     */
    public Message pollMessage() {
        return messageQueue.poll();
    }

    /**
     * Check if connected
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed() && running;
    }

    /**
     * Interface for message handling
     */
    public interface MessageHandler {
        void onMessageReceived(Message message);
    }
}
