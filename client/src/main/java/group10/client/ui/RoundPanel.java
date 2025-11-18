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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * RoundPanel with drag-and-drop reordering for arbitrary item strings.
 * - Items are displayed in a JList that supports dragging to reorder.
 * - Submission sends an array "submission" matching the list order.
 * - Shows order type (ASC/DESC if provided) and countdown.
 * - Accepts arbitrary item contents (not just single characters).
 */
public class RoundPanel extends JPanel {
    private final ClientConnection connection;

    // header
    private final JLabel roundLabel = new JLabel("Round: -");
    private final JLabel orderLabel = new JLabel("Order: -");
    private final JLabel deadlineLabel = new JLabel("Deadline: -");

    // items list (drag-to-reorder)
    private final DefaultListModel<String> itemsModel = new DefaultListModel<>();
    private final JList<String> itemsList = new JList<>(itemsModel);

    // submission controls
    private final JButton submitButton = new JButton("Submit");
    private final JLabel yourSubmissionLabel = new JLabel("Your submission: -");

    // bottom: score & message
    private final JLabel scoreLabel = new JLabel("Score A 0 - 0 B");
    private final JTextArea messageArea = new JTextArea();

    private volatile long deadlineTs = -1;
    private javax.swing.Timer uiTimer;
    private String currentMatchId = null;
    private String currentRoundId = null;

    public RoundPanel(ClientConnection connection) {
        this.connection = connection;
        setLayout(new BorderLayout(10,10));
        setBorder(new EmptyBorder(12,12,12,12));

        // top
        JPanel top = new JPanel(new GridLayout(1,3,8,8));
        roundLabel.setHorizontalAlignment(SwingConstants.LEFT);
        orderLabel.setHorizontalAlignment(SwingConstants.CENTER);
        deadlineLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        top.add(roundLabel);
        top.add(orderLabel);
        top.add(deadlineLabel);
        add(top, BorderLayout.NORTH);

        // center
        JPanel center = new JPanel(new GridLayout(1,2,12,12));

        // items panel
        JPanel itemsPanel = new JPanel(new BorderLayout(6,6));
        itemsPanel.setBorder(BorderFactory.createTitledBorder("Items (drag to reorder)"));
        itemsList.setVisibleRowCount(10);
        itemsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemsList.setDragEnabled(true);
        itemsList.setDropMode(DropMode.INSERT);
        itemsList.setTransferHandler(new ReorderListTransferHandler());
        JScrollPane scroll = new JScrollPane(itemsList);
        itemsPanel.add(scroll, BorderLayout.CENTER);


        center.add(itemsPanel);

        // controls
        JPanel control = new JPanel();
        control.setLayout(new BoxLayout(control, BoxLayout.Y_AXIS));
        control.setBorder(BorderFactory.createTitledBorder("Submission"));

        JLabel hint = new JLabel("Drag items to the order you want, then press Submit.");
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

        // bottom
        JPanel bottom = new JPanel(new BorderLayout(8,8));
        bottom.setBorder(BorderFactory.createTitledBorder("Score / messages"));
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD, 14f));
        bottom.add(scoreLabel, BorderLayout.NORTH);

        messageArea.setEditable(false);
        messageArea.setRows(4);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        bottom.add(new JScrollPane(messageArea), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        // actions
        submitButton.addActionListener(e -> doSubmit());

        // enable drop by double-clicking an item to start drag (UX helpful on some systems)
        itemsList.addMouseListener(new MouseInputAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int idx = itemsList.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        itemsList.setSelectedIndex(idx);
                        // start drag programmatically
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

        // populate items (accept arbitrary strings)
        itemsModel.clear();
        if (payload.has("items") && payload.get("items").isArray()) {
            ArrayNode arr = (ArrayNode) payload.get("items");
            for (JsonNode n : arr) itemsModel.addElement(n.asText());
        }

        // submissions
        int aPoints = payload.path("playerAPoint").asInt(0);
        int bPoints = payload.path("playerBPoint").asInt(0);
        scoreLabel.setText(String.format("Score A: %d    B: %d", aPoints, bPoints));

        messageArea.setText(payload.path("message").asText(""));

        if (payload.has("deadlineTs") && payload.get("deadlineTs").canConvertToLong()) {
            deadlineTs = payload.get("deadlineTs").asLong();
            updateCountdown();
            uiTimer.start();
        } else {
            deadlineTs = -1;
            deadlineLabel.setText("Deadline: -");
            uiTimer.stop();
        }

        boolean roundActive = deadlineTs <= 0 || Instant.now().toEpochMilli() < deadlineTs;
        setControlsEnabled(roundActive && !hasPlayerSubmitted(payload));
    }

    public void onRoundFinished(JsonNode payload) {
        if (payload == null) return;
        messageArea.setText(payload.path("message").asText("Round finished"));

        int aPoints = payload.path("playerAPoint").asInt(0);
        int bPoints = payload.path("playerBPoint").asInt(0);
        scoreLabel.setText(String.format("Score A: %d    B: %d", aPoints, bPoints));
        setControlsEnabled(false);
        uiTimer.stop();
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
        if (!enabled) submitButton.setText("Submit (disabled)");
        else submitButton.setText("Submit");
        itemsList.setEnabled(enabled);
    }

    private void updateCountdown() {
        if (deadlineTs <= 0) {
            deadlineLabel.setText("Deadline: -");
            return;
        }
        long now = Instant.now().toEpochMilli();
        long remaining = deadlineTs - now;
        if (remaining <= 0) {
            deadlineLabel.setText("Deadline: expired");
            setControlsEnabled(false);
            uiTimer.stop();
            return;
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
        String abs = fmt.format(Instant.ofEpochMilli(deadlineTs));
        String rel = String.format("%d s", (remaining + 500)/1000);
        deadlineLabel.setText("Deadline: " + abs + " (in " + rel + ")");
    }

    private void doSubmit() {
        if (currentMatchId == null || currentRoundId == null) {
            JOptionPane.showMessageDialog(this, "No active round to submit to", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // build array from list order
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

    // TransferHandler to reorder items inside JList via drag and drop (MOVE)
    private class ReorderListTransferHandler extends TransferHandler {
        private final DataFlavor localObjectFlavor = new DataFlavor(String.class, "String");
        private int sourceIndex = -1;

        public int getSourceActions(JComponent c) {
            return MOVE;
        }

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
                // adjust index when removing earlier element
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

        protected void exportDone(JComponent c, Transferable t, int action) {
            sourceIndex = -1;
        }
    }
}
