package group10.client.view;

import group10.client.controller.RegisterController;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class RegisterView {
    private VBox root;
    private TextField usernameField, displayNameField;
    private PasswordField passwordField;
    private Label messageLabel;
    private final RegisterController controller;

    public RegisterView(Stage stage) {
        controller = new RegisterController(this, stage);
        buildUI(stage);
    }

    private void buildUI(Stage stage) {
        root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(25));

        // 🏷️ Tiêu đề
        Label title = new Label("📝 ĐĂNG KÝ TÀI KHOẢN");
        title.setFont(Font.font("Arial", 22));
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #2E86DE;");

        // 🧱 Form nhập liệu (label + input cùng hàng)
        GridPane form = new GridPane();
        form.setAlignment(Pos.CENTER);
        form.setHgap(10);
        form.setVgap(10);

        Label usernameLabel = new Label("Tên đăng nhập:");
        usernameField = new TextField();
        usernameField.setPromptText("Nhập tên đăng nhập...");
        usernameField.setPrefWidth(200);

        Label displayNameLabel = new Label("Tên hiển thị:");
        displayNameField = new TextField();
        displayNameField.setPromptText("Nhập tên hiển thị...");
        displayNameField.setPrefWidth(200);

        Label passwordLabel = new Label("Mật khẩu:");
        passwordField = new PasswordField();
        passwordField.setPromptText("Nhập mật khẩu...");
        passwordField.setPrefWidth(200);

        // Căn phải các nhãn
        GridPane.setHalignment(usernameLabel, HPos.RIGHT);
        GridPane.setHalignment(displayNameLabel, HPos.RIGHT);
        GridPane.setHalignment(passwordLabel, HPos.RIGHT);

        // Thêm các hàng vào GridPane
        form.add(usernameLabel, 0, 0);
        form.add(usernameField, 1, 0);
        form.add(displayNameLabel, 0, 1);
        form.add(displayNameField, 1, 1);
        form.add(passwordLabel, 0, 2);
        form.add(passwordField, 1, 2);

        // 🔘 Nút đăng ký
        Button registerBtn = new Button("Đăng ký");
        registerBtn.setPrefWidth(200);
        registerBtn.setOnAction(e -> controller.onRegister());

        // 💬 Nhãn thông báo
        messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: red;");

        // 🔗 Hyperlink quay lại đăng nhập
        Hyperlink backLink = new Hyperlink("← Quay lại đăng nhập");
        backLink.setOnAction(e -> controller.backToLogin(stage));
        backLink.setStyle("-fx-text-fill: #2980B9; -fx-font-size: 13;");

        // 📦 Ghép tất cả vào root
        root.getChildren().addAll(title, form, registerBtn, messageLabel, backLink);
    }

    public VBox getRoot() { return root; }
    public String getUsername() { return usernameField.getText(); }
    public String getDisplayName() { return displayNameField.getText(); }
    public String getPassword() { return passwordField.getText(); }
    public void showMessage(String msg) { messageLabel.setText(msg); }
}
