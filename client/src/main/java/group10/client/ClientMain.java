package group10.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import group10.client.net.ClientConnection;
import group10.client.net.ClientMessageRouter;
import group10.client.ui.*;
import group10.common.protocol.ProtocolConstants;
import group10.common.util.JsonUtil;
import handlers.MatchHandlers;
import handlers.LeaderboardHandlers;
import handlers.HistoryHandlers;


import javax.swing.*;
import java.io.IOException;

public class ClientMain {
    public static void main(String[] args) throws IOException {

        // 1. CONNECT
        ClientConnection clientConnection = new ClientConnection("127.0.0.1", 9000);
        clientConnection.connect();

        // 2. ROUTER
        ClientMessageRouter router = new ClientMessageRouter();

        // 3. Handler nhận PONG để lấy sessionId
        clientConnection.on(ProtocolConstants.PONG, (env, sock) -> {
            clientConnection.setSessionId(env.getSessionId());
            System.out.println("Received SessionId = " + clientConnection.getSessionId());
        });

        // 4. BUILD UI
        SwingUtilities.invokeLater(() -> {

            JFrame frame = new JFrame("Sorting Match - Client");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 620);
            frame.setLocationRelativeTo(null);

            ScreenManager manager = new ScreenManager(frame);

            // Panels
            MainMenuPanel mainMenu = new MainMenuPanel(manager, clientConnection);
            PlayPanel playPanel = new PlayPanel(manager);
            WaitingPanel waitingPanel = new WaitingPanel();
            RoundPanel roundPanel = new RoundPanel(clientConnection);
            LeaderboardPanel leaderboardPanel = new LeaderboardPanel(manager);
            HistoryPanel historyPanel = new HistoryPanel(manager);



            // Register screens
            manager.registerScreen("leaderboard", leaderboardPanel);
            manager.registerScreen("main", mainMenu);
            manager.registerScreen("play", playPanel);
            manager.registerScreen("waiting", waitingPanel);
            manager.registerScreen("round", roundPanel);
            manager.registerScreen("history", historyPanel);

            // ========== REGISTER HANDLERS ==========
            router.add(ProtocolConstants.START_MATCH,
                    MatchHandlers.startMatchHandler(manager, clientConnection));

            router.add(ProtocolConstants.START_MATCH_TIMEOUT,
                    MatchHandlers.startMatchTimeoutHandler(manager));

            router.add(ProtocolConstants.START_ROUND,
                    MatchHandlers.startRoundHandler(manager, roundPanel));

            router.add(ProtocolConstants.ROUND_RESULT,
                    MatchHandlers.roundResultHandler(manager, roundPanel));

            router.add(ProtocolConstants.MATCH_RESULT,
                    MatchHandlers.matchFinishedHandler(manager, clientConnection));

            router.add(ProtocolConstants.LEADERBOARD_DATA,
                    LeaderboardHandlers.leaderboardHandler(manager));

            router.add(ProtocolConstants.MATCH_HISTORY_DATA,
                    HistoryHandlers.historyHandler(manager));



            // 🔴 THÊM ĐOẠN NÀY
            router.add(ProtocolConstants.ERROR, (env, sock) -> {
                System.out.println("== ERROR từ server ==");
                System.out.println("type  : " + env.getType());
                System.out.println("payload: " + env.getPayload());
            });

            // ========== GẮN ROUTER VÀO CONNECTION ==========
            router.registerAll(clientConnection, true);

            // ========== BÂY GIỜ MỚI SEND PING ==========
            try {
                ObjectNode pingPayload = JsonUtil.MAPPER.createObjectNode();
                clientConnection.send(ProtocolConstants.PING, pingPayload);
            } catch (IOException e) {
                e.printStackTrace();
            }

            manager.show("main");
            frame.setVisible(true);
        });
    }
}
