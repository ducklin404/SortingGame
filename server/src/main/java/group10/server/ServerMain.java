package group10.server;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ServerMain {
    private static final int PORT = 2206;
    private static final ExecutorService pool = Executors.newFixedThreadPool(10);
    
    // Shared managers for all client handlers
    private static final SessionManager sessionManager = new SessionManager();
    private static final MatchmakingManager matchmakingManager = new MatchmakingManager(sessionManager);

    public static void main(String[] args) {
        System.out.println("Server đang khởi động trên cổng " + PORT + " ...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server sẵn sàng nhận kết nối!");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("🔗 Client mới: " + clientSocket.getInetAddress());
                pool.execute(new ClientHandler(clientSocket, sessionManager, matchmakingManager));
            }

        } catch (IOException e) {
            System.err.println("Lỗi server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            pool.shutdown();
        }
    }
}
