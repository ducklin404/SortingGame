package handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import group10.client.net.ClientConnection;
import group10.client.net.MessageHandler;
import group10.client.ui.ScreenManager;
import group10.client.ui.WaitingPanel;
import group10.common.util.JsonUtil;
import group10.common.protocol.ProtocolConstants;

import javax.swing.*;
import java.io.IOException;

public class MatchHandlers {

    public static MessageHandler startMatchHandler(ScreenManager manager, ClientConnection clientConnection) {
        return (env, sock) -> {
            JsonNode payload = env.getPayload();
            System.out.println(payload);
            String opponent = payload != null && payload.has("opponent")
                    ? payload.get("opponent").asText(null)
                    : "opponent";

            // UI: manager.show must be called on EDT; if you used ClientConnection.onUi when registering,
            // this runs on EDT already. If not, wrap with SwingUtilities.invokeLater here.
            JPanel screen = manager.getScreen("waiting");
            if (screen instanceof WaitingPanel) {
                ((WaitingPanel) screen).setOpponentName(opponent);
            }
            manager.show("waiting");

            // ACK
            ObjectNode ackPayload = JsonUtil.MAPPER.createObjectNode();
            ackPayload.put("received", true);
            try {
                clientConnection.send(ProtocolConstants.START_MATCH_ACK, ackPayload);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }

    public static MessageHandler startMatchTimeoutHandler(ScreenManager manager) {
        return (env, sock) -> {
            // Make sure UI work runs on EDT. If you already register with onUi, this will run on EDT
            // and invokeLater simply runs code immediately; it's safe either way.
            SwingUtilities.invokeLater(() -> {
                // show a tiny notification/dialog
                JOptionPane.showMessageDialog(
                        null,
                        "Match cancelled (timeout).",
                        "Match cancelled",
                        JOptionPane.INFORMATION_MESSAGE
                );

                // return to main screen
                try {
                    WaitingPanel wp = (WaitingPanel) manager.getScreen("waiting");
                    if (wp != null) wp.showTemporaryMessage("Match cancelled (timeout)", 2500);
                    manager.show("main");
                } catch (Exception e) {
                    // defensive: if show throws, at least log it
                    e.printStackTrace();
                }
            });
        };
    }

}
