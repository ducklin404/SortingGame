package group10.client.controller;

import group10.client.network.TCPClient;
import group10.client.view.LoginView;
import group10.client.view.PlayerListView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PlayerListController {
    private final PlayerListView view;
    private final Stage stage;
    private final TCPClient client;

    public PlayerListController(PlayerListView view, Stage stage, TCPClient client) {
        this.view = view;
        this.stage = stage;
        this.client = client;
    }

    public void fetchPlayerList(String userId) {
        try {
            JSONObject req = new JSONObject()
                    .put("type", "PLAYER_LIST_REQUEST")
                    .put("payload", new JSONObject().put("userId", userId));
            client.send(req.toString());

            JSONObject res = new JSONObject(client.receive());
            JSONArray players = res.getJSONObject("payload").getJSONArray("players");

            ObservableList<Map<String, String>> data = FXCollections.observableArrayList();
            for (int i = 0; i < players.length(); i++) {
                JSONObject p = players.getJSONObject(i);
                Map<String, String> row = new HashMap<>();
                row.put("display_name", p.getString("display_name"));
                row.put("status", p.getString("status"));
                data.add(row);
            }

            view.setPlayers(data);
            view.showMessage("✅ Danh sách cập nhật thành công!");

        } catch (Exception e) {
            e.printStackTrace();
            view.showMessage("⚠️ Không thể tải danh sách người chơi!");
        }
    }

    public void logout(String userId) {
        try {
            JSONObject req = new JSONObject()
                    .put("type", "LOGOUT_REQUEST")
                    .put("payload", new JSONObject().put("userId", userId));
            client.send(req.toString());
            client.close();

            stage.setScene(new Scene(new LoginView(stage).getRoot(), 500, 400));
        } catch (Exception e) {
            e.printStackTrace();
            view.showMessage("⚠️ Lỗi khi đăng xuất!");
        }
    }
}
