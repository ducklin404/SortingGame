package group10.client.controller;

import group10.client.network.TCPClient;
import group10.client.view.*;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.IOException;

public class LoginController {
    private final LoginView view;
    private final Stage stage;
    private TCPClient client;

    public LoginController(LoginView view, Stage stage) {
        this.view = view;
        this.stage = stage;
    }

    public void onLogin() throws IOException {
        try {
            client = new TCPClient("localhost", 12345);

            JSONObject req = new JSONObject()
                    .put("type", "LOGIN_REQUEST")
                    .put("payload", new JSONObject()
                            .put("username", view.getUsername())
                            .put("password", view.getPassword()));

            client.send(req.toString());
            JSONObject res = new JSONObject(client.receive());
            JSONObject payload = res.getJSONObject("payload");

            if (payload.getBoolean("success")) {
                String displayName = payload.getString("displayName");
                String userId = payload.getString("id");

                // ✅ Truyền client sang PlayerListView để giữ kết nối
                PlayerListView listView = new PlayerListView(stage, displayName, userId, client);
                stage.setScene(new Scene(listView.getRoot(), 500, 400));

            } else {
                view.showMessage("❌ Sai tên đăng nhập hoặc mật khẩu!");
                client.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
            view.showMessage("⚠️ Không thể kết nối đến server!");
            if (client != null) client.close();
        }
    }

    public void onRegister(Stage stage) {
        stage.setScene(new Scene(new RegisterView(stage).getRoot(), 500, 400));
    }

    public void onForgotPassword(Stage stage) {
        stage.setScene(new Scene(new ForgotPasswordView(stage).getRoot(), 500, 400));
    }
}
