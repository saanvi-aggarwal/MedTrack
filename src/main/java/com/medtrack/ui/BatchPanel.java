package com.medtrack.ui;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.sql.SQLException;

import com.medtrack.dao.BatchDAO;
import com.medtrack.dao.MedicineDAO;
import com.medtrack.model.Batch;
import com.medtrack.model.Medicine;

public class BatchPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private JTable batchTable;
    private DefaultTableModel tableModel;
    private transient TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField searchField;
    private JComboBox<String> expiryFilterCombo;

    public BatchPanel() {

        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(245, 247, 250));

        JLabel heading = new JLabel("Batch Management");
        heading.setFont(new Font("Arial", Font.BOLD, 26));
        heading.setForeground(new Color(35, 45, 60));

        JButton addButton = new JButton("+ Add Batch");
        addButton.setFont(new Font("Arial", Font.BOLD, 13));
        addButton.setBackground(new Color(31, 78, 121));
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.setFont(new Font("Arial", Font.PLAIN, 13));
        refreshButton.setFocusPainted(false);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonBar.setBackground(new Color(245, 247, 250));
        buttonBar.add(refreshButton);
        buttonBar.add(addButton);

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
        searchField.setToolTipText("Search by Batch Number or Medicine Name");

        JLabel filterLabel = new JLabel("Expiry Status:");
        filterLabel.setFont(new Font("Arial", Font.BOLD, 13));

        expiryFilterCombo = new JComboBox<>(new String[]{
            "All Batches",
            "Active / Valid",
            "Expiring Soon (<= 45 days)",
            "Expired",
            "Depleted / Out of Stock"
        });

        JButton clearFilterBtn = new JButton("Clear Filters");
        clearFilterBtn.setFont(new Font("Arial", Font.PLAIN, 12));

        filterBar.add(searchLabel);
        filterBar.add(searchField);
        filterBar.add(Box.createHorizontalStrut(15));
        filterBar.add(filterLabel);
        filterBar.add(expiryFilterCombo);
        filterBar.add(Box.createHorizontalStrut(10));
        filterBar.add(clearFilterBtn);

        JPanel northContainer = new JPanel();
        northContainer.setLayout(new BoxLayout(northContainer, BoxLayout.Y_AXIS));
        northContainer.setBackground(new Color(245, 247, 250));
        northContainer.add(topPanel);
        northContainer.add(filterBar);

        String[] columns = {
            "Batch ID", "Batch Number", "Medicine",
            "Manufacturing Date", "Expiry Date",
            "Purchase Price", "Selling Price",
            "Quantity Received", "Available Quantity", "Status"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || columnIndex == 7 || columnIndex == 8) {
                    return Integer.class;
                }
                return String.class;
            }
        };

        batchTable = new JTable(tableModel);
        batchTable.setRowHeight(32);
        batchTable.setFont(new Font("Arial", Font.PLAIN, 12));
        batchTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        batchTable.getTableHeader().setFont(
            new Font("Arial", Font.BOLD, 12)
        );

        rowSorter = new TableRowSorter<>(tableModel);
        batchTable.setRowSorter(rowSorter);

        JScrollPane scrollPane = new JScrollPane(batchTable);
        scrollPane.setBorder(
            BorderFactory.createEmptyBorder(10, 25, 25, 25)
        );

        add(northContainer, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Search and Filter Listeners
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { applyFilters(); }
            @Override
            public void removeUpdate(DocumentEvent e) { applyFilters(); }
            @Override
            public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });

        expiryFilterCombo.addActionListener(e -> applyFilters());

        clearFilterBtn.addActionListener(e -> {
            searchField.setText("");
            expiryFilterCombo.setSelectedIndex(0);
            rowSorter.setRowFilter(null);
        });

        addButton.addActionListener(e -> showAddBatchForm());
        refreshButton.addActionListener(e -> loadBatches());

        loadBatches();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                loadBatches();
            }
        });
    }

    private void applyFilters() {
        String searchText = searchField.getText().trim();
        String selectedStatus = (String) expiryFilterCombo.getSelectedItem();

        RowFilter<DefaultTableModel, Integer> combinedFilter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                String status = (String) entry.getValue(9);

                if ("Active / Valid".equals(selectedStatus) && !"ACTIVE".equals(status)) {
                    return false;
                }
                if ("Expiring Soon (<= 45 days)".equals(selectedStatus) && !"EXPIRING SOON".equals(status)) {
                    return false;
                }
                if ("Expired".equals(selectedStatus) && !"EXPIRED".equals(status)) {
                    return false;
                }
                if ("Depleted / Out of Stock".equals(selectedStatus) && !"DEPLETED".equals(status)) {
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

    private void showAddBatchForm() {

        MedicineDAO medicineDAO = new MedicineDAO();
        List<Medicine> medicines = medicineDAO.getAllMedicines();

        if (medicines.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Please add a medicine to the catalog before registering a batch.",
                "Medicine Required",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        JComboBox<Medicine> medicineBox =
            new JComboBox<>(medicines.toArray(new Medicine[0]));

        medicineBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus
                );

                if (value instanceof Medicine) {
                    setText(((Medicine) value).getMedicineName());
                }

                return this;
            }
        });

        JTextField batchNumberField = new JTextField(15);
        JTextField manufacturingDateField = new JTextField(15);
        JTextField expiryDateField = new JTextField(15);
        JTextField purchasePriceField = new JTextField(15);
        JTextField sellingPriceField = new JTextField(15);
        JTextField quantityField = new JTextField(15);

        // Pre-fill today's date for convenience
        manufacturingDateField.setText(LocalDate.now().toString());
        expiryDateField.setText(LocalDate.now().plusYears(1).toString());

        JPanel form = new JPanel(new GridLayout(7, 2, 10, 10));

        form.add(new JLabel("Select Medicine:"));
        form.add(medicineBox);

        form.add(new JLabel("Batch Number:"));
        form.add(batchNumberField);

        form.add(new JLabel("Manufacturing Date (YYYY-MM-DD):"));
        form.add(manufacturingDateField);

        form.add(new JLabel("Expiry Date (YYYY-MM-DD):"));
        form.add(expiryDateField);

        form.add(new JLabel("Purchase Price:"));
        form.add(purchasePriceField);

        form.add(new JLabel("Selling Price:"));
        form.add(sellingPriceField);

        form.add(new JLabel("Quantity Received:"));
        form.add(quantityField);

        int result = JOptionPane.showConfirmDialog(
            this,
            form,
            "Register New Batch",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            Medicine selectedMedicine =
                (Medicine) medicineBox.getSelectedItem();

            String batchNumber = batchNumberField.getText().trim();
            String manufacturingText = manufacturingDateField.getText().trim();
            String expiryText = expiryDateField.getText().trim();
            String purchasePriceText = purchasePriceField.getText().trim();
            String sellingPriceText = sellingPriceField.getText().trim();
            String quantityText = quantityField.getText().trim();

            if (selectedMedicine == null ||
                batchNumber.isEmpty() ||
                manufacturingText.isEmpty() ||
                expiryText.isEmpty() ||
                purchasePriceText.isEmpty() ||
                sellingPriceText.isEmpty() ||
                quantityText.isEmpty()) {

                JOptionPane.showMessageDialog(
                    this,
                    "Please fill in all required fields.",
                    "Missing Information",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            LocalDate manufacturingDate = LocalDate.parse(manufacturingText);
            LocalDate expiryDate = LocalDate.parse(expiryText);

            if (!expiryDate.isAfter(manufacturingDate)) {
                JOptionPane.showMessageDialog(
                    this,
                    "Expiry date must be strictly after the manufacturing date.",
                    "Invalid Dates",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            BigDecimal purchasePrice;
            BigDecimal sellingPrice;
            int quantity;

            try {
                purchasePrice = new BigDecimal(purchasePriceText);
                sellingPrice = new BigDecimal(sellingPriceText);

                if (purchasePrice.compareTo(BigDecimal.ZERO) < 0 ||
                    sellingPrice.compareTo(BigDecimal.ZERO) < 0) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Purchase and selling prices cannot be negative.",
                        "Invalid Prices",
                        JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(
                    this,
                    "Please enter valid numeric values for purchase and selling prices.",
                    "Invalid Price Format",
                    JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            try {
                quantity = Integer.parseInt(quantityText);
                if (quantity <= 0) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Quantity received must be greater than zero.",
                        "Invalid Quantity",
                        JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(
                    this,
                    "Please enter a valid whole number for quantity received.",
                    "Invalid Quantity Format",
                    JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            BatchDAO dao = new BatchDAO();

            // Duplicate batch validation
            if (dao.isBatchNumberExists(selectedMedicine.getMedicineId(), batchNumber)) {
                JOptionPane.showMessageDialog(
                    this,
                    "A batch with number '" + batchNumber + "' already exists for " +
                    selectedMedicine.getMedicineName() + ".\nBatch numbers must be unique per medicine.",
                    "Duplicate Batch",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            Batch batch = new Batch();
            batch.setMedicineId(selectedMedicine.getMedicineId());
            batch.setBatchNumber(batchNumber);
            batch.setManufacturingDate(manufacturingDate);
            batch.setExpiryDate(expiryDate);
            batch.setPurchasePrice(purchasePrice);
            batch.setSellingPrice(sellingPrice);
            batch.setQuantityReceived(quantity);

            boolean success = dao.addBatch(batch);

            if (success) {
                JOptionPane.showMessageDialog(
                    this,
                    "Batch registered successfully!\nInitial purchase transaction has been recorded.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
                );
                loadBatches();
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "Unable to register batch. Please try again.",
                    "Operation Failed",
                    JOptionPane.ERROR_MESSAGE
                );
            }

        } catch (java.time.format.DateTimeParseException e) {
            JOptionPane.showMessageDialog(
                this,
                "Please enter dates in YYYY-MM-DD format (e.g. 2026-12-31).",
                "Invalid Date Format",
                JOptionPane.ERROR_MESSAGE
            );

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                this,
                "Database error: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void loadBatches() {

        tableModel.setRowCount(0);

        BatchDAO dao = new BatchDAO();
        List<Object[]> batches = dao.getAllBatchDetails();
        LocalDate today = LocalDate.now();

        for (Object[] batch : batches) {
            java.sql.Date expSqlDate = (java.sql.Date) batch[4];
            int availQty = (Integer) batch[8];

            String status = "ACTIVE";
            if (availQty <= 0) {
                status = "DEPLETED";
            } else if (expSqlDate != null) {
                LocalDate expDate = expSqlDate.toLocalDate();
                if (expDate.isBefore(today)) {
                    status = "EXPIRED";
                } else if (!expDate.isAfter(today.plusDays(45))) {
                    status = "EXPIRING SOON";
                }
            }

            Object[] fullRow = new Object[]{
                batch[0], batch[1], batch[2], batch[3], batch[4],
                batch[5], batch[6], batch[7], batch[8], status
            };
            tableModel.addRow(fullRow);
        }

        applyFilters();
    }
}
