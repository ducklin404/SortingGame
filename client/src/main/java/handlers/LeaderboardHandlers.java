package handlers;

import com.fasterxml.jackson.databind.JsonNode;
import group10.client.net.MessageHandler;
import group10.client.ui.ScreenManager;
import group10.client.ui.LeaderboardPanel;

import javax.swing.*;

public class LeaderboardHandlers {

    /**
     * Handler cho LEADERBOARD_DATA từ server
     */
    public static MessageHandler leaderboardHandler(ScreenManager manager) {

        return (env, sock) -> {

            JsonNode payload = env.getPayload();
            if (payload == null || !payload.has("items")) {
                System.out.println("Invalid LEADERBOARD_DATA payload");
                return;
            }

            JsonNode items = payload.get("items");

            // Cập nhật UI trên EDT
            SwingUtilities.invokeLater(() -> {
                // lấy panel leaderboard
                LeaderboardPanel panel = (LeaderboardPanel) manager.getScreen("leaderboard");
                if (panel != null) {
                    panel.updateLeaderboard(items);
                }

                // chuyển sang màn hình leaderboard
                manager.show("leaderboard");
            });
        };
    }
}
