package group10.server;

import group10.common.Message;
import group10.common.MessageType;
import group10.common.Protocol;
import group10.persistence.LeaderboardDAO;
import group10.common.PlayerStat;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.UUID;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final SessionManager sessionManager;
    private final MatchmakingManager matchmakingManager;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String sessionId;

    public ClientHandler(Socket socket, SessionManager sessionManager, MatchmakingManager matchmakingManager) {
        this.socket = socket;
        this.sessionManager = sessionManager;
        this.matchmakingManager = matchmakingManager;
    }

    @Override
    public void run() {
        try {
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            System.out.println("Client kết nối: " + socket.getInetAddress());

            while (true) {
                Message msg = receiveMessage(); // ⬅️ Nhận message đúng chuẩn
                if (msg == null) break;

                switch (msg.getType()) {
                    case LOGIN -> handleLogin(msg);
                    case LOGOUT -> handleLogout();
                    case HEARTBEAT -> handleHeartbeat();
                    case GET_LEADERBOARD -> handleLeaderboardRequest();
                    case INVITE -> handleInvite(msg);
                    case INVITE_RESPONSE -> handleInviteResponse(msg);
                    default -> sendError("Loại message không hợp lệ: " + msg.getType());
                }
            }

        } catch (IOException e) {
            System.err.println("Client ngắt kết nối: " + e.getMessage());
        } finally {
            // Cleanup on disconnect
            if (sessionId != null) {
                java.util.UUID pid = getPlayerId();
                if (pid != null) {
                    matchmakingManager.handlePlayerDisconnect(pid);
                }
                sessionManager.logout(sessionId);
            }
            try { socket.close(); } catch (IOException ignored) {}
            System.out.println("Đã đóng kết nối: " + socket.getInetAddress());
        }
    }

    /** 📥 Nhận message theo format [4 byte length] + JSON */
    private Message receiveMessage() {
        try {
            int length = dis.readInt(); // đọc độ dài JSON
            byte[] buffer = new byte[length];
            dis.readFully(buffer);

            String json = new String(buffer, Protocol.CHARSET);
            return Message.fromJson(json);
        } catch (IOException e) {
            return null;
        }
    }

    /** Gửi message theo format mới */
    public void sendMessage(Message msg) {
        try {
            byte[] jsonBytes = msg.toJson().getBytes(Protocol.CHARSET);
            dos.writeInt(jsonBytes.length);
            dos.write(jsonBytes);
            dos.flush();
        } catch (IOException e) {
            System.err.println("Gửi message lỗi: " + e.getMessage());
        }
    }

    /** 🔐 Xử lý đăng nhập */
    private void handleLogin(Message msg) {
        try {
            JSONObject payload = getPayloadAsObject(msg);
            String username = payload.optString("username", "");
            String password = payload.optString("password", "");

            if (username.isEmpty() || password.isEmpty()) {
                sendMessage(new Message(MessageType.LOGIN_FAILED, "Username and password required"));
                return;
            }

            String newSessionId = sessionManager.login(username, password, this);
            
            if (newSessionId != null) {
                this.sessionId = newSessionId;
                PlayerSession session = sessionManager.getSession(newSessionId);
                
                Message response = new Message(MessageType.LOGIN_SUCCESS);
                response.put("sessionId", newSessionId);
                response.put("playerId", session.getPlayer().getId().toString());
                response.put("username", session.getPlayer().getUsername());
                response.put("displayName", session.getPlayer().getDisplayName());
                
                sendMessage(response);
                System.out.println("Login successful: " + username);
            } else {
                sendMessage(new Message(MessageType.LOGIN_FAILED, "Invalid username or password"));
                System.out.println("Login failed: " + username);
            }
        } catch (Exception e) {
            System.err.println("Error handling login: " + e.getMessage());
            e.printStackTrace();
            sendMessage(new Message(MessageType.LOGIN_FAILED, "Server error during login"));
        }
    }

    /** 🚪 Xử lý đăng xuất */
    private void handleLogout() {
        if (sessionId != null) {
            sessionManager.logout(sessionId);
            sessionId = null;
        }
    }

    /** 💓 Xử lý heartbeat */
    private void handleHeartbeat() {
        if (sessionId != null) {
            sessionManager.updateHeartbeat(sessionId);
        }
    }

    /** 🎮 Xử lý lời mời */
    private void handleInvite(Message msg) {
        if (sessionId == null) {
            sendError("Not logged in");
            return;
        }

        try {
            JSONObject payload = getPayloadAsObject(msg);
            String toPlayerStr = payload.optString("to_player_id", "");

            if (toPlayerStr.isBlank()) {
                String toUsername = payload.optString("to", "");
                if (!toUsername.isEmpty()) {
                    sendError("Please use to_player_id");
                    return;
                }
                sendError("Invalid invite target");
                return;
            }

            UUID toPlayerId = UUID.fromString(toPlayerStr);

            PlayerSession session = sessionManager.getSession(sessionId);
            if (session == null) {
                sendError("Session not found");
                return;
            }

            boolean success = matchmakingManager.sendInvite(session.getPlayer().getId(), toPlayerId);
            if (!success) {
                sendError("Failed to send invite");
            }
        } catch (Exception e) {
            System.err.println("Error handling invite: " + e.getMessage());
            e.printStackTrace();
            sendError("Error processing invite");
        }
    }

    /** ✅ Xử lý phản hồi lời mời */
    private void handleInviteResponse(Message msg) {
        if (sessionId == null) {
            sendError("Not logged in");
            return;
        }

        try {
            JSONObject payload = getPayloadAsObject(msg);
            String inviteIdStr = payload.optString("invite_id", "");
            String status = payload.optString("status", "");

            if (inviteIdStr.isBlank() || status.isEmpty()) {
                sendError("Invalid invite response");
                return;
            }

            UUID inviteId = UUID.fromString(inviteIdStr);

            PlayerSession session = sessionManager.getSession(sessionId);
            if (session == null) {
                sendError("Session not found");
                return;
            }

            matchmakingManager.handleInviteResponse(session.getPlayer().getId(), inviteId, status);
        } catch (Exception e) {
            System.err.println("Error handling invite response: " + e.getMessage());
            e.printStackTrace();
            sendError("Error processing invite response");
        }
    }

    /** 🏆 Xử lý yêu cầu lấy BXH */
    private void handleLeaderboardRequest() {
        try {
            LeaderboardDAO dao = new LeaderboardDAO();
            List<PlayerStat> leaderboard = dao.getLeaderboard();

            sendMessage(new Message(MessageType.LEADERBOARD_DATA, leaderboard));
            System.out.println("Đã gửi BXH cho client");
        } catch (Exception e) {
            sendError("Lỗi khi lấy BXH: " + e.getMessage());
        }
    }

    /** Helper: Get payload as JSONObject */
    private JSONObject getPayloadAsObject(Message msg) {
        Object payload = msg.getPayload();
        if (payload instanceof JSONObject) {
            return (JSONObject) payload;
        } else if (payload instanceof String) {
            return new JSONObject((String) payload);
        } else {
            return new JSONObject();
        }
    }

    /** Helper: Get player ID from session */
    private UUID getPlayerId() {
        if (sessionId == null) return null;
        PlayerSession session = sessionManager.getSession(sessionId);
        return session != null ? session.getPlayer().getId() : null;
    }

    private void sendError(String text) {
        sendMessage(new Message(MessageType.ERROR, text));
    }
}
