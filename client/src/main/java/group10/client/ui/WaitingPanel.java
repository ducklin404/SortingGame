package group10.client.ui;

import javax.swing.*;
import java.awt.*;

public class WaitingPanel extends JPanel {
    private final JLabel label;

    public WaitingPanel() {
        setLayout(new BorderLayout());
        label = new JLabel("Waiting for match to start...", SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(18f));
        add(label, BorderLayout.CENTER);
        setFocusable(true);
    }

    public void setOpponentName(String name) {
        if (name == null || name.isEmpty()) name = "opponent";
        label.setText("<html>Matched with:<br/><b>" + escapeHtml(name) + "</b><br/>Waiting for match to start...</html>");
    }

    // tiny-escape; not a full HTML sanitizer but enough for simple names
    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
