package com.medtrack.ui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

import com.medtrack.dao.StockDAO;
import com.medtrack.model.StockTransaction;

public class TransactionPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private JTable transactionTable;
    private DefaultTableModel tableModel;
    private transient TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField searchField;
    private JComboBox<String> typeFilterCombo;

    public TransactionPanel() {

        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(245, 247, 250));

        // Heading
        JLabel heading = new JLabel("Stock Transactions");
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
        searchField.setToolTipText("Search by Reference, Batch ID, or Notes");

        JLabel filterLabel = new JLabel("Transaction Type:");
        filterLabel.setFont(new Font("Arial", Font.BOLD, 13));

        typeFilterCombo = new JComboBox<>(new String[]{
            "All Types",
            "PURCHASE",
            "SALE"
        });

        JButton clearFilterBtn = new JButton("Clear");
        clearFilterBtn.setFont(new Font("Arial", Font.PLAIN, 12));

        filterBar.add(searchLabel);
        filterBar.add(searchField);
        filterBar.add(Box.createHorizontalStrut(15));
        filterBar.add(filterLabel);
        filterBar.add(typeFilterCombo);
        filterBar.add(Box.createHorizontalStrut(10));
        filterBar.add(clearFilterBtn);

        JPanel northContainer = new JPanel();
        northContainer.setLayout(new BoxLayout(northContainer, BoxLayout.Y_AXIS));
        northContainer.setBackground(new Color(245, 247, 250));
        northContainer.add(topPanel);
        northContainer.add(filterBar);

        // Table columns
        String[] columns = {
            "Transaction ID",
            "Batch ID",
            "Transaction Type",
            "Quantity",
            "Reference Number",
            "Notes",
            "Transaction Date"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || columnIndex == 1 || columnIndex == 3) {
                    return Integer.class;
                }
                return String.class;
            }
        };

        transactionTable = new JTable(tableModel);
        transactionTable.setRowHeight(32);
        transactionTable.setFont(new Font("Arial", Font.PLAIN, 12));
        transactionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        transactionTable.getTableHeader().setFont(
            new Font("Arial", Font.BOLD, 12)
        );

        rowSorter = new TableRowSorter<>(tableModel);
        transactionTable.setRowSorter(rowSorter);

        JScrollPane scrollPane = new JScrollPane(transactionTable);
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

        typeFilterCombo.addActionListener(e -> applyFilters());

        clearFilterBtn.addActionListener(e -> {
            searchField.setText("");
            typeFilterCombo.setSelectedIndex(0);
            rowSorter.setRowFilter(null);
        });

        refreshButton.addActionListener(e -> loadTransactions());

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                loadTransactions();
            }
        });

        loadTransactions();
    }

    private void applyFilters() {
        String searchText = searchField.getText().trim();
        String selectedType = (String) typeFilterCombo.getSelectedItem();

        RowFilter<DefaultTableModel, Integer> combinedFilter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                String type = (String) entry.getValue(2);

                if ("PURCHASE".equals(selectedType) && !"PURCHASE".equalsIgnoreCase(type)) {
                    return false;
                }
                if ("SALE".equals(selectedType) && !"SALE".equalsIgnoreCase(type)) {
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

    private void loadTransactions() {

        tableModel.setRowCount(0);

        StockDAO dao = new StockDAO();
        List<StockTransaction> transactions = dao.getAllTransactions();

        for (StockTransaction transaction : transactions) {

            Object[] row = {
                transaction.getTransactionId(),
                transaction.getBatchId(),
                transaction.getTransactionType(),
                transaction.getQuantity(),
                transaction.getReferenceNumber() != null ? transaction.getReferenceNumber() : "-",
                transaction.getNotes() != null ? transaction.getNotes() : "-",
                transaction.getTransactionDate()
            };

            tableModel.addRow(row);
        }

        applyFilters();
    }
}
