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

import com.medtrack.dao.MedicineDAO;
import com.medtrack.model.Medicine;

public class MedicinePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private JTable medicineTable;
    private DefaultTableModel tableModel;
    private transient TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField searchField;
    private JComboBox<String> stockFilterCombo;

    public MedicinePanel() {

        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(245, 247, 250));

        // Top banner: Heading and action buttons
        JLabel heading = new JLabel("Medicine Management");
        heading.setFont(new Font("Arial", Font.BOLD, 26));
        heading.setForeground(new Color(35, 45, 60));

        JButton addButton = new JButton("+ Add Medicine");
        addButton.setFont(new Font("Arial", Font.BOLD, 13));
        addButton.setBackground(new Color(31, 78, 121));
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);

        JButton editButton = new JButton("Edit Selected");
        editButton.setFont(new Font("Arial", Font.BOLD, 13));
        editButton.setBackground(new Color(52, 120, 246));
        editButton.setForeground(Color.WHITE);
        editButton.setFocusPainted(false);

        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.setFont(new Font("Arial", Font.BOLD, 13));
        deleteButton.setBackground(new Color(220, 75, 75));
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setFocusPainted(false);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.setFont(new Font("Arial", Font.PLAIN, 13));
        refreshButton.setFocusPainted(false);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonBar.setBackground(new Color(245, 247, 250));
        buttonBar.add(refreshButton);
        buttonBar.add(editButton);
        buttonBar.add(deleteButton);
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
        searchField.setToolTipText("Search by Medicine Name, Generic Name, Category, or ID");

        JLabel filterLabel = new JLabel("Stock Availability:");
        filterLabel.setFont(new Font("Arial", Font.BOLD, 13));

        stockFilterCombo = new JComboBox<>(new String[]{
            "All Stock Levels",
            "In Stock",
            "Low Stock",
            "Out of Stock"
        });

        JButton clearFilterBtn = new JButton("Clear Filters");
        clearFilterBtn.setFont(new Font("Arial", Font.PLAIN, 12));

        filterBar.add(searchLabel);
        filterBar.add(searchField);
        filterBar.add(Box.createHorizontalStrut(15));
        filterBar.add(filterLabel);
        filterBar.add(stockFilterCombo);
        filterBar.add(Box.createHorizontalStrut(10));
        filterBar.add(clearFilterBtn);

        JPanel northContainer = new JPanel();
        northContainer.setLayout(new BoxLayout(northContainer, BoxLayout.Y_AXIS));
        northContainer.setBackground(new Color(245, 247, 250));
        northContainer.add(topPanel);
        northContainer.add(filterBar);

        // Table
        String[] columns = {
            "ID", "Medicine Name", "Generic Name",
            "Category", "Manufacturer", "Unit", "Reorder Level", "Available Stock"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || columnIndex == 6 || columnIndex == 7) {
                    return Integer.class;
                }
                return String.class;
            }
        };

        medicineTable = new JTable(tableModel);
        medicineTable.setRowHeight(32);
        medicineTable.setFont(new Font("Arial", Font.PLAIN, 13));
        medicineTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        medicineTable.getTableHeader().setFont(
            new Font("Arial", Font.BOLD, 13)
        );

        rowSorter = new TableRowSorter<>(tableModel);
        medicineTable.setRowSorter(rowSorter);

        JScrollPane scrollPane = new JScrollPane(medicineTable);
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

        stockFilterCombo.addActionListener(e -> applyFilters());

        clearFilterBtn.addActionListener(e -> {
            searchField.setText("");
            stockFilterCombo.setSelectedIndex(0);
            rowSorter.setRowFilter(null);
        });

        // Action Listeners
        addButton.addActionListener(e -> showAddMedicineForm());
        editButton.addActionListener(e -> showEditMedicineForm());
        deleteButton.addActionListener(e -> handleDeleteMedicine());
        refreshButton.addActionListener(e -> loadMedicines());

        // Component Shown Listener
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                loadMedicines();
            }
        });

        loadMedicines();
    }

    private void applyFilters() {
        String searchText = searchField.getText().trim();
        String selectedStock = (String) stockFilterCombo.getSelectedItem();

        RowFilter<DefaultTableModel, Integer> combinedFilter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                // Stock filter check
                int reorderLevel = (Integer) entry.getValue(6);
                int totalStock = (Integer) entry.getValue(7);

                if ("In Stock".equals(selectedStock) && totalStock <= 0) {
                    return false;
                }
                if ("Low Stock".equals(selectedStock) && (totalStock > reorderLevel || totalStock <= 0)) {
                    return false;
                }
                if ("Out of Stock".equals(selectedStock) && totalStock > 0) {
                    return false;
                }

                // Text search check
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

    private void showAddMedicineForm() {

        JTextField nameField = new JTextField(15);
        JTextField genericField = new JTextField(15);
        JTextField categoryField = new JTextField(15);
        JTextField manufacturerField = new JTextField(15);
        JTextField unitField = new JTextField(15);
        JTextField reorderField = new JTextField(15);

        JPanel form = new JPanel(new GridLayout(6, 2, 10, 10));

        form.add(new JLabel("Medicine Name:"));
        form.add(nameField);

        form.add(new JLabel("Generic Name:"));
        form.add(genericField);

        form.add(new JLabel("Category:"));
        form.add(categoryField);

        form.add(new JLabel("Manufacturer:"));
        form.add(manufacturerField);

        form.add(new JLabel("Unit (e.g. Strip, Bottle, Box):"));
        form.add(unitField);

        form.add(new JLabel("Reorder Level:"));
        form.add(reorderField);

        int result = JOptionPane.showConfirmDialog(
            this,
            form,
            "Add New Medicine",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {

            String name = nameField.getText().trim();
            String generic = genericField.getText().trim();
            String category = categoryField.getText().trim();
            String manufacturer = manufacturerField.getText().trim();
            String unit = unitField.getText().trim();
            String reorderText = reorderField.getText().trim();

            if (name.isEmpty() || generic.isEmpty()
                    || category.isEmpty() || manufacturer.isEmpty()
                    || unit.isEmpty() || reorderText.isEmpty()) {

                JOptionPane.showMessageDialog(
                    this,
                    "Please fill in all required fields.",
                    "Missing Information",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            int reorderLevel;
            try {
                reorderLevel = Integer.parseInt(reorderText);
                if (reorderLevel < 0) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Reorder level cannot be negative.",
                        "Validation Error",
                        JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                    this,
                    "Please enter a valid whole number for reorder level.",
                    "Invalid Number",
                    JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            MedicineDAO dao = new MedicineDAO();

            // Validate duplicate name
            if (dao.isMedicineNameExists(name, 0)) {
                JOptionPane.showMessageDialog(
                    this,
                    "A medicine with the name '" + name + "' already exists in the catalog.",
                    "Duplicate Medicine",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            try {
                Medicine medicine = new Medicine(
                    0,
                    name,
                    generic,
                    category,
                    manufacturer,
                    unit,
                    reorderLevel
                );

                boolean success = dao.addMedicine(medicine);

                if (success) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Medicine added successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                    loadMedicines();
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "Unable to add medicine. Please try again.",
                        "Operation Failed",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(
                    this,
                    "Database operation could not be completed: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void showEditMedicineForm() {
        int selectedRow = medicineTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                this,
                "Please select a medicine from the table to edit.",
                "Selection Required",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int modelRow = medicineTable.convertRowIndexToModel(selectedRow);
        int medicineId = (Integer) tableModel.getValueAt(modelRow, 0);

        MedicineDAO dao = new MedicineDAO();
        Medicine existing = dao.getMedicineById(medicineId);
        if (existing == null) {
            JOptionPane.showMessageDialog(
                this,
                "Could not find medicine details for ID: " + medicineId,
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        JTextField nameField = new JTextField(existing.getMedicineName(), 15);
        JTextField genericField = new JTextField(existing.getGenericName(), 15);
        JTextField categoryField = new JTextField(existing.getCategory(), 15);
        JTextField manufacturerField = new JTextField(existing.getManufacturer(), 15);
        JTextField unitField = new JTextField(existing.getUnit(), 15);
        JTextField reorderField = new JTextField(String.valueOf(existing.getReorderLevel()), 15);

        JPanel form = new JPanel(new GridLayout(6, 2, 10, 10));
        form.add(new JLabel("Medicine Name:"));
        form.add(nameField);
        form.add(new JLabel("Generic Name:"));
        form.add(genericField);
        form.add(new JLabel("Category:"));
        form.add(categoryField);
        form.add(new JLabel("Manufacturer:"));
        form.add(manufacturerField);
        form.add(new JLabel("Unit:"));
        form.add(unitField);
        form.add(new JLabel("Reorder Level:"));
        form.add(reorderField);

        int result = JOptionPane.showConfirmDialog(
            this,
            form,
            "Edit Medicine (ID: " + medicineId + ")",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String generic = genericField.getText().trim();
            String category = categoryField.getText().trim();
            String manufacturer = manufacturerField.getText().trim();
            String unit = unitField.getText().trim();
            String reorderText = reorderField.getText().trim();

            if (name.isEmpty() || generic.isEmpty()
                    || category.isEmpty() || manufacturer.isEmpty()
                    || unit.isEmpty() || reorderText.isEmpty()) {
                JOptionPane.showMessageDialog(
                    this,
                    "Please fill in all required fields.",
                    "Missing Information",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            int reorderLevel;
            try {
                reorderLevel = Integer.parseInt(reorderText);
                if (reorderLevel < 0) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Reorder level cannot be negative.",
                        "Validation Error",
                        JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                    this,
                    "Please enter a valid whole number for reorder level.",
                    "Invalid Number",
                    JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            if (dao.isMedicineNameExists(name, medicineId)) {
                JOptionPane.showMessageDialog(
                    this,
                    "Another medicine with the name '" + name + "' already exists.",
                    "Duplicate Name",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            try {
                existing.setMedicineName(name);
                existing.setGenericName(generic);
                existing.setCategory(category);
                existing.setManufacturer(manufacturer);
                existing.setUnit(unit);
                existing.setReorderLevel(reorderLevel);

                boolean success = dao.updateMedicine(existing);
                if (success) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Medicine updated successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                    loadMedicines();
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "Could not update medicine.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(
                    this,
                    "Database error: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void handleDeleteMedicine() {
        int selectedRow = medicineTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                this,
                "Please select a medicine from the table to delete.",
                "Selection Required",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int modelRow = medicineTable.convertRowIndexToModel(selectedRow);
        int medicineId = (Integer) tableModel.getValueAt(modelRow, 0);
        String medicineName = (String) tableModel.getValueAt(modelRow, 1);

        MedicineDAO dao = new MedicineDAO();

        try {
            if (dao.hasBatches(medicineId)) {
                JOptionPane.showMessageDialog(
                    this,
                    "Cannot delete '" + medicineName + "' because there are inventory batches associated with it.\n" +
                    "To maintain audit integrity, medicines with existing batches cannot be removed.",
                    "Deletion Prevented",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete '" + medicineName + "' (ID: " + medicineId + ")?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );

            if (confirm == JOptionPane.YES_OPTION) {
                boolean deleted = dao.deleteMedicine(medicineId);
                if (deleted) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Medicine deleted successfully.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                    loadMedicines();
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "Could not delete medicine.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                this,
                "Database error during deletion: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void loadMedicines() {

        tableModel.setRowCount(0);

        MedicineDAO dao = new MedicineDAO();
        List<Object[]> medicines = dao.getAllMedicinesWithStock();

        for (Object[] row : medicines) {
            tableModel.addRow(row);
        }

        applyFilters();
    }
}
