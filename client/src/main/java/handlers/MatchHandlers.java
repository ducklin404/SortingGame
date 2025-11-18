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

/**
 * MatchHandlers – xử lý toàn bộ flow của trận đấu
 * Không thay đổi logic gameplay, chỉ làm đẹp, tối ưu.
 */
public class MatchHandlers {

    // ============================================================
    // =============== START MATCH =================================
    // ============================================================

    public static MessageHandler startMatchHandler(ScreenManager manager, ClientConnection clientConnection) {
        return (env, sock) -> {

            JsonNode payload = env.getPayload();
            String opponent = payload != null && payload.has("opponent")
                    ? payload.get("opponent").asText()
                    : "Opponent";

            SwingUtilities.invokeLater(() -> {
                WaitingPanel waiting = (WaitingPanel) manager.getScreen("waiting");
                if (waiting != null) waiting.setOpponentName(opponent);
                manager.show("waiting");
            });

            // gửi ACK cho server
            ObjectNode ack = JsonUtil.MAPPER.createObjectNode();
            ack.put("received", true);

            try {
                clientConnection.send(ProtocolConstants.START_MATCH_ACK, ack);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }

    // ============================================================
    // =============== START MATCH TIMEOUT =========================
    // ============================================================

    public static MessageHandler startMatchTimeoutHandler(ScreenManager manager) {
        return (env, sock) -> SwingUtilities.invokeLater(() -> {

            JOptionPane.showMessageDialog(
                    null,
                    "Match cancelled (timeout).",
                    "Timeout",
                    JOptionPane.INFORMATION_MESSAGE
            );

            WaitingPanel waiting = (WaitingPanel) manager.getScreen("waiting");
            if (waiting != null)
                waiting.showTemporaryMessage("Match cancelled (timeout)", 2500);

            manager.show("main");
        });
    }

    // ============================================================
    // =============== START ROUND ================================
    // ============================================================

    public static MessageHandler startRoundHandler(ScreenManager manager, RoundPanel roundPanel) {
        return (env, sock) -> {
            JsonNode payload = env.getPayload();

            SwingUtilities.invokeLater(() -> {
                roundPanel.startRound(payload);
                manager.show("round");
            });
        };
    }

    // ============================================================
    // =============== ROUND RESULT ================================
    // ============================================================

    public static MessageHandler roundResultHandler(ScreenManager manager, RoundPanel roundPanel) {
        return (env, sock) -> {
            JsonNode payload = env.getPayload();

            SwingUtilities.invokeLater(() -> {

                roundPanel.onRoundFinished(payload);

                try {
                    WaitingPanel waiting = (WaitingPanel) manager.getScreen("waiting");
                    if (waiting != null)
                        waiting.setLastResult(payload);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Nếu muốn auto chuyển sang waiting:
                // manager.show("waiting");
            });
        };
    }

    // ============================================================
    // =============== MATCH FINISHED ==============================
    // ============================================================

    public static MessageHandler matchFinishedHandler(ScreenManager manager, ClientConnection connection) {
        return (env, sock) -> {
            JsonNode payload = env.getPayload();

            SwingUtilities.invokeLater(() -> {

                // cập nhật WaitingPanel
                try {
                    WaitingPanel waiting = (WaitingPanel) manager.getScreen("waiting");
                    if (waiting != null) waiting.setLastResult(payload);
                } catch (Exception ignored) {}

                // Tạo summary đẹp
                StringBuilder sb = new StringBuilder();
                sb.append("🎉 MATCH FINISHED 🎉\n\n");

                sb.append("Result: ")
                        .append(payload.path("result").asText(resultFallback(payload)))
                        .append("\n\n");

                sb.append("Score:  A ")
                        .append(payload.path("playerAPoint").asInt(0))
                        .append("  -  B ")
                        .append(payload.path("playerBPoint").asInt(0))
                        .append("\n");

                if (payload.has("playerATimeSec") || payload.has("playerBTimeSec")) {
                    sb.append(String.format(
                            "Time A: %s s\nTime B: %s s\n",
                            fmt(payload.get("playerATimeSec")),
                            fmt(payload.get("playerBTimeSec"))
                    ));
                }

                sb.append("\nMessage: ")
                        .append(payload.path("message").asText(""));

                // Hiển thị lựa chọn
                Object[] options = {"Request Rematch", "Exit to Main"};

                int choice = JOptionPane.showOptionDialog(
                        null,
                        sb.toString(),
                        "Match Result",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null,
                        options,
                        options[1]
                );

                // Xử lý lựa chọn
                if (choice == 0) {
                    ObjectNode rematch = JsonUtil.MAPPER.createObjectNode();
                    rematch.put("matchId", payload.path("matchId").asText(""));

                    try {
                        connection.send(ProtocolConstants.REMATCH_REQUEST, rematch);
                        JOptionPane.showMessageDialog(null, "Rematch request sent!", "Rematch", JOptionPane.INFORMATION_MESSAGE);
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(null, "Error sending rematch: " + e.getMessage(), "Network Error", JOptionPane.ERROR_MESSAGE);
                    }

                    manager.show("waiting");
                } else {
                    manager.show("main");
                }
            });
        };
    }

    // ============================================================
    // =============== UTILITY ====================================
    // ============================================================

    private static String resultFallback(JsonNode payload) {
        int a = payload.path("playerAPoint").asInt(0);
        int b = payload.path("playerBPoint").asInt(0);
        if (a > b) return "A_WIN";
        if (b > a) return "B_WIN";
        return "DRAW";
    }

    private static String fmt(JsonNode n) {
        return (n == null || !n.isNumber()) ? "-" : String.format("%.3f", n.asDouble());
    }
}
