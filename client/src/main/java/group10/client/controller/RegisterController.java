package group10.client.controller;

import group10.client.network.TCPClient;
import group10.client.view.LoginView;
import group10.client.view.RegisterView;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.json.JSONObject;

public class RegisterController {
    private final RegisterView view;
    private final Stage stage;

    public RegisterController(RegisterView view, Stage stage) {
        this.view = view;
        this.stage = stage;
    }

    public void onRegister() {
        try {
            TCPClient client = new TCPClient("localhost", 12345);
            JSONObject req = new JSONObject()
                    .put("type", "REGISTER_REQUEST")
                    .put("payload", new JSONObject()
                            .put("username", view.getUsername())
                            .put("display_name", view.getDisplayName())
                            .put("password", view.getPassword()));
            client.send(req.toString());

            JSONObject res = new JSONObject(client.receive());
            boolean success = res.getJSONObject("payload").getBoolean("success");
            view.showMessage(success ? "✅ Đăng ký thành công!" : "❌ Tên đăng nhập đã tồn tại!");
            client.close();
        } catch (Exception e) {
            view.showMessage("⚠️ Lỗi kết nối server!");
        }
    }

    public void backToLogin(Stage stage) {
        stage.setScene(new Scene(new LoginView(stage).getRoot(), 500, 400));
    }
}
