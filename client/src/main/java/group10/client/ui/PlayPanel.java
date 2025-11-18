package group10.client.ui;


import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class PlayPanel extends JPanel {
    // -- UI components (same idea as demo)
    private final DefaultListModel<String> sourceModel = new DefaultListModel<>();
    private final DefaultListModel<String> targetModel = new DefaultListModel<>();
    private final JList<String> sourceList = new JList<>(sourceModel);
    private final JList<String> targetList = new JList<>(targetModel);

    private final JLabel timerLabel = new JLabel("Timer: --:--", SwingConstants.CENTER);
    private final JLabel requirementLabel = new JLabel("Requirement: --", SwingConstants.CENTER);
    private final JLabel scoreboard = new JLabel("You: 0  |  Opponent: 0", SwingConstants.CENTER);
    private final JLabel status = new JLabel("Status: idle", SwingConstants.CENTER);

    private final JButton sendBtn = new JButton("Send");
    private final JButton exitBtn = new JButton("Exit");

    private javax.swing.Timer swingTimer;
    private Instant deadline;
    private final AtomicBoolean submitted = new AtomicBoolean(false);
    private String matchId = "demo-match-uuid";
    private String roundId;

    public PlayPanel(ScreenManager manager) {
        setLayout(new BorderLayout(8,8));
        setBorder(new EmptyBorder(10,10,10,10));
        initTop();
        initCenter();
        initBottom();
        setLocked(true);

        // Exit action returns to main menu (example)
        exitBtn.addActionListener(e -> {
            int r = JOptionPane.showConfirmDialog(this, "Exit match and return to menu?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.YES_OPTION) {
                // reset small state and go back
                if (swingTimer != null) swingTimer.stop();
                manager.show("main");
            }
        });

        sendBtn.addActionListener(e -> handleSend());
    }



    private void initTop() {
        JPanel top = new JPanel(new GridLayout(1,3,8,8));
        top.add(requirementLabel);
        top.add(timerLabel);
        top.add(scoreboard);
        add(top, BorderLayout.NORTH);
    }

    private void initCenter() {
        sourceList.setBorder(new TitledBorder("Source (drag items)"));
        targetList.setBorder(new TitledBorder("Target (drop here)"));

        sourceList.setDragEnabled(true);
        sourceList.setTransferHandler(new javax.swing.TransferHandler("selectedValue"));

        targetList.setDropMode(DropMode.INSERT);
        targetList.setTransferHandler(new javax.swing.TransferHandler("selectedValue"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(sourceList), new JScrollPane(targetList));
        split.setResizeWeight(0.5);
        add(split, BorderLayout.CENTER);
    }

    private void initBottom() {
        JPanel bottom = new JPanel(new BorderLayout(8,8));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        left.add(sendBtn);
        left.add(exitBtn);
        bottom.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new GridLayout(2,1));
        right.add(status);
        right.add(new JLabel("Round 0/10", SwingConstants.RIGHT));
        bottom.add(right, BorderLayout.EAST);

        add(bottom, BorderLayout.SOUTH);
    }

    // Called by MainMenu on Start (optional) to prepare UI - no server data yet.
    public void prepareForMatch() {
        submitted.set(false);
        setLocked(true);
        requirementLabel.setText("Requirement: waiting for round...");
        timerLabel.setText("Timer: --:--");
        sourceModel.clear();
        targetModel.clear();
        scoreboard.setText("You: 0  |  Opponent: 0");
        status.setText("Status: waiting");
    }

    // Called by network layer when START_ROUND arrives
    public void startRound(List<String> payload, String orderType, Instant serverDeadline) {
        SwingUtilities.invokeLater(() -> {
            submitted.set(false);
            sendBtn.setEnabled(true);
            sourceModel.clear();
            targetModel.clear();

            // shuffle for UI display
            List<String> shuffled = new ArrayList<>(payload);
            Collections.shuffle(shuffled);
            shuffled.forEach(sourceModel::addElement);

            requirementLabel.setText("Requirement: Sort " + orderType);
            roundId = UUID.randomUUID().toString();
            deadline = serverDeadline;

            if (swingTimer != null) swingTimer.stop();
            swingTimer = new javax.swing.Timer(100, ev -> updateTimerTick());
            swingTimer.start();

            setLocked(false);
            status.setText("Status: playing");
        });
    }

    private void updateTimerTick() {
        long remaining = computeRemainingMillis();
        if (remaining <= 0) {
            timerLabel.setText("Time: 00.0");
            if (swingTimer != null) swingTimer.stop();
            onTimeout();
            return;
        }
        long secs = remaining / 1000;
        long ms = (remaining % 1000) / 100;
        timerLabel.setText(String.format("Time: %02d.%d", secs, ms));
    }

    private long computeRemainingMillis() {
        if (deadline == null) return 0;
        return java.time.Duration.between(java.time.Instant.now(), deadline).toMillis();
    }

    private void handleSend() {
        if (submitted.getAndSet(true)) {
            sendBtn.setEnabled(false);
            return;
        }
        if (swingTimer != null) swingTimer.stop();
        setLocked(true);
        // build submission payload (UI only); real send via client.net
        List<String> submission = Collections.list(targetModel.elements());
        // Expose a callback or event bus later. For now we just show a confirm:
        status.setText("Status: submitted (local)");
        sendBtn.setEnabled(false);
        // Real project: call client.net.submit(matchId, roundId, submission, timeMs)
    }

    private void onTimeout() {
        if (!submitted.get()) {
            submitted.set(true);
            setLocked(true);
            sendBtn.setEnabled(false);
            status.setText("Status: timed out");
            // Notify network layer if necessary
        }
    }

    private void setLocked(boolean locked) {
        sourceList.setEnabled(!locked);
        targetList.setEnabled(!locked);
        sendBtn.setEnabled(!locked && !submitted.get());
    }
}
