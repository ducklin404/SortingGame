package group10.client.ui;

import group10.client.network.ClientConnection;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class GameScreen {
    private final VBox root = new VBox(15);
    private final Stage stage;
    private final ClientConnection client;
    private final String matchId;
    private final String opponentName;

    public GameScreen(Stage stage, String matchId, String opponentName, ClientConnection client) {
        this.stage = stage;
        this.client = client;
        this.matchId = matchId;
        this.opponentName = opponentName;
        buildUI();
    }

    private void buildUI() {
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        Label title = new Label("🎮 Trận đấu bắt đầu");
        title.setFont(Font.font("Arial", 22));
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #2E86DE;");

        Label opp = new Label("Đối thủ: " + opponentName);
        opp.setStyle("-fx-font-size: 14; -fx-text-fill: #444;");

        Label mid = new Label("Match ID: " + matchId);
        mid.setStyle("-fx-text-fill: #777;");

        Button back = new Button("🔙 Trở về sảnh");
        back.setOnAction(e -> {
            // Minimal back: close client and go back to login
            try { client.close(); } catch (Exception ignored) {}
            LoginScreen login = new LoginScreen(stage);
            stage.setScene(new javafx.scene.Scene(login.getRoot(), 500, 400));
        });

        root.getChildren().addAll(title, opp, mid, back);
    }

    public VBox getRoot() { return root; }
}