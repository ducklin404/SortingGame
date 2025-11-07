package group10.client.controller;

import group10.client.network.TCPClient;
import group10.client.view.ForgotPasswordView;
import group10.client.view.LoginView;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.json.JSONObject;

public class ForgotPasswordController {
    private final ForgotPasswordView view;
    private final Stage stage;

    public ForgotPasswordController(ForgotPasswordView view, Stage stage) {
        this.view = view;
        this.stage = stage;
    }

    public void onSendRequest() {
        try {
            TCPClient client = new TCPClient("localhost", 12345);

            JSONObject req = new JSONObject()
                    .put("type", "FORGOT_PASSWORD_REQUEST")
                    .put("payload", new JSONObject().put("username", view.getUsername()));

            client.send(req.toString());
            JSONObject res = new JSONObject(client.receive());

            if(res.getJSONObject("payload").getBoolean("success")) {
                String newPassword = res.getJSONObject("payload").getString("new_password");
                view.showMessage("Mật khẩu mới là: " + newPassword);
            }else {
                view.showMessage("Sai tên đăng nhập !");
            }
            client.close();
        } catch (Exception e) {
            view.showMessage("⚠️ Không thể gửi yêu cầu đến server!");
        }
    }

    public void backToLogin(Stage stage) {
        stage.setScene(new Scene(new LoginView(stage).getRoot(), 500, 400));
    }
}
