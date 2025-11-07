package group10.client.view;

import group10.client.controller.ForgotPasswordController;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class ForgotPasswordView {
    private VBox root;
    private TextField usernameField;
    private Label messageLabel;
    private final ForgotPasswordController controller;

    public ForgotPasswordView(Stage stage) {
        controller = new ForgotPasswordController(this, stage);
        buildUI(stage);
    }

    private void buildUI(Stage stage) {
        root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(25));

        // 🏷️ Tiêu đề chính
        Label title = new Label("🔐 KHÔI PHỤC MẬT KHẨU");
        title.setFont(Font.font("Arial", 22));
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #2E86DE;");

        Label subtitle = new Label("Nhập tên đăng nhập để gửi yêu cầu khôi phục");
        subtitle.setFont(Font.font("Arial", 13));
        subtitle.setStyle("-fx-text-fill: #555555;");

        // 🧾 Form nhập liệu
        GridPane form = new GridPane();
        form.setAlignment(Pos.CENTER);
        form.setHgap(10);
        form.setVgap(10);

        Label usernameLabel = new Label("Tên đăng nhập:");
        usernameField = new TextField();
        usernameField.setPromptText("Nhập tên đăng nhập...");
        usernameField.setPrefWidth(200);

        GridPane.setHalignment(usernameLabel, HPos.RIGHT);
        form.add(usernameLabel, 0, 0);
        form.add(usernameField, 1, 0);

        // 🔘 Nút gửi yêu cầu
        Button sendBtn = new Button("Gửi yêu cầu khôi phục");
        sendBtn.setPrefWidth(200);
        sendBtn.setOnAction(e -> controller.onSendRequest());

        // 💬 Nhãn thông báo
        messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: red;");

        // 🔗 Liên kết quay lại đăng nhập
        Hyperlink backLink = new Hyperlink("← Quay lại đăng nhập");
        backLink.setStyle("-fx-text-fill: #2980B9; -fx-font-size: 13;");
        backLink.setOnAction(e -> controller.backToLogin(stage));

        // 📦 Gộp tất cả thành giao diện chính
        root.getChildren().addAll(title, subtitle, form, sendBtn, messageLabel, backLink);
    }

    public VBox getRoot() { return root; }
    public String getUsername() { return usernameField.getText(); }
    public void showMessage(String msg) { messageLabel.setText(msg); }
}
