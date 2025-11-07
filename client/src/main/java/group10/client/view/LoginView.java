package group10.client.view;

import group10.client.controller.LoginController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginView {
    private VBox root;
    private TextField usernameField;
    private PasswordField passwordField;
    private TextField passwordVisibleField;
    private Label messageLabel;
    private final LoginController controller;

    public LoginView(Stage stage) {
        controller = new LoginController(this, stage);
        buildUI(stage);
    }

    private void buildUI(Stage stage) {
        root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(25));

        // 🏷️ Tiêu đề
        Label title = new Label("🎮 SORT GAME");
        title.setFont(Font.font("Arial", 26));
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #2E86DE;");

        Label subtitle = new Label("Sắp xếp và chiến thắng cùng bạn bè!");
        subtitle.setFont(Font.font("Arial", 14));
        subtitle.setStyle("-fx-text-fill: #555555;");

        // 🧱 Form đăng nhập (label + input cùng hàng)
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setAlignment(Pos.CENTER);

        Label usernameLabel = new Label("Tên đăng nhập:");
        usernameField = new TextField();
        usernameField.setPromptText("Nhập tên đăng nhập...");
        usernameField.setPrefWidth(200);

        Label passwordLabel = new Label("Mật khẩu:");
        passwordField = new PasswordField();
        passwordField.setPromptText("Nhập mật khẩu...");
        passwordField.setPrefWidth(200);

        // Trường hiển thị mật khẩu (khi tick)
        passwordVisibleField = new TextField();
        passwordVisibleField.setPrefWidth(200);
        passwordVisibleField.setManaged(false);
        passwordVisibleField.setVisible(false);
        passwordVisibleField.setPromptText("Nhập mật khẩu...");

        // Liên kết 2 trường (đồng bộ giá trị)
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());

        // Thêm checkbox "Hiển thị mật khẩu"
        CheckBox showPasswordCheck = new CheckBox("Hiển thị mật khẩu");
        showPasswordCheck.setOnAction(e -> {
            boolean show = showPasswordCheck.isSelected();
            passwordVisibleField.setVisible(show);
            passwordVisibleField.setManaged(show);
            passwordField.setVisible(!show);
            passwordField.setManaged(!show);
        });

        // Thêm các thành phần vào form
        form.add(usernameLabel, 0, 0);
        form.add(usernameField, 1, 0);
        form.add(passwordLabel, 0, 1);
        form.add(passwordField, 1, 1);
        form.add(passwordVisibleField, 1, 1);
        form.add(showPasswordCheck, 1, 2);

        // 🔘 Nút đăng nhập
        Button loginBtn = new Button("Đăng nhập");
        loginBtn.setPrefWidth(200);
        loginBtn.setOnAction(e -> {
            try {
                controller.onLogin();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        // 🔗 Liên kết phụ
        Hyperlink registerLink = new Hyperlink("Đăng ký tài khoản mới");
        registerLink.setOnAction(e -> controller.onRegister(stage));

        Hyperlink forgotLink = new Hyperlink("Quên mật khẩu?");
        forgotLink.setOnAction(e -> controller.onForgotPassword(stage));

        // 💬 Nhãn hiển thị thông báo
        messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: red;");

        // 📦 Gộp tất cả vào layout chính
        root.getChildren().addAll(
                title,
                subtitle,
                form,
                loginBtn,
                registerLink,
                forgotLink,
                messageLabel
        );
    }

    public VBox getRoot() { return root; }
    public String getUsername() { return usernameField.getText(); }
    public String getPassword() { return passwordField.isVisible() ? passwordField.getText() : passwordVisibleField.getText(); }
    public void showMessage(String msg) { messageLabel.setText(msg); }
}
