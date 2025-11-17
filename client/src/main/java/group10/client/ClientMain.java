package group10.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import group10.client.net.ClientConnection;
import group10.client.net.ClientMessageRouter;
import group10.client.ui.*;
import group10.common.dto.Envelope;
import group10.common.net.LengthPrefixedIO;
import group10.common.protocol.ProtocolConstants;
import group10.common.util.JsonUtil;
import handlers.MatchHandlers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.swing.SwingUtilities;
import java.io.IOException;

import javax.swing.*;
import java.io.IOException;
import java.util.UUID;

import static group10.common.protocol.ProtocolConstants.*;


public class ClientMain {
    public static void main(String[] args) throws IOException {
        ClientConnection clientConnection = new ClientConnection("127.0.0.1", 9000);
        clientConnection.connect();
        ClientMessageRouter router = new ClientMessageRouter();

        MatchHandlers matchHandlers = new MatchHandlers();


        // test (send a ping to receive the session id)
        clientConnection.on(ProtocolConstants.PONG, (env, sock) -> {
            clientConnection.setSessionId(env.getSessionId());
            System.out.println(clientConnection.getSessionId());
        });
        ObjectNode payload = JsonUtil.MAPPER.createObjectNode();

        try {
            clientConnection.send(PING, payload);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        // init the UI
        SwingUtilities.invokeLater(() -> {
            // top-level frame and the screen manager
            JFrame frame = new JFrame("Sorting Match - Client");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 620);
            frame.setLocationRelativeTo(null);

            ScreenManager manager = new ScreenManager(frame);


            // Create UI screens
            MainMenuPanel mainMenu = new MainMenuPanel(manager, clientConnection);
            PlayPanel playPanel = new PlayPanel(manager);
            WaitingPanel waitingPanel = new WaitingPanel();
            RoundPanel roundPanel = new RoundPanel(clientConnection);

            // Register screens
            manager.registerScreen("main", mainMenu);
            manager.registerScreen("play", playPanel);
            manager.registerScreen("waiting", waitingPanel);
            manager.registerScreen("round", roundPanel);


            router.add(ProtocolConstants.START_MATCH,
                    MatchHandlers.startMatchHandler(manager, clientConnection));
            router.add(ProtocolConstants.START_MATCH_TIMEOUT,
                    MatchHandlers.startMatchTimeoutHandler(manager));
            router.add(ProtocolConstants.START_ROUND, MatchHandlers.startRoundHandler(manager, roundPanel));
            router.add(ProtocolConstants.ROUND_RESULT, MatchHandlers.roundResultHandler(manager, roundPanel));

            router.registerAll(clientConnection, true);


            // Show main menu
            manager.show("main");

            frame.setVisible(true);


        });
    }
}
