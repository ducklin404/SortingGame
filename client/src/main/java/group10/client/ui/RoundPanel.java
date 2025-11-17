package group10.client.ui;

import group10.client.net.ClientConnection;
import group10.common.util.JsonUtil;
import group10.common.protocol.ProtocolConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

public class RoundPanel extends JPanel {
    // header
    private final JLabel playersLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel roundLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel timerLabel = new JLabel("", SwingConstants.CENTER);

    // ordering UI
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> itemsList = new JList<>(listModel);
    private final JButton upButton = new JButton("Up");
    private final JButton downButton = new JButton("Down");
    private final JButton submitButton = new JButton("Submit");

    // waiting message after submit
    private final JLabel waitingLabel = new JLabel("", SwingConstants.CENTER);

    private final ClientConnection clientConnection;

    // tracking
    private String matchId;
    private String roundId;
    private Timer countdownTimer; // Swing timer
    private long deadlineTs;

    public RoundPanel(ClientConnection clientConnection) {
        this.clientConnection = clientConnection;
        setLayout(new BorderLayout(8, 8));
        JPanel header = new JPanel(new GridLayout(1, 3));
        header.add(playersLabel);
        header.add(roundLabel);
        header.add(timerLabel);
        add(header, BorderLayout.NORTH);

        // center: list + up/down buttons
        itemsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(itemsList);

        JPanel controls = new JPanel(new BorderLayout());
        JPanel arrows = new JPanel(new GridLayout(2, 1, 4, 4));
        arrows.add(upButton);
        arrows.add(downButton);
        controls.add(arrows, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(6,6));
        center.add(scroll, BorderLayout.CENTER);
        center.add(controls, BorderLayout.EAST);

        add(center, BorderLayout.CENTER);

        // bottom: submit & waiting
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(submitButton, BorderLayout.WEST);
        bottom.add(waitingLabel, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        // buttons
        upButton.addActionListener(this::onUp);
        downButton.addActionListener(this::onDown);
        submitButton.addActionListener(this::onSubmit);

        // initial state
        setIdleState();
    }

    private void setIdleState() {
        playersLabel.setText("");
        roundLabel.setText("");
        timerLabel.setText("");
        waitingLabel.setText("");
        listModel.clear();
        enableEditing(false);
    }

    private void enableEditing(boolean enabled) {
        itemsList.setEnabled(enabled);
        upButton.setEnabled(enabled);
        downButton.setEnabled(enabled);
        submitButton.setEnabled(enabled);
    }

    private void onUp(ActionEvent e) {
        int idx = itemsList.getSelectedIndex();
        if (idx > 0) {
            String v = listModel.get(idx);
            listModel.remove(idx);
            listModel.add(idx - 1, v);
            itemsList.setSelectedIndex(idx - 1);
        }
    }

    private void onDown(ActionEvent e) {
        int idx = itemsList.getSelectedIndex();
        if (idx >= 0 && idx < listModel.getSize() - 1) {
            String v = listModel.get(idx);
            listModel.remove(idx);
            listModel.add(idx + 1, v);
            itemsList.setSelectedIndex(idx + 1);
        }
    }

    private void onSubmit(ActionEvent e) {
        // build order string (concat items). Adjust if your server expects a different format.
        String order = "";
        for (int i = 0; i < listModel.size(); i++) {
            order += listModel.get(i);
            if (i < listModel.size() - 1) order += ""; // no separator or add if protocol expects
        }

        // disable editing & show waiting
        enableEditing(false);
        waitingLabel.setText("Submitted — waiting for opponent...");

        // send payload
        ObjectNode payload = JsonUtil.MAPPER.createObjectNode();
        payload.put("matchId", matchId);
        payload.put("roundId", roundId);
        payload.put("order", order);
        payload.put("timestamp", System.currentTimeMillis());

        try {
            clientConnection.send(ProtocolConstants.SUBMIT, payload);
        } catch (IOException ex) {
            ex.printStackTrace();
            // restore UI so player can retry if needed
            waitingLabel.setText("Failed to submit. Try again.");
            enableEditing(true);
        }
    }

    /**
     * Populate UI for a new incoming START_ROUND. Call on EDT.
     * @param payload the START_ROUND payload as JsonNode
     */
    public void startRound(JsonNode payload) {
        // cancel any prior timer
        if (countdownTimer != null) {
            countdownTimer.stop();
            countdownTimer = null;
        }

        // parse fields
        this.matchId = payload.path("matchId").asText(null);
        this.roundId = payload.path("roundId").asText(null);
        int roundNo = payload.path("round").asInt(-1);
        long serverDeadline = payload.path("deadlineTs").asLong(0);
        this.deadlineTs = serverDeadline;

        // header: players & scores
        int aPts = payload.path("playerAPoint").asInt(0);
        int bPts = payload.path("PlayerBPoint").asInt(0);
        // you may prefer to include player names in payload; if so, read them and show
        // for now show points only
        playersLabel.setText(String.format("Player A: %d    —    Player B: %d", aPts, bPts));
        roundLabel.setText("Round " + roundNo);

        // items array
        listModel.clear();
        JsonNode itemsNode = payload.path("items");
        if (itemsNode.isArray()) {
            for (JsonNode it : itemsNode) {
                listModel.addElement(it.asText());
            }
        }

        // enable editing
        waitingLabel.setText("");
        enableEditing(true);

        // start countdown (update every second)
        countdownTimer = new Timer(250, evt -> updateTimer());
        countdownTimer.setRepeats(true);
        countdownTimer.start();
        updateTimer();
    }

    private void updateTimer() {
        long now = System.currentTimeMillis();
        long remaining = deadlineTs - now;
        if (remaining <= 0) {
            timerLabel.setText("Time: 0s");
            if (countdownTimer != null) {
                countdownTimer.stop();
                countdownTimer = null;
            }
            // client does not auto-submit here; server will treat missing submit as timeout.
            enableEditing(false);
            waitingLabel.setText("Time up — waiting for results...");
        } else {
            timerLabel.setText(String.format("Time: %d s", (remaining + 500) / 1000));
        }
    }

    /**
     * Called when round result / cancel arrives. Run on EDT.
     */
    public void onRoundFinished(JsonNode resultPayload) {
        // Example: update header scores from resultPayload
        int aPts = resultPayload.path("playerAPoint").asInt(0);
        int bPts = resultPayload.path("PlayerBPoint").asInt(0);
        playersLabel.setText(String.format("Player A: %d    —    Player B: %d", aPts, bPts));

        // show a short message (modal or toast). We'll use JOptionPane here:
        String msg = resultPayload.path("message").asText("Round finished");
        JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                msg, "Round finished", JOptionPane.INFORMATION_MESSAGE);

        // stop timer if still running
        if (countdownTimer != null) {
            countdownTimer.stop();
            countdownTimer = null;
        }

        // clear UI or leave final arrangement visible, then later server will issue next START_ROUND
        enableEditing(false);
        waitingLabel.setText("Round finished.");
    }
}
