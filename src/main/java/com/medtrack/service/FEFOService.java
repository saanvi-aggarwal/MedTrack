package com.medtrack.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.medtrack.config.DBConnection;

public class FEFOService {

    public void allocateStock(int medicineId, int requestedQuantity)
            throws SQLException {

        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException(
                "Requested quantity must be greater than zero.");
        }

        String selectSQL =
            "SELECT batch_id, available_quantity FROM batches " +
            "WHERE medicine_id = ? " +
            "AND available_quantity > 0 " +
            "AND expiry_date >= CURDATE() " +
            "ORDER BY expiry_date ASC FOR UPDATE";

        String updateSQL =
            "UPDATE batches SET available_quantity = available_quantity - ? " +
            "WHERE batch_id = ?";

        String allocationSQL =
            "INSERT INTO stock_allocations " +
            "(medicine_id, batch_id, quantity_allocated) VALUES (?, ?, ?)";

        String transactionSQL =
            "INSERT INTO stock_transactions " +
            "(batch_id, transaction_type, quantity, notes) " +
            "VALUES (?, 'SALE', ?, 'FEFO stock allocation')";

        try (Connection connection = DBConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {

                int remainingQuantity = requestedQuantity;
                int batchesFound = 0;

                try (PreparedStatement selectStatement =
                        connection.prepareStatement(selectSQL)) {

                    selectStatement.setInt(1, medicineId);

                    try (ResultSet rs = selectStatement.executeQuery()) {

                        while (rs.next() && remainingQuantity > 0) {
                            batchesFound++;

                            int batchId = rs.getInt("batch_id");
                            int availableQuantity =
                                rs.getInt("available_quantity");

                            int allocatedQuantity = Math.min(
                                remainingQuantity, availableQuantity);

                            try (PreparedStatement updateStatement =
                                    connection.prepareStatement(updateSQL)) {

                                updateStatement.setInt(1, allocatedQuantity);
                                updateStatement.setInt(2, batchId);
                                updateStatement.executeUpdate();
                            }

                            try (PreparedStatement allocationStatement =
                                    connection.prepareStatement(allocationSQL)) {

                                allocationStatement.setInt(1, medicineId);
                                allocationStatement.setInt(2, batchId);
                                allocationStatement.setInt(3, allocatedQuantity);
                                allocationStatement.executeUpdate();
                            }

                            try (PreparedStatement transactionStatement =
                                    connection.prepareStatement(transactionSQL)) {

                                transactionStatement.setInt(1, batchId);
                                transactionStatement.setInt(2, allocatedQuantity);
                                transactionStatement.executeUpdate();
                            }

                            remainingQuantity -= allocatedQuantity;
                        }
                    }
                }

                if (remainingQuantity > 0) {
                    int allocatedSoFar = requestedQuantity - remainingQuantity;
                    if (batchesFound == 0) {
                        throw new SQLException(
                            "No eligible non-expired stock is available for this medicine.");
                    } else {
                        throw new SQLException(
                            "Insufficient non-expired stock available. Requested: " +
                            requestedQuantity + ", Available: " + allocatedSoFar + ".");
                    }
                }

                connection.commit();

                System.out.println("Stock allocated successfully!");

            } catch (SQLException | RuntimeException e) {

                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("Rollback failed: " + rollbackEx.getMessage());
                }
                throw e;

            } finally {

                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        }
    }
}
