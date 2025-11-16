package group10.client.ui;

import javax.swing.*;
import java.awt.*;


public class MainMenuPanel extends JPanel {
    private final ScreenManager manager;

    public MainMenuPanel(ScreenManager manager) {
        this.manager = manager;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10,10));
        JPanel center = new JPanel(new GridBagLayout());
        add(center, BorderLayout.CENTER);

        JLabel title = new JLabel("Sorting Match", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(28f));
        add(title, BorderLayout.NORTH);

        // Big start button
        JButton startBtn = new JButton("Start Match");
        startBtn.setPreferredSize(new Dimension(220, 48));
        startBtn.addActionListener(e -> {
            // Navigate to play screen
            manager.show("play");
            // Optionally initialize the play screen (or the network layer will call startRound)
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
