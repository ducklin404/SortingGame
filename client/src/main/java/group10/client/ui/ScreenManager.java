package group10.client.ui;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;


public class ScreenManager {
    private final JFrame frame;
    private final Map<String, JPanel> screens = new HashMap<>();

    public ScreenManager(JFrame frame) {
        this.frame = frame;
        frame.getContentPane().setLayout(new BorderLayout());

    }

    public void registerScreen(String key, JPanel panel) {
        screens.put(key, panel);
    }

    public JPanel getScreen(String key) {
        return screens.get(key);
    }

    public void show(String key) {
        JPanel panel = screens.get(key);
        if (panel == null) throw new IllegalArgumentException("No screen: " + key);
        frame.getContentPane().removeAll();
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.revalidate();
        frame.repaint();
        // move focus into the newly shown panel
        panel.requestFocusInWindow();
    }
}
