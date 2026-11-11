package com.medtrack.ui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.SQLException;
import java.util.List;

import com.medtrack.model.Batch;
import com.medtrack.service.ExpiryService;

public class ExpiryPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private JTable table;
    private DefaultTableModel tableModel;
    private transient TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField searchField;
    private JComboBox<String> statusFilterCombo;

    public ExpiryPanel() {

        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(245, 247, 250));

        JLabel heading = new JLabel("Expiry Alerts");
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

        // Search and Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        filterBar.setBackground(Color.WHITE);
        filterBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(220, 225, 230)),
            BorderFactory.createEmptyBorder(6, 20, 6, 20)
        ));

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font("Arial", Font.BOLD, 13));

        searchField = new JTextField(18);
        searchField.setToolTipText("Search by Batch Number or ID");

        JLabel filterLabel = new JLabel("Status Filter:");
        filterLabel.setFont(new Font("Arial", Font.BOLD, 13));

        statusFilterCombo = new JComboBox<>(new String[]{
            "All Alerts",
            "EXPIRED",
            "EXPIRING SOON"
        });

        JButton clearFilterBtn = new JButton("Clear");
        clearFilterBtn.setFont(new Font("Arial", Font.PLAIN, 12));

        filterBar.add(searchLabel);
        filterBar.add(searchField);
        filterBar.add(Box.createHorizontalStrut(15));
        filterBar.add(filterLabel);
        filterBar.add(statusFilterCombo);
        filterBar.add(Box.createHorizontalStrut(10));
        filterBar.add(clearFilterBtn);

        JPanel northContainer = new JPanel();
        northContainer.setLayout(new BoxLayout(northContainer, BoxLayout.Y_AXIS));
        northContainer.setBackground(new Color(245, 247, 250));
        northContainer.add(topPanel);
        northContainer.add(filterBar);

        String[] columns = {
            "Batch ID", "Medicine ID", "Batch Number",
            "Expiry Date", "Available Quantity", "Status"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || columnIndex == 1 || columnIndex == 4) {
                    return Integer.class;
                }
                return String.class;
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

        statusFilterCombo.addActionListener(e -> applyFilters());

        clearFilterBtn.addActionListener(e -> {
            searchField.setText("");
            statusFilterCombo.setSelectedIndex(0);
            rowSorter.setRowFilter(null);
        });

        refreshButton.addActionListener(e -> loadExpiryAlerts());

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                loadExpiryAlerts();
            }
        });

        loadExpiryAlerts();
    }

    private void applyFilters() {
        String searchText = searchField.getText().trim();
        String selectedStatus = (String) statusFilterCombo.getSelectedItem();

        RowFilter<DefaultTableModel, Integer> combinedFilter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                String status = (String) entry.getValue(5);

                if ("EXPIRED".equals(selectedStatus) && !"EXPIRED".equals(status)) {
                    return false;
                }
                if ("EXPIRING SOON".equals(selectedStatus) && !"EXPIRING SOON".equals(status)) {
                    return false;
                }

                if (searchText.isEmpty()) {
                    return true;
                }

                String lower = searchText.toLowerCase();
                for (int i = 0; i < entry.getValueCount(); i++) {
                    Object val = entry.getValue(i);
                    if (val != null && val.toString().toLowerCase().contains(lower)) {
                        return true;
                    }
                }
                return false;
            }
        };

        rowSorter.setRowFilter(combinedFilter);
    }

    private void loadExpiryAlerts() {

        tableModel.setRowCount(0);

        ExpiryService service = new ExpiryService();

        try {
            List<Batch> expired = service.getExpiredBatches();
            List<Batch> expiringSoon = service.getExpiringSoonBatches(45);

            for (Batch batch : expired) {
                addBatchRow(batch, "EXPIRED");
            }

            for (Batch batch : expiringSoon) {
                addBatchRow(batch, "EXPIRING SOON");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                this,
                "Unable to load expiry alerts: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
        }

        applyFilters();
    }

    private void addBatchRow(Batch batch, String status) {

        Object[] row = {
            batch.getBatchId(),
            batch.getMedicineId(),
            batch.getBatchNumber(),
            batch.getExpiryDate(),
            batch.getAvailableQuantity(),
            status
        };

        tableModel.addRow(row);
    }
}
