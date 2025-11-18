package group10.client.ui;

import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardPanel extends JPanel {

    private final ScreenManager manager;

    private JTable table;
    private DefaultTableModel tableModel;

    private JButton backBtn, prevBtn, nextBtn;
    private JLabel pageLabel;

    // Data lưu toàn bộ leaderboard từ server
    private List<JsonNode> allItems = new ArrayList<>();

    private int currentPage = 0;
    private int pageSize = 10;  // số người mỗi trang

    public LeaderboardPanel(ScreenManager manager) {
        this.manager = manager;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10,10));
        setBorder(new EmptyBorder(20,20,20,20));

        JLabel title = new JLabel("Leaderboard", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(26f));
        add(title, BorderLayout.NORTH);

        // Table
        tableModel = new DefaultTableModel(
                new String[]{"Rank", "Player", "Points", "Wins", "Losses", "Draws", "Matches"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(26);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom buttons panel
        JPanel bottom = new JPanel(new BorderLayout());

        // Back button
        backBtn = new JButton("Back");
        backBtn.addActionListener(e -> manager.show("main"));
        bottom.add(backBtn, BorderLayout.WEST);

        // Pagination controls
        JPanel paging = new JPanel(new FlowLayout(FlowLayout.CENTER));

        prevBtn = new JButton("<< Prev");
        nextBtn = new JButton("Next >>");
        pageLabel = new JLabel("Page 1 / 1");

        prevBtn.addActionListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                updateTablePage();
            }
        });

        nextBtn.addActionListener(e -> {
            int totalPages = getTotalPages();
            if (currentPage < totalPages - 1) {
                currentPage++;
                updateTablePage();
            }
        });

        paging.add(prevBtn);
        paging.add(pageLabel);
        paging.add(nextBtn);

        bottom.add(paging, BorderLayout.CENTER);

        add(bottom, BorderLayout.SOUTH);
    }


    /** Called by Client Handler when LEADERBOARD_DATA arrived */
    public void updateLeaderboard(JsonNode itemsArray) {
        allItems.clear();
        currentPage = 0;

        if (itemsArray != null && itemsArray.isArray()) {
            for (JsonNode item : itemsArray) {
                allItems.add(item);
            }
        }

        updateTablePage();
    }


    /** Render bảng theo page hiện tại */
    private void updateTablePage() {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);

            int start = currentPage * pageSize;
            int end = Math.min(start + pageSize, allItems.size());

            for (int i = start; i < end; i++) {
                JsonNode item = allItems.get(i);

                int rank = i + 1;
                String username = item.path("username").asText();
                double points = item.path("totalPoints").asDouble();
                int wins = item.path("wins").asInt();
                int losses = item.path("losses").asInt();
                int draws = item.path("draws").asInt();
                int matches = item.path("matchesPlayed").asInt();

                tableModel.addRow(new Object[]{
                        rank, username, points, wins, losses, draws, matches
                });
            }

            pageLabel.setText("Page " + (currentPage+1) + " / " + getTotalPages());
            prevBtn.setEnabled(currentPage > 0);
            nextBtn.setEnabled(currentPage < getTotalPages() - 1);
        });
    }

    private int getTotalPages() {
        if (allItems.isEmpty()) return 1;
        return (int) Math.ceil(allItems.size() / (double) pageSize);
    }
}
