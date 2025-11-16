package group10.client;

import group10.client.ui.ScreenManager;
import group10.client.ui.MainMenuPanel;
import group10.client.ui.PlayPanel;

import javax.swing.*;

public class ClientMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // top-level frame and the screen manager
            JFrame frame = new JFrame("Sorting Match - Client");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 620);
            frame.setLocationRelativeTo(null);

            ScreenManager manager = new ScreenManager(frame);

            // Create UI screens
            MainMenuPanel mainMenu = new MainMenuPanel(manager);
            PlayPanel playPanel = new PlayPanel(manager);

            // Register screens
            manager.registerScreen("main", mainMenu);
            manager.registerScreen("play", playPanel);

            // Show main menu
            manager.show("main");

            frame.setVisible(true);
        });
    }
}
