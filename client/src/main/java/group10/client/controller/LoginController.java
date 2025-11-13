package group10.client.controller;

import group10.client.network.ClientConnection;
import group10.client.ui.LoginScreen;
import group10.client.ui.PlayerListView;
import group10.common.Message;
import group10.common.MessageType;
import group10.common.Protocol;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.json.JSONArray;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.json.JSONObject;

public class LoginController {
    private final LoginScreen view;
    private final Stage stage;
    private ClientConnection client;

    public LoginController(LoginScreen view, Stage stage) {
        this.view = view;
        this.stage = stage;
    }

    public void onLogin() {
        try {
            client = new ClientConnection(Protocol.SERVER_HOST, Protocol.SERVER_PORT);
            if (!client.connect()) {
                view.showMessage("⚠️ Không thể kết nối đến server!");
                return;
            }

            Message req = new Message(MessageType.LOGIN);
            req.put("username", view.getUsername());
            req.put("password", view.getPassword());
            client.sendMessage(req);

            JSONArray bufferedPlayersArray = null;

            while (true) {
                Message msg = client.receiveMessage();
                if (msg == null) {
                    view.showMessage("⚠️ Không nhận được phản hồi từ server!");
                    client.close();
                    return;
                }
                if (msg.getType() == MessageType.LOGIN_SUCCESS) {
                    JSONObject payload = msg.getPayloadAsObject();
                    String displayName = payload.optString("displayName");
                    String userId = payload.optString("playerId");

                    PlayerListView listView = new PlayerListView(stage, displayName, userId, client);
                    stage.setScene(new Scene(listView.getRoot(), 500, 400));

                    if (bufferedPlayersArray != null) {
                        ObservableList<java.util.Map<String, String>> data = FXCollections.observableArrayList();
                        for (int i = 0; i < bufferedPlayersArray.length(); i++) {
                            JSONObject p = bufferedPlayersArray.getJSONObject(i);
                            String pid = p.optString("playerId", "");
                            if (pid.equals(userId)) continue;
                            java.util.Map<String, String> row = new java.util.HashMap<>();
                            String display = p.optString("displayName", p.optString("username", ""));
                            row.put("display_name", display);
                            row.put("status", p.optString("status", ""));
                            row.put("player_id", pid);
                            data.add(row);
                        }
                        listView.setPlayers(data);
                        listView.showMessage("✅ Danh sách cập nhật");
                    }
                    break;
                } else if (msg.getType() == MessageType.LOGIN_FAILED) {
                    view.showMessage("❌ Sai tên đăng nhập hoặc mật khẩu!");
                    client.close();
                    return;
                } else if (msg.getType() == MessageType.ONLINE_LIST) {
                    try {
                        JSONObject payload = msg.getPayloadAsObject();
                        JSONArray arr = payload.optJSONArray("players");
                        if (arr != null) bufferedPlayersArray = arr;
                    } catch (Exception ignored) {}
                } else {
                    // ignore other messages during login
                }
            }
        } catch (Exception e) {
            view.showMessage("⚠️ Không thể kết nối đến server!");
            if (client != null) client.close();
        }
    }

    public void onRegister(Stage stage) {
        view.showMessage("Tính năng đăng ký chưa khả dụng.");
    }

    public void onForgotPassword(Stage stage) {
        view.showMessage("Tính năng quên mật khẩu chưa khả dụng.");
    }
}
