package com.medtrack.ui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Map;

import com.medtrack.service.StockAlertService;

public class LowStockPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private JTable table;
    private DefaultTableModel tableModel;
    private transient TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField searchField;

    public LowStockPanel() {

        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(245, 247, 250));

        JLabel heading = new JLabel("Low Stock Alerts");
        heading.setFont(new Font("Arial", Font.BOLD, 26));
        heading.setForeground(new Color(35, 45, 60));

        JButton refreshButton = new JButton("Refresh");
        refreshButton.setFont(new Font("Arial", Font.PLAIN, 13));
        refreshButton.setFocusPainted(false);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonBar.setBackground(new Color(245, 247, 250));
        buttonBar.add(refreshButton);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(245, 247, 250));
        topPanel.setBorder(
            BorderFactory.createEmptyBorder(20, 25, 10, 25)
        );
        topPanel.add(heading, BorderLayout.WEST);
        topPanel.add(buttonBar, BorderLayout.EAST);

        // Search Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        filterBar.setBackground(Color.WHITE);
        filterBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(220, 225, 230)),
            BorderFactory.createEmptyBorder(6, 20, 6, 20)
        ));

        JLabel searchLabel = new JLabel("Search Medicine:");
        searchLabel.setFont(new Font("Arial", Font.BOLD, 13));

        searchField = new JTextField(20);
        searchField.setToolTipText("Search low stock medicines");

        JButton clearFilterBtn = new JButton("Clear");
        clearFilterBtn.setFont(new Font("Arial", Font.PLAIN, 12));

        filterBar.add(searchLabel);
        filterBar.add(searchField);
        filterBar.add(Box.createHorizontalStrut(10));
        filterBar.add(clearFilterBtn);

        JPanel northContainer = new JPanel();
        northContainer.setLayout(new BoxLayout(northContainer, BoxLayout.Y_AXIS));
        northContainer.setBackground(new Color(245, 247, 250));
        northContainer.add(topPanel);
        northContainer.add(filterBar);

        String[] columns = {
            "Medicine Name", "Available Stock", "Reorder Status", "Alert Level"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(32);
        table.setFont(new Font("Arial", Font.PLAIN, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(
            new Font("Arial", Font.BOLD, 12)
        );

        rowSorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(rowSorter);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(
            BorderFactory.createEmptyBorder(10, 25, 25, 25)
        );

        add(northContainer, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { applyFilters(); }
            @Override
            public void removeUpdate(DocumentEvent e) { applyFilters(); }
            @Override
            public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });

        clearFilterBtn.addActionListener(e -> {
            searchField.setText("");
            rowSorter.setRowFilter(null);
        });

        refreshButton.addActionListener(e -> loadLowStockAlerts());

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                loadLowStockAlerts();
            }
        });

        loadLowStockAlerts();
    }

    private void applyFilters() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            rowSorter.setRowFilter(null);
            return;
        }

        rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(searchText)));
    }

    private void loadLowStockAlerts() {

        tableModel.setRowCount(0);

        StockAlertService service = new StockAlertService();

        Map<String, Integer> lowStock =
            service.getLowStockMedicines();

        if (lowStock.isEmpty()) {
            Object[] row = {
                "No low-stock medicines",
                "-",
                "All stock levels are above reorder thresholds",
                "ALL STOCK LEVELS NORMAL"
            };

            tableModel.addRow(row);
            return;
        }

        for (Map.Entry<String, Integer> entry : lowStock.entrySet()) {

            int stock = entry.getValue();
            String alertLevel = stock == 0 ? "CRITICAL (OUT OF STOCK)" : "LOW STOCK";

            Object[] row = {
                entry.getKey(),
                stock,
                "Stock at or below reorder level",
                alertLevel
            };

            tableModel.addRow(row);
        }

        applyFilters();
    }
}
