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

    // tiny-escape
    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public void showTemporaryMessage(String msg, int ms) {
        JLabel tmp = new JLabel(msg, SwingConstants.CENTER);
        tmp.setOpaque(true);
        tmp.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        tmp.setBackground(new Color(255, 255, 225));
        add(tmp, BorderLayout.SOUTH);
        revalidate();
        repaint();
        new javax.swing.Timer(ms, e -> {
            remove(tmp);
            revalidate();
            repaint();
            ((javax.swing.Timer)e.getSource()).stop();
        }).start();
    }
}
