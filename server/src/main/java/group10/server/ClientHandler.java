package group10.server;

import group10.common.Message;
import group10.common.MessageType;
import group10.common.Protocol;
import group10.persistence.LeaderboardDAO;
import group10.common.PlayerStat;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    public ClientHandler(Socket socket) {
        this.socket = socket;
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
                    case GET_LEADERBOARD -> handleLeaderboardRequest();
                    default -> sendError("Loại message không hợp lệ: " + msg.getType());
                }
            }

        } catch (IOException e) {
            System.err.println("Client ngắt kết nối: " + e.getMessage());
        } finally {
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
    private void sendMessage(Message msg) {
        try {
            byte[] jsonBytes = msg.toJson().getBytes(Protocol.CHARSET);
            dos.writeInt(jsonBytes.length);
            dos.write(jsonBytes);
            dos.flush();
        } catch (IOException e) {
            System.err.println("Gửi message lỗi: " + e.getMessage());
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

    private void sendError(String text) {
        sendMessage(new Message(MessageType.ERROR, text));
    }
}
