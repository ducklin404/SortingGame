    package group10.client.ui;

    import javax.swing.*;
    import java.awt.*;
    import com.fasterxml.jackson.databind.JsonNode;

    public class WaitingPanel extends JPanel {
        private final JLabel opponentLabel = new JLabel("", SwingConstants.CENTER);
        private final JLabel messageLabel = new JLabel("", SwingConstants.CENTER);

        // last result UI (new)
        private final JLabel lastResultHeader = new JLabel("Last round result", SwingConstants.CENTER);
        private final JTextArea lastResultArea = new JTextArea();

        public WaitingPanel() {
            setLayout(new BorderLayout(6,6));
            add(opponentLabel, BorderLayout.NORTH);
            add(messageLabel, BorderLayout.CENTER);

            lastResultArea.setEditable(false);
            lastResultArea.setLineWrap(true);
            lastResultArea.setWrapStyleWord(true);
            lastResultArea.setRows(4);
            JScrollPane scroll = new JScrollPane(lastResultArea);
            scroll.setPreferredSize(new Dimension(300, 100));
            lastResultHeader.setVisible(false);
            scroll.setVisible(false);

            JPanel south = new JPanel(new BorderLayout());
            south.add(lastResultHeader, BorderLayout.NORTH);
            south.add(scroll, BorderLayout.CENTER);
            add(south, BorderLayout.SOUTH);
        }

        public void setOpponentName(String name) {
            opponentLabel.setText("Opponent: " + (name == null ? "unknown" : name));
        }

        public void showTemporaryMessage(String msg, int ms) {
            messageLabel.setText(msg);
            // non-blocking hide after ms
            Timer t = new Timer(ms, e -> messageLabel.setText(""));
            t.setRepeats(false);
            t.start();
        }

        // new: allow server result to be shown while waiting for next round
        public void setLastResult(JsonNode payload) {
            if (payload == null) {
                lastResultHeader.setVisible(false);
                lastResultArea.setText("");
                lastResultArea.getParent().setVisible(false);
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Round: ").append(payload.path("round").asText("n/a")).append("\n");
            sb.append("Message: ").append(payload.path("message").asText("")).append("\n");
            sb.append("A points: ").append(payload.path("playerAPoint").asInt(0)).append("  ");
            sb.append("B points: ").append(payload.path("playerBPoint").asInt(0)).append("\n\n");
            sb.append("A submission: ").append(payload.path("playerASubmission").isTextual() ? payload.path("playerASubmission").asText() : payload.path("playerASubmission").toString()).append("\n");
            sb.append("B submission: ").append(payload.path("playerBSubmission").isTextual() ? payload.path("playerBSubmission").asText() : payload.path("playerBSubmission").toString()).append("\n");

            lastResultArea.setText(sb.toString());
            lastResultArea.setCaretPosition(0);
            lastResultHeader.setVisible(true);
            lastResultArea.getParent().setVisible(true);
        }
    }
