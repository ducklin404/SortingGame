package handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import group10.client.net.ClientConnection;
import group10.client.net.MessageHandler;
import group10.client.ui.RoundPanel;
import group10.client.ui.ScreenManager;
import group10.client.ui.WaitingPanel;
import group10.common.util.JsonUtil;
import group10.common.protocol.ProtocolConstants;

import javax.swing.*;
import java.io.IOException;
import java.util.Objects;

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


    public static MessageHandler startRoundHandler(ScreenManager manager, RoundPanel roundPanel) {
        return (env, sock) -> {
            JsonNode payload = env.getPayload();
            // ensure UI changes happen on EDT — if you register with onUi, it's already EDT.
            javax.swing.SwingUtilities.invokeLater(() -> {
                // populate and show round panel
                roundPanel.startRound(payload);
                manager.show("round"); // make sure "round" screen is registered in ScreenManager
            });
        };
    }

    public static MessageHandler roundResultHandler(ScreenManager manager, RoundPanel roundPanel) {
        return (env, sock) -> {
            JsonNode payload = env.getPayload();
            javax.swing.SwingUtilities.invokeLater(() -> {
                // Update round panel
                roundPanel.onRoundFinished(payload);

                // Also update waiting panel (so if user is on waiting screen they see the last result)
                try {
                    WaitingPanel wp = (WaitingPanel) manager.getScreen("waiting");
                    if (wp != null) wp.setLastResult(payload);
                } catch (Exception e) {
                    // defensive: don't crash UI if waiting panel missing
                    e.printStackTrace();
                }

                // Optional: you can automatically switch to waiting screen after result,
                // comment/uncomment per desired UX:
                // manager.show("waiting");
            });
        };
    }

    public static MessageHandler matchFinishedHandler(ScreenManager manager, ClientConnection clientConnection) {
        return (env, sock) -> {
            JsonNode payload = env.getPayload();

            // run UI work on EDT
            SwingUtilities.invokeLater(() -> {
                // show last result in WaitingPanel too (non-fatal if missing)
                try {
                    WaitingPanel wp = (WaitingPanel) manager.getScreen("waiting");
                    if (wp != null) wp.setLastResult(payload);
                } catch (Exception ignored) {}

                // build a friendly message
                StringBuilder sb = new StringBuilder();
                sb.append("Match finished\n\n");
                if (Objects.equals(payload.path("result").asText(), "DRAW")){
                    sb.append("Draw!").append("\n\n");
                } else if (Objects.equals(payload.path("winnerId").asText(), clientConnection.getSessionId().toString())) {
                    sb.append("You win!").append("\n\n");
                }else{
                    sb.append("You lose!").append("\n\n");
                }
                System.out.println(clientConnection.getSessionId().toString());


                sb.append("Score: A ").append(payload.path("playerAPoint").asInt(0))
                        .append("  -  B ").append(payload.path("playerBPoint").asInt(0)).append("\n");
                if (payload.has("playerATimeSec") || payload.has("playerBTimeSec")) {
                    sb.append(String.format(
                            "Time A: %s s  Time B: %s s\n",
                            fmt3(payload.get("playerATimeSec")),
                            fmt3(payload.get("playerBTimeSec"))
                    ));
                }


                // options
                Object[] options = new Object[] { "Request Rematch", "Exit to Main" };
                int chosen = JOptionPane.showOptionDialog(
                        null,
                        sb.toString(),
                        "Match finished",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null,
                        options,
                        options[1] // default to Exit
                );

                // chosen == 0 => Rematch, chosen == 1 or -1 => Exit
                if (chosen == 0) {
                    // Build rematch payload (server may expect other fields; adapt as necessary)
                    ObjectNode out = JsonUtil.MAPPER.createObjectNode();
                    out.put("matchId", payload.path("matchId").asText(""));
                    try {
                        clientConnection.send(ProtocolConstants.REMATCH_REQUEST, out);
                        // provide immediate feedback
                        JOptionPane.showMessageDialog(null, "Rematch request sent.", "Rematch", JOptionPane.INFORMATION_MESSAGE);
                    } catch (IOException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Failed to send rematch request: " + e.getMessage(),
                                "Network error", JOptionPane.ERROR_MESSAGE);
                    }
                    // switch to waiting screen where player waits for opponent decision
                    manager.show("waiting");
                } else {
                    // Exit to main screen
                    manager.show("main");
                }
            });
        };
    }

    // small helper: compute result string if server didn't include "result"
    private static String computeResultFallback(JsonNode payload) {
        int a = payload.path("playerAPoint").asInt(0);
        int b = payload.path("playerBPoint").asInt(0);
        if (a > b) return "A_WIN";
        if (b > a) return "B_WIN";
        return "DRAW";
    }

    private static String fmt3(JsonNode n) {
        if (n == null || !n.isNumber()) return "-";
        return String.format("%.3f", n.asDouble());
    }



}
