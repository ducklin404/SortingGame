package handlers;

import com.fasterxml.jackson.databind.JsonNode;
import group10.client.ui.HistoryPanel;
import group10.client.ui.ScreenManager;
import group10.client.net.MessageHandler;

import javax.swing.*;

public class HistoryHandlers {

    /**
     * Handler nhận MATCH_HISTORY_DATA từ server
     */
    public static MessageHandler historyHandler(ScreenManager manager) {

        return (env, sock) -> {

            JsonNode payload = env.getPayload();
            if (payload == null || !payload.has("items")) {
                System.out.println("Invalid MATCH_HISTORY_DATA payload");
                return;
            }

            JsonNode items = payload.get("items");

            SwingUtilities.invokeLater(() -> {
                HistoryPanel panel = (HistoryPanel) manager.getScreen("history");
                if (panel != null) {
                    panel.updateHistory(items);   // cập nhật bảng lịch sử
                }

                manager.show("history");         // chuyển sang màn hình history
            });
        };
    }
}
