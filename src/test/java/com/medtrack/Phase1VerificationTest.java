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

import com.medtrack.config.DBConnection;
import com.medtrack.dao.BatchDAO;
import com.medtrack.dao.MedicineDAO;
import com.medtrack.model.Batch;
import com.medtrack.model.Medicine;
import com.medtrack.service.FEFOService;

public class Phase1VerificationTest {

    private static final String TEST_URL = "jdbc:mariadb://localhost:3306/medtrack_test";

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  STARTING PHASE 1 CRITICAL FUNCTIONALITY TESTS  ");
        System.out.println("=================================================");

        DBConnection.setCustomUrl(TEST_URL);

        try {
            resetTestData();

            testQuantityValidation();
            testInsufficientStockScenario();
            testExpiredStockScenarios();
            testFEFOOrdering();
            testTransactionRollbackSafety();

            System.out.println("\n>>> ALL PHASE 1 TESTS PASSED SUCCESSFULLY! <<<");

        } catch (Throwable t) {
            System.err.println("\nTEST FAILED!");
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

    private static int insertMedicine(String name, int reorderLevel) throws SQLException {
        MedicineDAO dao = new MedicineDAO();
        Medicine m = new Medicine(0, name, name + " Gen", "General", "Pharma Corp", "Box", reorderLevel);
        dao.addMedicine(m);

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT medicine_id FROM medicines WHERE medicine_name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to find inserted medicine: " + name);
    }

    private static int insertBatch(int medicineId, String batchNo, LocalDate mfg, LocalDate exp, int qty, int availQty) throws SQLException {
        String sql = """
            INSERT INTO batches (medicine_id, batch_number, manufacturing_date, expiry_date,
                                purchase_price, selling_price, quantity_received, available_quantity)
            VALUES (?, ?, ?, ?, 10.00, 15.00, ?, ?)
        """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, medicineId);
            ps.setString(2, batchNo);
            ps.setDate(3, mfg != null ? Date.valueOf(mfg) : null);
            ps.setDate(4, Date.valueOf(exp));
            ps.setInt(5, qty);
            ps.setInt(6, availQty);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to insert batch: " + batchNo);
    }

    private static void testQuantityValidation() {
        System.out.println("\n[Test 1] Validating Requested Stock Quantity...");
        FEFOService service = new FEFOService();

        // Zero quantity
        try {
            service.allocateStock(1, 0);
            throw new AssertionError("Zero quantity was NOT rejected!");
        } catch (IllegalArgumentException e) {
            System.out.println("  PASS: Zero quantity rejected with: " + e.getMessage());
        } catch (Exception e) {
            throw new AssertionError("Expected IllegalArgumentException for zero quantity, got: " + e);
        }

        // Negative quantity
        try {
            service.allocateStock(1, -10);
            throw new AssertionError("Negative quantity was NOT rejected!");
        } catch (IllegalArgumentException e) {
            System.out.println("  PASS: Negative quantity rejected with: " + e.getMessage());
        } catch (Exception e) {
            throw new AssertionError("Expected IllegalArgumentException for negative quantity, got: " + e);
        }
    }

    private static void testInsufficientStockScenario() throws SQLException {
        System.out.println("\n[Test 2] Testing Insufficient-Stock Scenario...");
        resetTestData();

        int medId = insertMedicine("Test Med A", 10);
        LocalDate futureExp = LocalDate.now().plusMonths(6);
        insertBatch(medId, "B001", LocalDate.now().minusMonths(1), futureExp, 50, 50);

        FEFOService service = new FEFOService();

        // Attempt to allocate 60 when only 50 available
        boolean rejected = false;
        try {
            service.allocateStock(medId, 60);
        } catch (SQLException e) {
            rejected = true;
            System.out.println("  PASS: Allocation safely rejected with: " + e.getMessage());
        }

        if (!rejected) {
            throw new AssertionError("Allocation was NOT rejected when requesting more than available stock!");
        }

        // Verify stock remains exactly 50 (NO partial allocation)
        BatchDAO batchDAO = new BatchDAO();
        int available = batchDAO.getAvailableStock(medId);
        if (available != 50) {
            throw new AssertionError("Stock quantity changed after failed allocation! Expected 50, found: " + available);
        }
        System.out.println("  PASS: Available stock remains unchanged at 50.");

        // Verify NO sale transaction was recorded
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM stock_transactions WHERE transaction_type = 'SALE'")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int saleCount = rs.getInt(1);
                if (saleCount != 0) {
                    throw new AssertionError("Incorrect SALE transaction was recorded! Count: " + saleCount);
                }
            }
        }
        System.out.println("  PASS: No incorrect SALE transaction recorded.");

        // Verify NO stock_allocation record was saved
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM stock_allocations")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int allocCount = rs.getInt(1);
                if (allocCount != 0) {
                    throw new AssertionError("Partial stock_allocation records were saved! Count: " + allocCount);
                }
            }
        }
        System.out.println("  PASS: No partial stock_allocation records created.");
    }

    private static void testExpiredStockScenarios() throws SQLException {
        System.out.println("\n[Test 3] Testing Expired-Stock Scenarios...");
        resetTestData();

        int medId = insertMedicine("Test Med B", 10);
        LocalDate pastExp = LocalDate.now().minusDays(10);
        // Insert expired batch with stock
        insertBatch(medId, "EXP001", LocalDate.now().minusMonths(6), pastExp, 100, 100);

        FEFOService service = new FEFOService();

        // Try to allocate from expired stock
        boolean rejected = false;
        try {
            service.allocateStock(medId, 10);
        } catch (SQLException e) {
            rejected = true;
            System.out.println("  PASS: Expired-only allocation rejected with message: " + e.getMessage());
        }

        if (!rejected) {
            throw new AssertionError("Expired batch was incorrectly selected for allocation!");
        }

        // Verify available stock reports 0
        BatchDAO batchDAO = new BatchDAO();
        int available = batchDAO.getAvailableStock(medId);
        if (available != 0) {
            throw new AssertionError("Expired batch was counted in available non-expired stock! Stock: " + available);
        }
        System.out.println("  PASS: Available non-expired stock correctly reports 0 for expired batch.");

        // Add an eligible batch alongside expired batch
        LocalDate futureExp = LocalDate.now().plusMonths(3);
        int eligibleBatchId = insertBatch(medId, "VALID001", LocalDate.now().minusMonths(1), futureExp, 30, 30);

        // Allocate 20 - must take ONLY from eligible batch
        service.allocateStock(medId, 20);

        // Verify expired batch stock is untouched (still 100)
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT available_quantity FROM batches WHERE batch_number = 'EXP001'")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int expQty = rs.getInt(1);
                if (expQty != 100) {
                    throw new AssertionError("Expired batch was deducted! Quantity: " + expQty);
                }
            }
        }
        System.out.println("  PASS: Expired batch was completely untouched (remains 100).");

        // Verify eligible batch was deducted by 20 -> remaining 10
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT available_quantity FROM batches WHERE batch_id = ?")) {
            ps.setInt(1, eligibleBatchId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int valQty = rs.getInt(1);
                if (valQty != 10) {
                    throw new AssertionError("Eligible batch not deducted properly! Expected 10, found: " + valQty);
                }
            }
        }
        System.out.println("  PASS: Eligible batch deducted properly (10 remaining).");
    }

    private static void testFEFOOrdering() throws SQLException {
        System.out.println("\n[Test 4] Testing FEFO Ordering (Earliest Expiry First)...");
        resetTestData();

        int medId = insertMedicine("Test Med C", 10);

        // Batch 1: Expiring in 2 months (earlier) - 30 units
        LocalDate exp1 = LocalDate.now().plusMonths(2);
        int b1 = insertBatch(medId, "EARLY_EXP", LocalDate.now().minusMonths(2), exp1, 30, 30);

        // Batch 2: Expiring in 6 months (later) - 50 units
        LocalDate exp2 = LocalDate.now().plusMonths(6);
        int b2 = insertBatch(medId, "LATER_EXP", LocalDate.now().minusMonths(1), exp2, 50, 50);

        FEFOService service = new FEFOService();

        // Allocate 40 units: should deplete all 30 from EARLY_EXP and 10 from LATER_EXP
        service.allocateStock(medId, 40);

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT available_quantity FROM batches WHERE batch_id = ?")) {
            
            // Check Early batch
            ps.setInt(1, b1);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int q1 = rs.getInt(1);
                if (q1 != 0) {
                    throw new AssertionError("Earlier expiring batch was not depleted first! Remaining: " + q1);
                }
            }

            // Check Later batch
            ps.setInt(1, b2);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int q2 = rs.getInt(1);
                if (q2 != 40) {
                    throw new AssertionError("Later expiring batch does not have expected stock! Expected 40, found: " + q2);
                }
            }
        }
        System.out.println("  PASS: FEFO selected earliest batch first, then partially allocated from next batch.");
    }

    private static void testTransactionRollbackSafety() throws SQLException {
        System.out.println("\n[Test 5] Testing Database Constraints and Transaction Safety...");
        resetTestData();

        int medId = insertMedicine("Test Med D", 10);
        LocalDate futureExp = LocalDate.now().plusMonths(4);
        int bId = insertBatch(medId, "TXN_TEST", LocalDate.now().minusMonths(1), futureExp, 20, 20);

        // Attempt allocation of 25 (exceeds available 20)
        FEFOService service = new FEFOService();
        try {
            service.allocateStock(medId, 25);
            throw new AssertionError("Allocation should have failed!");
        } catch (SQLException e) {
            System.out.println("  PASS: Caught expected exception: " + e.getMessage());
        }

        // Verify available quantity in batches is STILL 20
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT available_quantity FROM batches WHERE batch_id = ?")) {
            ps.setInt(1, bId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int qty = rs.getInt(1);
                if (qty != 20) {
                    throw new AssertionError("Batch quantity was modified during failed transaction! Qty: " + qty);
                }
            }
        }
        System.out.println("  PASS: Batch quantity remained untouched after transaction rollback.");

        // Verify CHECK constraint in database prevents negative available_quantity
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE batches SET available_quantity = -5 WHERE batch_id = ?")) {
            ps.setInt(1, bId);
            try {
                ps.executeUpdate();
                throw new AssertionError("Database CHECK constraint failed to prevent negative quantity!");
            } catch (SQLException e) {
                System.out.println("  PASS: Database CHECK constraint successfully rejected negative available_quantity: " + e.getMessage());
            }
        }
    }
}

