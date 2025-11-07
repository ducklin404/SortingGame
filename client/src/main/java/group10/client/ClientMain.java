package group10.client;

import group10.client.view.LoginView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientMain extends Application {
    @Override
    public void start(Stage stage) {
        LoginView loginView = new LoginView(stage);
        stage.setTitle("Sort Game - Login");
        stage.setScene(new Scene(loginView.getRoot(), 500, 400));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
