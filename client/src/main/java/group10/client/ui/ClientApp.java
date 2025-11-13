package group10.client.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApp extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("Sorting Game - Login");
        LoginScreen login = new LoginScreen(stage);
        stage.setScene(new Scene(login.getRoot(), 500, 400));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}