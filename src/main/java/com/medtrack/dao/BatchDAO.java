package com.medtrack.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.medtrack.config.DBConnection;
import com.medtrack.model.Batch;

public class BatchDAO {

    // Get batches belonging to a particular medicine
    public List<Batch> getBatchesByMedicine(int medicineId) {

        List<Batch> batches = new ArrayList<>();

        String sql = "SELECT * FROM batches WHERE medicine_id = ? "
                   + "ORDER BY expiry_date ASC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, medicineId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {

                    Batch batch = new Batch();

                    batch.setBatchId(resultSet.getInt("batch_id"));
                    batch.setMedicineId(resultSet.getInt("medicine_id"));
                    batch.setBatchNumber(resultSet.getString("batch_number"));

                    if (resultSet.getDate("manufacturing_date") != null) {
                        batch.setManufacturingDate(
                            resultSet.getDate("manufacturing_date").toLocalDate()
                        );
                    }

                    batch.setExpiryDate(
                        resultSet.getDate("expiry_date").toLocalDate()
                    );

                    batch.setPurchasePrice(resultSet.getBigDecimal("purchase_price"));
                    batch.setSellingPrice(resultSet.getBigDecimal("selling_price"));
                    batch.setQuantityReceived(resultSet.getInt("quantity_received"));
                    batch.setAvailableQuantity(resultSet.getInt("available_quantity"));

                    batches.add(batch);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return batches;
    }

    // Get total number of batches
    public int getTotalBatches() {

        String sql = "SELECT COUNT(*) FROM batches";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    // Get all batch details for the table
    public List<Object[]> getAllBatchDetails() {

        List<Object[]> batches = new ArrayList<>();

        String sql = """
            SELECT
                b.batch_id,
                b.batch_number,
                m.medicine_name,
                b.manufacturing_date,
                b.expiry_date,
                b.purchase_price,
                b.selling_price,
                b.quantity_received,
                b.available_quantity
            FROM batches b
            JOIN medicines m
                ON b.medicine_id = m.medicine_id
            ORDER BY b.expiry_date ASC
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                Object[] row = {
                    rs.getInt("batch_id"),
                    rs.getString("batch_number"),
                    rs.getString("medicine_name"),
                    rs.getDate("manufacturing_date"),
                    rs.getDate("expiry_date"),
                    rs.getBigDecimal("purchase_price"),
                    rs.getBigDecimal("selling_price"),
                    rs.getInt("quantity_received"),
                    rs.getInt("available_quantity")
                };

                batches.add(row);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return batches;
    }

    // Add a new batch and record its initial purchase transaction
    public boolean addBatch(Batch batch) throws SQLException {

        String batchSql = """
            INSERT INTO batches (
                medicine_id,
                batch_number,
                manufacturing_date,
                expiry_date,
                purchase_price,
                selling_price,
                quantity_received,
                available_quantity
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        String transactionSql = """
            INSERT INTO stock_transactions (
                batch_id,
                transaction_type,
                quantity,
                reference_number,
                notes
            )
            VALUES (?, 'PURCHASE', ?, ?, ?)
        """;

        try (Connection con = DBConnection.getConnection()) {

            con.setAutoCommit(false);

            try {

                int batchId;

                try (PreparedStatement ps = con.prepareStatement(
                        batchSql, Statement.RETURN_GENERATED_KEYS)) {

                    ps.setInt(1, batch.getMedicineId());
                    ps.setString(2, batch.getBatchNumber());

                    if (batch.getManufacturingDate() == null) {
                        ps.setNull(3, java.sql.Types.DATE);
                    } else {
                        ps.setDate(3, Date.valueOf(batch.getManufacturingDate()));
                    }

                    ps.setDate(4, Date.valueOf(batch.getExpiryDate()));
                    ps.setBigDecimal(5, batch.getPurchasePrice());
                    ps.setBigDecimal(6, batch.getSellingPrice());
                    ps.setInt(7, batch.getQuantityReceived());
                    ps.setInt(8, batch.getQuantityReceived());

                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {

                        if (!keys.next()) {
                            throw new SQLException("Batch ID was not generated.");
                        }

                        batchId = keys.getInt(1);
                    }
                }

                // Record the initial stock purchase
                try (PreparedStatement ps = con.prepareStatement(transactionSql)) {

                    ps.setInt(1, batchId);
                    ps.setInt(2, batch.getQuantityReceived());
                    ps.setString(3, batch.getBatchNumber());
                    ps.setString(4, "Initial stock received");

                    ps.executeUpdate();
                }

                con.commit();
                return true;

            } catch (SQLException e) {

                try {
                    con.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("Rollback failed: " + rollbackEx.getMessage());
                }
                throw e;

            } finally {

                try {
                    con.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    // Get total available non-expired stock for a medicine
    public int getAvailableStock(int medicineId) {

        String sql = """
            SELECT COALESCE(SUM(available_quantity), 0)
            FROM batches
            WHERE medicine_id = ?
            AND available_quantity > 0
            AND expiry_date >= CURDATE()
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, medicineId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    // Check if a batch number already exists for a medicine
    public boolean isBatchNumberExists(int medicineId, String batchNumber) {
        String sql = "SELECT COUNT(*) FROM batches WHERE medicine_id = ? AND LOWER(TRIM(batch_number)) = LOWER(TRIM(?))";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, medicineId);
            ps.setString(2, batchNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
