package com.medtrack.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.SQLException;
import java.util.List;

import com.medtrack.dao.MedicineDAO;
import com.medtrack.dao.BatchDAO;
import com.medtrack.model.Medicine;
import com.medtrack.service.FEFOService;

public class StockAllocationPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private JComboBox<Medicine> medicineCombo;
    private JTextField quantityField;
    private JLabel availableStockLabel;

    public StockAllocationPanel() {

        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(245, 247, 250));

        JLabel heading = new JLabel("FEFO Stock Allocation");
        heading.setFont(new Font("Arial", Font.BOLD, 26));
        heading.setForeground(new Color(35, 45, 60));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(245, 247, 250));
        topPanel.setBorder(
            BorderFactory.createEmptyBorder(25, 25, 10, 25)
        );

        topPanel.add(heading, BorderLayout.WEST);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(
            BorderFactory.createEmptyBorder(30, 30, 30, 30)
        );

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel medicineLabel = new JLabel("Select Medicine:");
        medicineLabel.setFont(new Font("Arial", Font.BOLD, 14));

        medicineCombo = new JComboBox<>();
        medicineCombo.setPreferredSize(new Dimension(250, 35));

        medicineCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {

                super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus
                );

                if (value instanceof Medicine) {
                    Medicine medicine = (Medicine) value;
                    setText(medicine.getMedicineName());
                }

                return this;
            }
        });

        JLabel quantityLabel = new JLabel("Required Quantity:");
        quantityLabel.setFont(new Font("Arial", Font.BOLD, 14));

        quantityField = new JTextField(20);
        quantityField.setPreferredSize(new Dimension(250, 35));

        availableStockLabel = new JLabel("Available Stock: 0");
        availableStockLabel.setFont(new Font("Arial", Font.BOLD, 14));
        availableStockLabel.setForeground(new Color(20, 130, 90));

        JButton allocateButton = new JButton("Allocate Stock");
        allocateButton.setBackground(new Color(31, 78, 121));
        allocateButton.setForeground(Color.WHITE);
        allocateButton.setFocusPainted(false);

        // Medicine selection
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(medicineLabel, gbc);

        gbc.gridx = 1;
        formPanel.add(medicineCombo, gbc);

        // Required quantity
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(quantityLabel, gbc);

        gbc.gridx = 1;
        formPanel.add(quantityField, gbc);

        // Available stock preview
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Stock Available:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        formPanel.add(availableStockLabel, gbc);

        // Allocate button
        gbc.gridx = 1;
        gbc.gridy = 3;
        formPanel.add(allocateButton, gbc);

        add(topPanel, BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);

        loadMedicines();

        allocateButton.addActionListener(e -> allocateStock());

        medicineCombo.addActionListener(
            e -> updateAvailableStock()
        );

        updateAvailableStock();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                loadMedicines();
                updateAvailableStock();
            }
        });
    }

    private void loadMedicines() {

        MedicineDAO dao = new MedicineDAO();
        List<Medicine> medicines = dao.getAllMedicines();

        Medicine previouslySelected = (Medicine) medicineCombo.getSelectedItem();
        int previousId = previouslySelected != null ? previouslySelected.getMedicineId() : -1;

        medicineCombo.removeAllItems();

        Medicine toSelect = null;
        for (Medicine medicine : medicines) {
            medicineCombo.addItem(medicine);
            if (medicine.getMedicineId() == previousId) {
                toSelect = medicine;
            }
        }

        if (toSelect != null) {
            medicineCombo.setSelectedItem(toSelect);
        }
    }

    private void allocateStock() {

        Medicine selectedMedicine =
            (Medicine) medicineCombo.getSelectedItem();

        if (selectedMedicine == null) {
            JOptionPane.showMessageDialog(
                this,
                "Please select a medicine before allocating stock.",
                "Selection Required",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String rawInput = quantityField.getText();
        if (rawInput == null || rawInput.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Please enter a quantity to allocate.",
                "Input Required",
                JOptionPane.WARNING_MESSAGE
            );
            quantityField.requestFocusInWindow();
            return;
        }

        int quantity;

        try {
            quantity = Integer.parseInt(rawInput.trim());

            if (quantity <= 0) {
                JOptionPane.showMessageDialog(
                    this,
                    "Requested quantity must be greater than zero.",
                    "Invalid Quantity",
                    JOptionPane.WARNING_MESSAGE
                );
                quantityField.requestFocusInWindow();
                return;
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                this,
                "Please enter a valid whole number for quantity.",
                "Invalid Number Format",
                JOptionPane.ERROR_MESSAGE
            );
            quantityField.requestFocusInWindow();
            return;
        }

        // Check available stock before allocation
        BatchDAO batchDAO = new BatchDAO();

        int availableStock =
            batchDAO.getAvailableStock(
                selectedMedicine.getMedicineId()
            );

        if (availableStock <= 0) {
            JOptionPane.showMessageDialog(
                this,
                "No eligible non-expired stock available for " + selectedMedicine.getMedicineName() + ".\n" +
                "All existing batches are either out of stock or expired.",
                "Stock Unavailable",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (quantity > availableStock) {

            JOptionPane.showMessageDialog(
                this,
                "Insufficient stock!\n\n" +
                "Available Non-Expired Stock: " + availableStock +
                "\nRequested Quantity: " + quantity,
                "Insufficient Stock",
                JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        try {

            FEFOService service = new FEFOService();

            service.allocateStock(
                selectedMedicine.getMedicineId(),
                quantity
            );

            JOptionPane.showMessageDialog(
                this,
                "Stock allocated successfully using FEFO!\n" +
                "Allocated " + quantity + " unit(s) for " + selectedMedicine.getMedicineName() + ".",
                "Allocation Successful",
                JOptionPane.INFORMATION_MESSAGE
            );

            quantityField.setText("");

            // Refresh available stock after allocation
            updateAvailableStock();

        } catch (SQLException | IllegalArgumentException e) {

            JOptionPane.showMessageDialog(
                this,
                e.getMessage(),
                "Allocation Failed",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void updateAvailableStock() {

        Medicine selectedMedicine =
            (Medicine) medicineCombo.getSelectedItem();

        if (selectedMedicine == null) {
            availableStockLabel.setText("Available Stock: 0");
            return;
        }

        BatchDAO batchDAO = new BatchDAO();

        int availableStock =
            batchDAO.getAvailableStock(
                selectedMedicine.getMedicineId()
            );

        availableStockLabel.setText(
            "Available Stock: " + availableStock
        );
    }
}
