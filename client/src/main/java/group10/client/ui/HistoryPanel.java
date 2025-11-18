package group10.client.ui;

import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class HistoryPanel extends JPanel {

    private final ScreenManager manager;

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Match ID", "Opponent", "My Points", "Opp Points", "Result", "Created At"},
            0
    );
    private final JTable table = new JTable(tableModel);

    public HistoryPanel(ScreenManager manager) {
        this.manager = manager;
        setLayout(new BorderLayout(10, 10));
        initUI();
    }

    private void initUI() {

        JLabel title = new JLabel("Match History", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(24f));

        add(title, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton backBtn = new JButton("Back");
        backBtn.addActionListener(e -> manager.show("main"));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(backBtn);
        add(bottom, BorderLayout.SOUTH);
    }

    /**
     * Được gọi từ client handler khi server gửi MATCH_HISTORY_DATA
     */
    public void updateHistory(JsonNode items) {

        tableModel.setRowCount(0); // clear table

        if (items == null || !items.isArray()) {
            System.out.println("No match history items!");
            return;
        }

        for (JsonNode item : items) {

            String matchId = item.get("matchId").asText();
            String opponent = item.get("opponent").asText();
            int myPts = item.get("myPoints").asInt();
            int oppPts = item.get("opponentPoints").asInt();
            String result = item.get("result").asText();
            String createdAt = item.get("createdAt").asText();

            tableModel.addRow(new Object[]{
                    matchId,
                    opponent,
                    myPts,
                    oppPts,
                    result,
                    createdAt
            });
        }
    }
}
