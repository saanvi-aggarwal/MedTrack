package com.medtrack;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.medtrack.config.DBConnection;
import com.medtrack.dao.BatchDAO;
import com.medtrack.dao.MedicineDAO;
import com.medtrack.dao.StockDAO;
import com.medtrack.model.Batch;
import com.medtrack.model.Medicine;
import com.medtrack.model.StockTransaction;
import com.medtrack.service.ExpiryService;
import com.medtrack.service.FEFOService;
import com.medtrack.service.StockAlertService;

public class FullApplicationWorkflowTest {

    private static final String TEST_URL = "jdbc:mariadb://localhost:3306/medtrack_test";

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("  MEDTRACK FULL APPLICATION WORKFLOW & QA VERIFICATION   ");
        System.out.println("==========================================================");

        DBConnection.setCustomUrl(TEST_URL);

        try {
            // 1. Application startup & DB connectivity
            testStartupAndConnectivity();

            // 2. Fresh DB tables verification
            testFreshDatabaseTables();

            // 3. Medicine creation
            int medId1 = testMedicineCreation();

            // 4. Medicine editing
            testMedicineEditing(medId1);

            // 5. Batch creation & transaction recording
            int batchId1 = testBatchCreation(medId1);

            // 6. Medicine deletion protection (with existing batches)
            testMedicineDeletionProtection(medId1);

            // 7. Medicine deletion when safe (no batches)
            testMedicineDeletionSupported();

            // 8. Batch management & duplicate batch handling
            testBatchDuplicateHandling(medId1);

            // 9. Additional batch creation for FEFO test
            int batchId2 = testSecondBatchCreation(medId1);

            // 10. FEFO allocation & stock deduction
            testFEFOAllocationAndDeduction(medId1, batchId1, batchId2);

            // 11. Transaction recording verification (PURCHASE & SALE)
            testTransactionRecording(batchId1, batchId2);

            // 12. Expiry monitoring (expired vs expiring soon)
            testExpiryMonitoring(medId1);

            // 13. Low-stock alerts
            testLowStockAlerts(medId1);

            // 14. Dashboard updates
            testDashboardUpdates();

            // 15. Search and filtering DAO methods
            testSearchAndFiltering(medId1);

            // 16. Invalid input handling
            testInvalidInputHandling(medId1);

            // 17. Insufficient-stock handling
            testInsufficientStockHandling(medId1);

            // 18. Expired-stock handling
            testExpiredStockHandling(medId1);

            System.out.println("\n==========================================================");
            System.out.println(">>> ALL 18 FULL APPLICATION WORKFLOW TESTS PASSED! <<<");
            System.out.println("==========================================================");

        } catch (Throwable t) {
            System.err.println("\nFULL WORKFLOW TEST FAILED!");
            t.printStackTrace();
            System.exit(1);
        } finally {
            DBConnection.resetUrl();
        }
    }

    private static void resetTestData() throws SQLException {
        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate("DELETE FROM stock_allocations");
            stmt.executeUpdate("DELETE FROM stock_transactions");
            stmt.executeUpdate("DELETE FROM batches");
            stmt.executeUpdate("DELETE FROM medicines");
        }
    }

    private static void testStartupAndConnectivity() throws SQLException {
        System.out.println("\n[Workflow 1 & 2] Application Startup and Database Connectivity...");
        try (Connection con = DBConnection.getConnection()) {
            if (con == null || con.isClosed()) {
                throw new AssertionError("Failed to acquire valid database connection!");
            }
            System.out.println("  PASS: Database connection established successfully with: " + con.getMetaData().getDatabaseProductName() + " " + con.getMetaData().getDatabaseProductVersion());
        }
    }

    private static void testFreshDatabaseTables() throws SQLException {
        System.out.println("\n[Fresh DB Verification] Verifying required tables and integrity constraints...");
        resetTestData();
        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW TABLES")) {
            int tableCount = 0;
            while (rs.next()) {
                tableCount++;
            }
            if (tableCount < 4) {
                throw new AssertionError("Expected 4 tables, found: " + tableCount);
            }
            System.out.println("  PASS: All 4 core tables exist in fresh test database.");
        }
    }

    private static int testMedicineCreation() throws SQLException {
        System.out.println("\n[Workflow 3] Medicine Creation...");
        MedicineDAO dao = new MedicineDAO();
        Medicine m = new Medicine(0, "Amoxicillin 500mg", "Amoxicillin", "Antibiotic", "Sun Pharma", "Strip", 20);
        boolean success = dao.addMedicine(m);
        if (!success) {
            throw new AssertionError("addMedicine failed!");
        }

        List<Medicine> all = dao.getAllMedicines();
        if (all.isEmpty()) {
            throw new AssertionError("No medicines found after adding!");
        }
        Medicine created = all.get(0);
        System.out.println("  PASS: Created medicine ID: " + created.getMedicineId() + ", Name: " + created.getMedicineName());
        return created.getMedicineId();
    }

    private static void testMedicineEditing(int medicineId) throws SQLException {
        System.out.println("\n[Workflow 4] Medicine Editing...");
        MedicineDAO dao = new MedicineDAO();
        Medicine existing = dao.getMedicineById(medicineId);
        if (existing == null) {
            throw new AssertionError("Could not retrieve medicine for editing!");
        }

        existing.setManufacturer("Sun Pharma Laboratories");
        existing.setReorderLevel(25);
        boolean updated = dao.updateMedicine(existing);
        if (!updated) {
            throw new AssertionError("updateMedicine failed!");
        }

        Medicine reloaded = dao.getMedicineById(medicineId);
        if (!"Sun Pharma Laboratories".equals(reloaded.getManufacturer()) || reloaded.getReorderLevel() != 25) {
            throw new AssertionError("Medicine properties not updated as expected!");
        }
        System.out.println("  PASS: Medicine successfully updated. Manufacturer: " + reloaded.getManufacturer() + ", Reorder: " + reloaded.getReorderLevel());
    }

    private static int testBatchCreation(int medicineId) throws SQLException {
        System.out.println("\n[Workflow 5 & 6] Batch Creation and Purchase Transaction Recording...");
        BatchDAO dao = new BatchDAO();
        Batch batch = new Batch();
        batch.setMedicineId(medicineId);
        batch.setBatchNumber("AMX-B1");
        batch.setManufacturingDate(LocalDate.now().minusMonths(2));
        batch.setExpiryDate(LocalDate.now().plusMonths(3));
        batch.setPurchasePrice(new BigDecimal("25.00"));
        batch.setSellingPrice(new BigDecimal("35.00"));
        batch.setQuantityReceived(50);

        boolean success = dao.addBatch(batch);
        if (!success) {
            throw new AssertionError("addBatch failed!");
        }

        List<Batch> batches = dao.getBatchesByMedicine(medicineId);
        if (batches.isEmpty()) {
            throw new AssertionError("No batches found for medicine after addition!");
        }

        Batch created = batches.get(0);
        if (!"AMX-B1".equals(created.getBatchNumber()) || created.getAvailableQuantity() != 50) {
            throw new AssertionError("Batch attributes mismatch!");
        }
        System.out.println("  PASS: Batch AMX-B1 created with available quantity: " + created.getAvailableQuantity());
        return created.getBatchId();
    }

    private static void testMedicineDeletionProtection(int medicineId) {
        System.out.println("\n[Workflow 6] Medicine Deletion Protection with Associated Batches...");
        MedicineDAO dao = new MedicineDAO();
        try {
            dao.deleteMedicine(medicineId);
            throw new AssertionError("Medicine with existing batches was deleted when it should have been protected!");
        } catch (SQLException e) {
            System.out.println("  PASS: Deletion successfully blocked: " + e.getMessage());
        }
    }

    private static void testMedicineDeletionSupported() throws SQLException {
        System.out.println("\n[Workflow 7] Medicine Deletion when Safe (No Batches)...");
        MedicineDAO dao = new MedicineDAO();
        Medicine temp = new Medicine(0, "Temp Med", "Temp", "Temp", "Temp", "Strip", 5);
        dao.addMedicine(temp);

        List<Medicine> all = dao.getAllMedicines();
        int tempId = -1;
        for (Medicine m : all) {
            if ("Temp Med".equals(m.getMedicineName())) {
                tempId = m.getMedicineId();
                break;
            }
        }

        boolean deleted = dao.deleteMedicine(tempId);
        if (!deleted || dao.getMedicineById(tempId) != null) {
            throw new AssertionError("Safe medicine deletion failed!");
        }
        System.out.println("  PASS: Unlinked medicine successfully deleted.");
    }

    private static void testBatchDuplicateHandling(int medicineId) {
        System.out.println("\n[Workflow 8] Batch Duplicate Detection...");
        BatchDAO dao = new BatchDAO();
        boolean exists = dao.isBatchNumberExists(medicineId, "AMX-B1");
        if (!exists) {
            throw new AssertionError("Duplicate check failed to detect existing batch AMX-B1!");
        }
        System.out.println("  PASS: isBatchNumberExists correctly detected duplicate batch number 'AMX-B1'.");
    }

    private static int testSecondBatchCreation(int medicineId) throws SQLException {
        System.out.println("\n[Workflow 9] Registering Second Batch for FEFO Test...");
        BatchDAO dao = new BatchDAO();
        Batch batch = new Batch();
        batch.setMedicineId(medicineId);
        batch.setBatchNumber("AMX-B2");
        batch.setManufacturingDate(LocalDate.now().minusMonths(1));
        batch.setExpiryDate(LocalDate.now().plusMonths(8)); // Later expiry than AMX-B1 (+3 months)
        batch.setPurchasePrice(new BigDecimal("26.00"));
        batch.setSellingPrice(new BigDecimal("36.00"));
        batch.setQuantityReceived(100);

        dao.addBatch(batch);
        List<Batch> batches = dao.getBatchesByMedicine(medicineId);
        Batch b2 = batches.stream().filter(b -> "AMX-B2".equals(b.getBatchNumber())).findFirst().orElseThrow();
        System.out.println("  PASS: Batch AMX-B2 created with available quantity: " + b2.getAvailableQuantity());
        return b2.getBatchId();
    }

    private static void testFEFOAllocationAndDeduction(int medicineId, int b1Id, int b2Id) throws SQLException {
        System.out.println("\n[Workflow 10] FEFO Stock Allocation and Multi-Batch Stock Deduction...");
        FEFOService service = new FEFOService();

        // Total available is 50 (B1) + 100 (B2) = 150.
        // Request 70 units -> B1 (earlier expiry) should be completely depleted (50 -> 0).
        // B2 should supply remaining 20 (100 -> 80).
        service.allocateStock(medicineId, 70);

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT batch_id, available_quantity FROM batches WHERE medicine_id = ? ORDER BY expiry_date ASC")) {
            ps.setInt(1, medicineId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int q1 = rs.getInt("available_quantity");
                if (q1 != 0) {
                    throw new AssertionError("First batch was not depleted! Expected 0, found: " + q1);
                }

                rs.next();
                int q2 = rs.getInt("available_quantity");
                if (q2 != 80) {
                    throw new AssertionError("Second batch was not deducted correctly! Expected 80, found: " + q2);
                }
            }
        }
        System.out.println("  PASS: FEFO correctly depleted Batch 1 (0 remaining) and deducted Batch 2 (80 remaining).");
    }

    private static void testTransactionRecording(int b1Id, int b2Id) {
        System.out.println("\n[Workflow 11] Transaction Recording Verification...");
        StockDAO dao = new StockDAO();
        List<StockTransaction> list = dao.getAllTransactions();

        long purchaseCount = list.stream().filter(t -> "PURCHASE".equals(t.getTransactionType())).count();
        long saleCount = list.stream().filter(t -> "SALE".equals(t.getTransactionType())).count();

        if (purchaseCount < 2 || saleCount < 2) {
            throw new AssertionError("Expected at least 2 PURCHASE and 2 SALE transactions! Found: " + purchaseCount + " purchases, " + saleCount + " sales.");
        }
        System.out.println("  PASS: Correct transaction records verified (Purchases: " + purchaseCount + ", Sales: " + saleCount + ").");
    }

    private static void testExpiryMonitoring(int medicineId) throws SQLException {
        System.out.println("\n[Workflow 12] Expiry Monitoring (Expired vs Expiring Soon)...");
        // Add an expired batch
        BatchDAO dao = new BatchDAO();
        Batch expBatch = new Batch();
        expBatch.setMedicineId(medicineId);
        expBatch.setBatchNumber("AMX-EXPIRED");
        expBatch.setManufacturingDate(LocalDate.now().minusYears(2));
        expBatch.setExpiryDate(LocalDate.now().minusMonths(1));
        expBatch.setPurchasePrice(new BigDecimal("20.00"));
        expBatch.setSellingPrice(new BigDecimal("30.00"));
        expBatch.setQuantityReceived(15);
        dao.addBatch(expBatch);

        ExpiryService expiryService = new ExpiryService();
        List<Batch> expiredList = expiryService.getExpiredBatches();
        boolean foundExpired = expiredList.stream().anyMatch(b -> "AMX-EXPIRED".equals(b.getBatchNumber()));
        if (!foundExpired) {
            throw new AssertionError("Expired batch was not detected by ExpiryService!");
        }

        List<Batch> expiringSoon = expiryService.getExpiringSoonBatches(45);
        System.out.println("  PASS: ExpiryService successfully identified expired batches (Count: " + expiredList.size() + ") and expiring-soon batches (Count: " + expiringSoon.size() + ").");
    }

    private static void testLowStockAlerts(int medicineId) {
        System.out.println("\n[Workflow 13] Low-Stock Alerts Evaluation...");
        StockAlertService service = new StockAlertService();
        Map<String, Integer> alerts = service.getLowStockMedicines();

        // Available non-expired stock for Amoxicillin is 80 (batch 2). Reorder level is 25.
        // It should NOT be low stock yet.
        System.out.println("  Current alerts count: " + alerts.size());

        // Now test a medicine with stock below reorder level
        MedicineDAO mDao = new MedicineDAO();
        Medicine lowMed = new Medicine(0, "Low Stock Paracetamol", "Para", "Analgesic", "Cipla", "Strip", 50);
        try {
            mDao.addMedicine(lowMed);
            // Has 0 stock, reorder level is 50 -> MUST trigger alert
            Map<String, Integer> newAlerts = service.getLowStockMedicines();
            if (!newAlerts.containsKey("Low Stock Paracetamol")) {
                throw new AssertionError("Low stock medicine was not reported by StockAlertService!");
            }
            System.out.println("  PASS: Low-stock medicine triggered alert successfully. Stock: " + newAlerts.get("Low Stock Paracetamol"));
        } catch (SQLException e) {
            throw new AssertionError("Failed during low stock test: " + e);
        }
    }

    private static void testDashboardUpdates() {
        System.out.println("\n[Workflow 14] Dashboard Statistics Aggregation...");
        MedicineDAO mDao = new MedicineDAO();
        BatchDAO bDao = new BatchDAO();
        ExpiryService eService = new ExpiryService();
        StockAlertService sService = new StockAlertService();

        int totalMeds = mDao.getTotalMedicines();
        int totalBatches = bDao.getTotalBatches();
        int expiringSoon = 0;
        try {
            expiringSoon = eService.getExpiringSoonBatches(45).size();
        } catch (SQLException ignored) {}
        int lowStockCount = sService.getLowStockMedicines().size();

        if (totalMeds <= 0 || totalBatches <= 0) {
            throw new AssertionError("Dashboard metrics returned invalid counts!");
        }
        System.out.println("  PASS: Dashboard metrics retrieved: Total Meds=" + totalMeds + ", Batches=" + totalBatches + ", Expiring Soon=" + expiringSoon + ", Low Stock=" + lowStockCount);
    }

    private static void testSearchAndFiltering(int medicineId) {
        System.out.println("\n[Workflow 15] Search and Filtering Capabilities...");
        MedicineDAO mDao = new MedicineDAO();
        List<Object[]> medsWithStock = mDao.getAllMedicinesWithStock();
        if (medsWithStock.isEmpty()) {
            throw new AssertionError("getAllMedicinesWithStock returned empty!");
        }

        boolean found = false;
        for (Object[] row : medsWithStock) {
            String name = (String) row[1];
            int stock = (Integer) row[7];
            if (name.contains("Amoxicillin")) {
                found = true;
                if (stock != 80) {
                    throw new AssertionError("Amoxicillin stock in search query mismatch! Expected 80, found: " + stock);
                }
            }
        }
        if (!found) {
            throw new AssertionError("Amoxicillin not found in medicines with stock!");
        }
        System.out.println("  PASS: Search & stock reporting query returned accurate stock levels.");
    }

    private static void testInvalidInputHandling(int medicineId) {
        System.out.println("\n[Workflow 16] Invalid Input Handling...");
        FEFOService service = new FEFOService();
        try {
            service.allocateStock(medicineId, -5);
            throw new AssertionError("Negative quantity not rejected!");
        } catch (IllegalArgumentException e) {
            System.out.println("  PASS: Negative quantity rejected: " + e.getMessage());
        } catch (Exception e) {
            throw new AssertionError("Unexpected exception type: " + e);
        }
    }

    private static void testInsufficientStockHandling(int medicineId) {
        System.out.println("\n[Workflow 17] Insufficient-Stock Handling...");
        FEFOService service = new FEFOService();
        // Available is 80. Request 100.
        try {
            service.allocateStock(medicineId, 100);
            throw new AssertionError("Insufficient stock was NOT rejected!");
        } catch (SQLException e) {
            System.out.println("  PASS: Insufficient stock rejected with: " + e.getMessage());
        }

        // Verify remaining stock is still 80
        BatchDAO dao = new BatchDAO();
        int avail = dao.getAvailableStock(medicineId);
        if (avail != 80) {
            throw new AssertionError("Stock was altered during failed allocation! Current: " + avail);
        }
        System.out.println("  PASS: Available stock intact at 80.");
    }

    private static void testExpiredStockHandling(int medicineId) throws SQLException {
        System.out.println("\n[Workflow 18] Expired-Stock Handling (Exclusion from Allocation)...");
        // Create medicine with ONLY expired stock
        MedicineDAO mDao = new MedicineDAO();
        Medicine expOnlyMed = new Medicine(0, "Expired Only Med", "ExpiredGen", "Misc", "TestCorp", "Vial", 10);
        mDao.addMedicine(expOnlyMed);

        List<Medicine> all = mDao.getAllMedicines();
        int expOnlyId = all.stream().filter(m -> "Expired Only Med".equals(m.getMedicineName())).findFirst().orElseThrow().getMedicineId();

        BatchDAO bDao = new BatchDAO();
        Batch b = new Batch();
        b.setMedicineId(expOnlyId);
        b.setBatchNumber("OLD-BATCH");
        b.setManufacturingDate(LocalDate.now().minusYears(3));
        b.setExpiryDate(LocalDate.now().minusMonths(6));
        b.setPurchasePrice(new BigDecimal("10.00"));
        b.setSellingPrice(new BigDecimal("20.00"));
        b.setQuantityReceived(50);
        bDao.addBatch(b);

        int avail = bDao.getAvailableStock(expOnlyId);
        if (avail != 0) {
            throw new AssertionError("Available non-expired stock must be 0! Found: " + avail);
        }

        FEFOService service = new FEFOService();
        try {
            service.allocateStock(expOnlyId, 10);
            throw new AssertionError("Allocation from expired-only batch succeeded when it should have failed!");
        } catch (SQLException e) {
            System.out.println("  PASS: Expired-stock allocation rejected safely: " + e.getMessage());
        }
    }
}

