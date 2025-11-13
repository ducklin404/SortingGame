package group10.client.ui;

import group10.client.controller.PlayerListController;
import group10.client.network.ClientConnection;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Map;

public class PlayerListView {
    private final VBox root = new VBox(15);
    private final TableView<Map<String, String>> table = new TableView<>();
    private final Label message = new Label();
    private Button challengeBtn;
    private String currentUserId;
    private final PlayerListController controller;

    public PlayerListView(Stage stage, String displayName, String userId, ClientConnection client) {
        this.currentUserId = userId;
        controller = new PlayerListController(this, stage, client, userId);
        buildUI(displayName, userId);
    }

    private void buildUI(String displayName, String userId) {
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        Label title = new Label("📋 DANH SÁCH NGƯỜI CHƠI");
        title.setFont(Font.font("Arial", 20));
        title.setStyle("-fx-text-fill: #2E86DE; -fx-font-weight: bold;");

        Label currentPlayer = new Label("👤 Người chơi: " + displayName);
        currentPlayer.setStyle("-fx-font-size: 14; -fx-text-fill: #444;");

        TableColumn<Map<String, String>, String> nameCol = new TableColumn<>("Tên hiển thị");
        nameCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().get("display_name")));
        nameCol.setPrefWidth(250);

        TableColumn<Map<String, String>, String> statusCol = new TableColumn<>("Trạng thái");
        statusCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().get("status")));
        statusCol.setPrefWidth(150);

        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "Online" -> setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                        case "Đang trong trận" -> setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                        default -> setStyle("-fx-text-fill: gray;");
                    }
                }
            }
        });

        TableColumn<Map<String, String>, String> actionCol = new TableColumn<>("Hành động");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("⚔ Thách đấu");
            {
                btn.setOnAction(e -> {
                    Map<String, String> row = getTableView().getItems().get(getIndex());
                    if (row != null) {
                        String pid = row.get("player_id");
                        String name = row.get("display_name");
                        controller.challengePlayer(pid, name);
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Map<String, String> row = getTableView().getItems().get(getIndex());
                    boolean enabled = row != null && "IDLE".equals(row.get("status"));
                    btn.setDisable(!enabled);
                    setGraphic(btn);
                }
            }
        });
        actionCol.setPrefWidth(120);

        table.getColumns().addAll(nameCol, statusCol, actionCol);
        table.setPrefWidth(450);

        Button refreshBtn = new Button("🔄 Làm mới");
        refreshBtn.setOnAction(e -> controller.fetchPlayerList(userId));

        Button logoutBtn = new Button("🚪 Đăng xuất");
        logoutBtn.setOnAction(e -> controller.logout(userId));

        HBox actions = new HBox(10, refreshBtn, logoutBtn);
        actions.setAlignment(Pos.CENTER);

        message.setStyle("-fx-text-fill: gray;");

        root.getChildren().addAll(title, currentPlayer, table, actions, message);
        controller.fetchPlayerList(userId);
    }

    public VBox getRoot() { return root; }
    public void setPlayers(ObservableList<Map<String, String>> list) { table.setItems(list); }
    public void showMessage(String msg) { message.setText(msg); }
    public Map<String, String> getSelectedPlayer() { return table.getSelectionModel().getSelectedItem(); }
}
