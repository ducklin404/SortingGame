package ui;

import group10.persistence.LeaderboardDAO;
import group10.persistence.PlayerStat;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class LeaderboardApp extends Application {

    private static final int ROWS_PER_PAGE = 10;
    private ObservableList<PlayerStat> data;
    private TableView<PlayerStat> table;
    private Pagination pagination;

    @Override
    public void start(Stage stage) {
        stage.setTitle("🏆 Bảng Xếp Hạng - Sorting Game");

        LeaderboardDAO dao = new LeaderboardDAO();
        data = FXCollections.observableArrayList(dao.getLeaderboard());

        table = new TableView<>();
        table.getStyleClass().add("leaderboard-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); // 👈 Thêm dòng này


        // ===== CỘT HẠNG =====
        TableColumn<PlayerStat, Number> rankCol = new TableColumn<>("Hạng");
        rankCol.setCellValueFactory(cellData ->
                javafx.beans.binding.Bindings.createIntegerBinding(() ->
                        data.indexOf(cellData.getValue()) + 1
                )
        );
        rankCol.setPrefWidth(70);
        rankCol.setSortable(false);

        TableColumn<PlayerStat, String> usernameCol = new TableColumn<>("Người chơi");
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        usernameCol.setPrefWidth(150);

        TableColumn<PlayerStat, Double> pointsCol = new TableColumn<>("Tổng điểm");
        pointsCol.setCellValueFactory(new PropertyValueFactory<>("totalPoints"));
        pointsCol.setPrefWidth(120);

        TableColumn<PlayerStat, Integer> winsCol = new TableColumn<>("Thắng");
        winsCol.setCellValueFactory(new PropertyValueFactory<>("wins"));
        winsCol.setPrefWidth(90);

        TableColumn<PlayerStat, Integer> lossesCol = new TableColumn<>("Thua");
        lossesCol.setCellValueFactory(new PropertyValueFactory<>("losses"));
        lossesCol.setPrefWidth(90);

        TableColumn<PlayerStat, Integer> drawsCol = new TableColumn<>("Hòa");
        drawsCol.setCellValueFactory(new PropertyValueFactory<>("draws"));
        drawsCol.setPrefWidth(90);

        table.getColumns().addAll(rankCol, usernameCol, pointsCol, winsCol, lossesCol, drawsCol);

        pagination = new Pagination((int) Math.ceil((double) data.size() / ROWS_PER_PAGE), 0);
        pagination.setPageFactory(this::createPage);
        pagination.getStyleClass().add("custom-pagination");

        Button updateButton = new Button("🔄 Cập nhật");
        updateButton.getStyleClass().add("update-button");
        updateButton.setOnAction(e -> refreshData(dao));

        Label titleLabel = new Label("🏆 BẢNG XẾP HẠNG NGƯỜI CHƠI 🏆");
        titleLabel.getStyleClass().add("title-label");

        VBox topBox = new VBox(10, titleLabel);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(10, 0, 5, 0));

        HBox controls = new HBox(updateButton);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(15, 0, 15, 0));

        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(pagination);
        root.setBottom(controls);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 750, 450);
        scene.getStylesheets().add(getClass().getResource("/ui/leaderboard.css").toExternalForm());

        stage.setScene(scene);
        stage.show();
    }

    private BorderPane createPage(int pageIndex) {
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, data.size());
        table.setItems(FXCollections.observableArrayList(data.subList(fromIndex, toIndex)));
        return new BorderPane(table);
    }

    private void refreshData(LeaderboardDAO dao) {
        List<PlayerStat> newData = dao.getLeaderboard();
        data.setAll(newData);
        pagination.setPageCount((int) Math.ceil((double) data.size() / ROWS_PER_PAGE));
        pagination.setCurrentPageIndex(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
