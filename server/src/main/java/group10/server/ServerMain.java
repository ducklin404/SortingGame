package group10.server;

import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("✅ Server started on port 12345");
            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("📥 New client connected: " + client.getInetAddress());
                new Thread(new ClientHandler(client)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
