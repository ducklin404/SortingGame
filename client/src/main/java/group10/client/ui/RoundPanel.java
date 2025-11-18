// (Full simplified RoundPanel as before, only changed updateCountdown to show two-decimal numbers.)
package group10.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import group10.client.net.ClientConnection;
import group10.common.util.JsonUtil;
import group10.common.protocol.ProtocolConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RoundPanel extends JPanel {
    private final ClientConnection connection;

    private final JLabel roundLabel = new JLabel("Round: -");
    private final JLabel orderLabel = new JLabel("Order: -");

    private final JProgressBar deadlineBar = new JProgressBar();
    private final JLabel deadlineSecondsLabel = new JLabel("");

    private final DefaultListModel<String> itemsModel = new DefaultListModel<>();
    private final JList<String> itemsList = new JList<>(itemsModel);

    private final JButton submitButton = new JButton("Submit");
    private final JLabel yourSubmissionLabel = new JLabel("Your submission: -");

    private final JLabel scoreLabel = new JLabel("Score -");

    private volatile long deadlineTs = -1;
    private volatile long deadlineTotalMs = -1;
    private javax.swing.Timer uiTimer;
    private String currentMatchId = null;
    private String currentRoundId = null;

    private String playerADisplayName = "A";
    private String playerBDisplayName = "B";

    public RoundPanel(ClientConnection connection) {
        this.connection = connection;
        setLayout(new BorderLayout(10,10));
        setBorder(new EmptyBorder(12,12,12,12));

        JPanel top = new JPanel(new GridLayout(1,3,8,8));
        roundLabel.setHorizontalAlignment(SwingConstants.LEFT);
        orderLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel deadlinePanel = new JPanel(new BorderLayout(4,4));
        deadlineBar.setStringPainted(true);
        deadlineBar.setMinimum(0);
        deadlineBar.setMaximum(1000);
        deadlineBar.setValue(0);
        deadlinePanel.add(deadlineBar, BorderLayout.CENTER);
        deadlineSecondsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        deadlinePanel.add(deadlineSecondsLabel, BorderLayout.SOUTH);

        top.add(roundLabel);
        top.add(orderLabel);
        top.add(deadlinePanel);
        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1,2,12,12));

        JPanel itemsPanel = new JPanel(new BorderLayout(6,6));
        itemsPanel.setBorder(BorderFactory.createTitledBorder("Items (drag to reorder)"));
        itemsList.setVisibleRowCount(10);
        itemsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemsList.setDragEnabled(true);
        itemsList.setDropMode(DropMode.INSERT);
        itemsList.setTransferHandler(new ReorderListTransferHandler());
        itemsPanel.add(new JScrollPane(itemsList), BorderLayout.CENTER);

        center.add(itemsPanel);

        JPanel control = new JPanel();
        control.setLayout(new BoxLayout(control, BoxLayout.Y_AXIS));
        control.setBorder(BorderFactory.createTitledBorder("Submission"));

        JLabel hint = new JLabel("Drag items into desired order, then press Submit.");
        hint.setAlignmentX(LEFT_ALIGNMENT);
        control.add(hint);
        control.add(Box.createRigidArea(new Dimension(0,8)));

        submitButton.setAlignmentX(LEFT_ALIGNMENT);
        control.add(submitButton);
        control.add(Box.createRigidArea(new Dimension(0,12)));

        control.add(yourSubmissionLabel);
        control.add(Box.createRigidArea(new Dimension(0,6)));

        center.add(control);
        add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8,8));
        bottom.setBorder(BorderFactory.createTitledBorder("Score"));
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD, 14f));
        bottom.add(scoreLabel, BorderLayout.NORTH);
        add(bottom, BorderLayout.SOUTH);

        submitButton.addActionListener(e -> doSubmit());

        itemsList.addMouseListener(new MouseInputAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int idx = itemsList.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        itemsList.setSelectedIndex(idx);
                        TransferHandler th = itemsList.getTransferHandler();
                        th.exportAsDrag(itemsList, e, TransferHandler.MOVE);
                    }
                }
            }
        });

        uiTimer = new javax.swing.Timer(250, e -> updateCountdown());
        uiTimer.setRepeats(true);
    }

    public void startRound(JsonNode payload) {
        if (payload == null) return;
        currentMatchId = payload.path("matchId").asText(null);
        currentRoundId = payload.path("roundId").asText(null);
        int r = payload.path("round").asInt(-1);
        roundLabel.setText("Round: " + (r >= 0 ? r : "n/a"));

        String orderType = payload.has("order") ? payload.path("order").asText("") : "";
        orderLabel.setText("Order: " + (orderType.isEmpty() ? "(none)" : orderType));

        itemsModel.clear();
        if (payload.has("items") && payload.get("items").isArray()) {
            ArrayNode arr = (ArrayNode) payload.get("items");
            for (JsonNode n : arr) itemsModel.addElement(n.asText());
        }

        if (payload.has("playerA")) playerADisplayName = payload.path("playerA").asText(playerADisplayName);
        if (payload.has("playerB")) playerBDisplayName = payload.path("playerB").asText(playerBDisplayName);
        int aPoints = payload.path("playerAPoint").asInt(0);
        int bPoints = payload.path("playerBPoint").asInt(0);
        scoreLabel.setText(String.format("%s: %d    %s: %d", playerADisplayName, aPoints, playerBDisplayName, bPoints));
        yourSubmissionLabel.setText("Your submission: ");
        if (payload.has("deadlineTs") && payload.get("deadlineTs").canConvertToLong()) {
            deadlineTs = payload.get("deadlineTs").asLong();
            long now = System.currentTimeMillis();
            deadlineTotalMs = Math.max(1, deadlineTs - now);
            deadlineBar.setValue(deadlineBar.getMaximum());
            updateCountdown();
            uiTimer.start();
        } else {
            deadlineTs = -1;
            deadlineTotalMs = -1;
            deadlineBar.setValue(0);
            deadlineBar.setString("");
            deadlineSecondsLabel.setText("");
            uiTimer.stop();
        }

        boolean roundActive = deadlineTs <= 0 || System.currentTimeMillis() < deadlineTs;
        setControlsEnabled(roundActive && !hasPlayerSubmitted(payload));
    }

    public void onRoundFinished(JsonNode payload) {
        if (payload == null) return;
        if (payload.has("playerA")) playerADisplayName = payload.path("playerA").asText(playerADisplayName);
        if (payload.has("playerB")) playerBDisplayName = payload.path("playerB").asText(playerBDisplayName);
        int aPoints = payload.path("playerAPoint").asInt(0);
        int bPoints = payload.path("playerBPoint").asInt(0);
        scoreLabel.setText(String.format("%s: %d    %s: %d", playerADisplayName, aPoints, playerBDisplayName, bPoints));
        setControlsEnabled(false);
        uiTimer.stop();
        deadlineBar.setValue(0);
        deadlineBar.setString("Done");
        deadlineSecondsLabel.setText("");
    }

    private boolean hasPlayerSubmitted(JsonNode payload) {
        JsonNode aSub = payload.path("playerASubmission");
        if (aSub.isArray()) return aSub.size() > 0;
        if (aSub.isTextual()) return !aSub.asText().trim().isEmpty();
        return !aSub.isMissingNode() && !aSub.isNull();
    }

    private String jsonNodeToString(JsonNode n, boolean joinArrays) {
        if (n == null || n.isMissingNode() || n.isNull()) return "-";
        if (n.isTextual()) return n.asText();
        if (n.isArray() && joinArrays) {
            List<String> vals = new ArrayList<>();
            for (JsonNode x : n) vals.add(x.asText());
            return String.join(" , ", vals);
        }
        return n.toString();
    }

    private void setControlsEnabled(boolean enabled) {
        submitButton.setEnabled(enabled);
        if (!enabled) submitButton.setText("Submitted");
        else submitButton.setText("Submit");
        itemsList.setEnabled(enabled);
    }

    /**
     * Updated: display two-decimal values for remaining and total seconds.
     * Example: "12.12 12.11"
     */
    private void updateCountdown() {
        if (deadlineTs <= 0 || deadlineTotalMs <= 0) {
            deadlineBar.setValue(0);
            deadlineBar.setString("");
            deadlineSecondsLabel.setText("");
            return;
        }
        long now = System.currentTimeMillis();
        long remainingMs = Math.max(0, deadlineTs - now);

        double remainingSec = remainingMs / 1000.0;
        double totalSec = Math.max(0.0, deadlineTotalMs / 1000.0);

        // If time expired
        if (remainingMs <= 0) {
            String remStr = String.format("%.2f", 0.0);
            String totStr = String.format("%.2f", totalSec);
            deadlineBar.setValue(0);
            deadlineBar.setString(remStr);               // shows "0.00" on the bar
            deadlineSecondsLabel.setText(remStr + " " + totStr);
            setControlsEnabled(false);
            uiTimer.stop();
            return;
        }

        // percent remaining (0..1)
        double frac = remainingMs / (double) deadlineTotalMs;
        int val = (int) Math.round(frac * deadlineBar.getMaximum());
        deadlineBar.setValue(val);

        String remStr = String.format("%.2f", remainingSec);
        String totStr = String.format("%.2f", totalSec);

        // bar shows remaining (two decimals); label shows "remaining total"
        deadlineBar.setString(remStr);
        deadlineSecondsLabel.setText(remStr + " " + totStr);
    }

    private void doSubmit() {
        if (currentMatchId == null || currentRoundId == null) {
            JOptionPane.showMessageDialog(this, "No active round to submit to", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        ArrayNode outArr = JsonUtil.MAPPER.createArrayNode();
        for (int i = 0; i < itemsModel.size(); i++) outArr.add(itemsModel.get(i));

        ObjectNode out = JsonUtil.MAPPER.createObjectNode();
        out.put("matchId", currentMatchId);
        out.put("roundId", currentRoundId);
        out.set("submission", outArr);

        try {
            connection.send(ProtocolConstants.SUBMIT, out);
            yourSubmissionLabel.setText("Your submission: " + jsonNodeToString(outArr, true));
            setControlsEnabled(false);
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to send submission: " + ex.getMessage(), "Network error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class ReorderListTransferHandler extends TransferHandler {
        private final DataFlavor localObjectFlavor = new DataFlavor(String.class, "String");
        private int sourceIndex = -1;

        public int getSourceActions(JComponent c) { return MOVE; }

        protected Transferable createTransferable(JComponent c) {
            sourceIndex = itemsList.getSelectedIndex();
            final String val = itemsList.getSelectedValue();
            return new Transferable() {
                public Object getTransferData(DataFlavor flavor) { return val; }
                public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[] { localObjectFlavor }; }
                public boolean isDataFlavorSupported(DataFlavor flavor) { return localObjectFlavor.equals(flavor); }
            };
        }

        public boolean canImport(TransferHandler.TransferSupport info) {
            return info.isDrop() && info.isDataFlavorSupported(localObjectFlavor);
        }

        public boolean importData(TransferHandler.TransferSupport info) {
            if (!canImport(info)) return false;
            JList.DropLocation dl = (JList.DropLocation) info.getDropLocation();
            int index = dl.getIndex();
            try {
                String data = (String) info.getTransferable().getTransferData(localObjectFlavor);
                if (sourceIndex < 0) return false;
                if (index > sourceIndex) index--;
                itemsModel.remove(sourceIndex);
                itemsModel.add(index, data);
                itemsList.setSelectedIndex(index);
                return true;
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
        }

        protected void exportDone(JComponent c, Transferable t, int action) { sourceIndex = -1; }
    }
}
