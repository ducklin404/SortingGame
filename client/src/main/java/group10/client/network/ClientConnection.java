package group10.client.network;

import group10.common.Message;
import group10.common.MessageType;
import group10.common.Protocol;

import java.io.*;
import java.net.Socket;

/**
 * ClientConnection — Quản lý việc kết nối TCP tới Server,
 * gửi/nhận Message JSON có format:
 *
 * [4 byte đầu: độ dài JSON][n byte: dữ liệu JSON]
 */
public class ClientConnection {
    private final String host;
    private final int port;
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    public ClientConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Kết nối tới server
     */
    public boolean connect() {
        try {
            socket = new Socket(host, port);
            socket.setSoTimeout(Protocol.SOCKET_TIMEOUT);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            System.out.println("Kết nối thành công tới server: " + host + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("Không thể kết nối tới server: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gửi message JSON tới server theo chuẩn:
     * [4 byte độ dài][JSON nội dung]
     */
    public void sendMessage(Message msg) {
        try {
            byte[] jsonBytes = msg.toJson().getBytes(Protocol.CHARSET);
            int length = jsonBytes.length;

            dos.writeInt(length);    // Gửi độ dài JSON (4 byte)
            dos.write(jsonBytes);    // Gửi nội dung JSON
            dos.flush();

            System.out.println("Đã gửi: " + msg.toJson());
        } catch (IOException e) {
            System.err.println("Gửi dữ liệu thất bại: " + e.getMessage());
        }
    }

    /**
     * Nhận message JSON từ server
     * Đọc 4 byte đầu tiên để biết độ dài gói, rồi đọc toàn bộ JSON.
     */
    public Message receiveMessage() {
        try {
            int length = dis.readInt(); // Đọc độ dài
            if (length <= 0) return null;

            byte[] buffer = new byte[length];
            dis.readFully(buffer); // Đọc đủ số byte JSON

            String json = new String(buffer, Protocol.CHARSET);
            System.out.println("Nhận từ server: " + json);
            return Message.fromJson(json);
        } catch (EOFException e) {
            System.err.println("Server đã đóng kết nối.");
            return null;
        } catch (IOException e) {
            System.err.println("Lỗi khi nhận dữ liệu: " + e.getMessage());
            return null;
        }
    }

    /**
     * 🔚 Đóng kết nối an toàn
     */
    public void close() {
        try {
            if (socket != null) socket.close();
            System.out.println("Đã đóng kết nối tới server.");
        } catch (IOException ignored) {}
    }

    // 🔧 Test nhanh độc lập (dùng để kiểm tra kết nối)
    public static void main(String[] args) {
        ClientConnection client = new ClientConnection("localhost", 2206);

        if (!client.connect()) return;

        // Gửi yêu cầu lấy BXH
        client.sendMessage(new Message(MessageType.GET_LEADERBOARD, null));

        // Nhận phản hồi từ server
        Message response = client.receiveMessage();
        if (response != null)
            System.out.println("Nhận được phản hồi: " + response);

        client.close();
    }
}
