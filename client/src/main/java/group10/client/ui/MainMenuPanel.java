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
        setLayout(new BorderLayout(10,10));
        JPanel center = new JPanel(new GridBagLayout());
        add(center, BorderLayout.CENTER);

        JLabel title = new JLabel("Sorting Match", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(28f));
        add(title, BorderLayout.NORTH);

        // Start button
        JButton startBtn = new JButton("Start Match");
        startBtn.setPreferredSize(new Dimension(220, 48));
        startBtn.addActionListener(e -> {
            // Navigate to play screen
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
                // lightweight demo initialization; real app should wait for START_ROUND from server
                playPanel.prepareForMatch(); // prepares UI, keeps logic separate
            }
        });


        JPanel btns = new JPanel();
        btns.setLayout(new BoxLayout(btns, BoxLayout.Y_AXIS));
        btns.add(startBtn);
        btns.add(Box.createRigidArea(new Dimension(0, 12)));

        center.add(btns);
        setBorder(BorderFactory.createEmptyBorder(24,24,24,24));
    }
}
