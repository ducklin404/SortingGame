package group10.client.ui;

import com.fasterxml.jackson.databind.node.ObjectNode;
import group10.client.net.ClientConnection;
import group10.common.util.JsonUtil;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

import static group10.common.protocol.ProtocolConstants.*;

public class MainMenuPanel extends JPanel {
    private final ScreenManager manager;
    private final ClientConnection clientConnection;

    public MainMenuPanel(ScreenManager manager, ClientConnection clientConnection) {
        this.manager = manager;
        this.clientConnection = clientConnection;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        JPanel center = new JPanel(new GridBagLayout());
        add(center, BorderLayout.CENTER);

        JLabel title = new JLabel("Sorting Match", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(28f));
        add(title, BorderLayout.NORTH);

        // ========= BUTTON: START ===========
        JButton startBtn = new JButton("Start Match");
        startBtn.setPreferredSize(new Dimension(220, 48));
        startBtn.addActionListener(e -> {
            manager.show("play");
            ObjectNode payload = JsonUtil.MAPPER.createObjectNode();
            payload.put("inviteId", "86f18844-88b6-46d1-b132-513c31527fd1");
            payload.put("response", "OK");

            try {
                clientConnection.send(INVITE_RESPONSE, payload);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            PlayPanel playPanel = (PlayPanel) manager.getScreen("play");
            if (playPanel != null) {
                playPanel.prepareForMatch();
            }
        });

        // ========= BUTTON: LEADERBOARD ===========
        JButton leaderboardBtn = new JButton("Leaderboard");
        leaderboardBtn.setPreferredSize(new Dimension(220, 48));
        leaderboardBtn.addActionListener(e -> {
            try {
                ObjectNode payload = JsonUtil.MAPPER.createObjectNode();
                clientConnection.send(GET_LEADERBOARD, payload);
                System.out.println("Đã gửi GET_LEADERBOARD");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        // ========= BUTTON: MATCH HISTORY ===========
        JButton historyBtn = new JButton("Match History");
        historyBtn.setPreferredSize(new Dimension(220, 48));

        historyBtn.addActionListener(e -> {
            // Hiển thị hộp thoại nhập username (popup)
            String username = JOptionPane.showInputDialog(
                    this,
                    "Nhập username để xem lịch sử đấu:",
                    "Match History",
                    JOptionPane.PLAIN_MESSAGE
            );

            if (username == null) {
                // User bấm Cancel
                return;
            }

            username = username.trim();

            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Bạn phải nhập username hợp lệ!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                ObjectNode p = JsonUtil.MAPPER.createObjectNode();
                p.put("username", username);

                clientConnection.send(GET_MATCH_HISTORY, p);
                System.out.println("Request MATCH_HISTORY sent for: " + username);

                // Chuyển sang màn hình history
                manager.show("history");

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // ========= LAYOUT ===========
        JPanel btns = new JPanel();
        btns.setLayout(new BoxLayout(btns, BoxLayout.Y_AXIS));

        btns.add(startBtn);
        btns.add(Box.createRigidArea(new Dimension(0, 12)));

        btns.add(leaderboardBtn);
        btns.add(Box.createRigidArea(new Dimension(0, 12)));

        btns.add(historyBtn);

        center.add(btns);

        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    }
}
