package com.medtrack.ui;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.sql.SQLException;

import com.medtrack.dao.MedicineDAO;
import com.medtrack.dao.BatchDAO;
import com.medtrack.service.ExpiryService;
import com.medtrack.service.StockAlertService;

public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private JPanel contentPanel;
    private JPanel cardsPanel;
    private transient Map<String, JButton> sidebarButtons = new HashMap<>();
    private String currentView = "Dashboard";

    public MainFrame() {

        setTitle("MedTrack - Medicine Stock Management");
        setSize(1050, 680);
        setMinimumSize(new Dimension(950, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main background
        JPanel mainPanel = new JPanel(new BorderLayout());
        contentPanel = new JPanel(new CardLayout());
        mainPanel.setBackground(new Color(245, 247, 250));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(31, 78, 121));
        header.setPreferredSize(new Dimension(1050, 75));

        JLabel title = new JLabel("  MedTrack");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 26));

        JLabel subtitle = new JLabel("Medicine Expiry & Stock Monitoring System   ");
        subtitle.setForeground(new Color(220, 235, 250));
        subtitle.setFont(new Font("Arial", Font.PLAIN, 14));

        header.add(title, BorderLayout.WEST);
        header.add(subtitle, BorderLayout.EAST);

        // Welcome section with quick refresh
        JPanel welcomePanel = new JPanel(new BorderLayout());
        welcomePanel.setBackground(new Color(245, 247, 250));
        welcomePanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 15, 40));

        JPanel welcomeTextPanel = new JPanel();
        welcomeTextPanel.setLayout(new BoxLayout(welcomeTextPanel, BoxLayout.Y_AXIS));
        welcomeTextPanel.setBackground(new Color(245, 247, 250));

        JLabel welcome = new JLabel("Inventory Overview");
        welcome.setFont(new Font("Arial", Font.BOLD, 26));
        welcome.setForeground(new Color(35, 45, 60));

        JLabel description = new JLabel(
            "Live snapshot of medicine catalog, inventory batches, upcoming expiries and low stock alerts."
        );
        description.setFont(new Font("Arial", Font.PLAIN, 14));
        description.setForeground(new Color(110, 120, 135));

        welcomeTextPanel.add(welcome);
        welcomeTextPanel.add(Box.createVerticalStrut(6));
        welcomeTextPanel.add(description);

        JButton refreshDashboardBtn = new JButton("Refresh Metrics");
        refreshDashboardBtn.setFont(new Font("Arial", Font.BOLD, 12));
        refreshDashboardBtn.setBackground(new Color(31, 78, 121));
        refreshDashboardBtn.setForeground(Color.WHITE);
        refreshDashboardBtn.setFocusPainted(false);
        refreshDashboardBtn.addActionListener(e -> refreshDashboardCards());

        welcomePanel.add(welcomeTextPanel, BorderLayout.WEST);
        welcomePanel.add(refreshDashboardBtn, BorderLayout.EAST);

        // Sidebar
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(25, 39, 52));
        sidebar.setPreferredSize(new Dimension(220, 500));
        sidebar.setBorder(
            BorderFactory.createEmptyBorder(25, 15, 20, 15)
        );

        String[] menuItems = {
            "Dashboard",
            "Medicines",
            "Batches",
            "Stock Transactions",
            "Stock Allocation",
            "Expiry Alerts",
            "Low Stock Alerts"
        };

        for (String item : menuItems) {

            JButton button = new JButton(item);

            button.setMaximumSize(new Dimension(190, 44));
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setBackground(item.equals("Dashboard") ? new Color(42, 63, 84) : new Color(25, 39, 52));
            button.setForeground(Color.WHITE);
            button.setFont(new Font("Arial", Font.PLAIN, 14));
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));

            button.addActionListener(e -> switchView(item));

            sidebarButtons.put(item, button);
            sidebar.add(button);
            sidebar.add(Box.createVerticalStrut(8));
        }

        mainPanel.add(sidebar, BorderLayout.WEST);
        mainPanel.add(header, BorderLayout.NORTH);

        // Dashboard content
        JPanel dashboardContent = new JPanel(new BorderLayout());
        dashboardContent.setBackground(new Color(245, 247, 250));
        dashboardContent.add(welcomePanel, BorderLayout.NORTH);

        // Summary cards
        cardsPanel = new JPanel(new GridLayout(2, 2, 25, 25));
        cardsPanel.setBackground(new Color(245, 247, 250));
        cardsPanel.setBorder(
            BorderFactory.createEmptyBorder(10, 40, 40, 40)
        );

        refreshDashboardCards();

        dashboardContent.add(cardsPanel, BorderLayout.CENTER);

        // Refresh dashboard whenever it becomes visible
        dashboardContent.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                refreshDashboardCards();
            }
        });

        contentPanel.add(dashboardContent, "Dashboard");
        contentPanel.add(new MedicinePanel(), "Medicines");
        contentPanel.add(new BatchPanel(), "Batches");
        contentPanel.add(new TransactionPanel(), "Stock Transactions");
        contentPanel.add(new ExpiryPanel(), "Expiry Alerts");
        contentPanel.add(new LowStockPanel(), "Low Stock Alerts");
        contentPanel.add(new StockAllocationPanel(), "Stock Allocation");

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        add(mainPanel);
    }

    private void switchView(String viewName) {
        currentView = viewName;

        for (Map.Entry<String, JButton> entry : sidebarButtons.entrySet()) {
            if (entry.getKey().equals(viewName)) {
                entry.getValue().setBackground(new Color(42, 63, 84));
                entry.getValue().setFont(new Font("Arial", Font.BOLD, 14));
            } else {
                entry.getValue().setBackground(new Color(25, 39, 52));
                entry.getValue().setFont(new Font("Arial", Font.PLAIN, 14));
            }
        }

        CardLayout cl = (CardLayout) contentPanel.getLayout();
        cl.show(contentPanel, viewName);
    }

    private JPanel createCard(String heading, String value, String subtitle, Color accent) {

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);

        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 6, 0, 0, accent),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 235, 240), 1),
                BorderFactory.createEmptyBorder(22, 22, 22, 22)
            )
        ));

        JLabel title = new JLabel(heading);
        title.setFont(new Font("Arial", Font.BOLD, 15));
        title.setForeground(new Color(90, 100, 115));

        JLabel number = new JLabel(value);
        number.setFont(new Font("Arial", Font.BOLD, 36));
        number.setForeground(new Color(30, 40, 55));

        JLabel desc = new JLabel(subtitle);
        desc.setFont(new Font("Arial", Font.PLAIN, 12));
        desc.setForeground(new Color(130, 140, 155));

        card.add(title);
        card.add(Box.createVerticalStrut(10));
        card.add(number);
        card.add(Box.createVerticalStrut(8));
        card.add(desc);

        return card;
    }

    private void refreshDashboardCards() {

        cardsPanel.removeAll();

        MedicineDAO medicineDAO = new MedicineDAO();
        BatchDAO batchDAO = new BatchDAO();
        ExpiryService expiryService = new ExpiryService();
        StockAlertService stockAlertService = new StockAlertService();

        int totalMedicines = medicineDAO.getTotalMedicines();
        int totalBatches = batchDAO.getTotalBatches();

        int expiringSoon = 0;
        try {
            expiringSoon = expiryService.getExpiringSoonBatches(45).size();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        int lowStockItems =
            stockAlertService.getLowStockMedicines().size();

        cardsPanel.add(createCard(
            "Total Medicines",
            String.valueOf(totalMedicines),
            "Registered medicine catalog entries",
            new Color(52, 120, 246)
        ));

        cardsPanel.add(createCard(
            "Total Batches",
            String.valueOf(totalBatches),
            "Inventory batches in system",
            new Color(20, 160, 120)
        ));

        cardsPanel.add(createCard(
            "Expiring Soon",
            String.valueOf(expiringSoon),
            "Batches expiring within 45 days",
            new Color(230, 155, 40)
        ));

        cardsPanel.add(createCard(
            "Low Stock Items",
            String.valueOf(lowStockItems),
            "Medicines at or below reorder level",
            new Color(220, 75, 75)
        ));

        cardsPanel.revalidate();
        cardsPanel.repaint();
    }
}
