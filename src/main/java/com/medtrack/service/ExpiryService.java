
package com.medtrack.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.medtrack.config.DBConnection;
import com.medtrack.model.Batch;

public class ExpiryService {

    public List<Batch> getExpiredBatches() throws SQLException {

        List<Batch> batches = new ArrayList<>();

        String sql = "SELECT * FROM batches "
                   + "WHERE expiry_date < CURDATE() "
                   + "AND available_quantity > 0 "
                   + "ORDER BY expiry_date ASC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {

                Batch batch = new Batch();

                batch.setBatchId(rs.getInt("batch_id"));
                batch.setMedicineId(rs.getInt("medicine_id"));
                batch.setBatchNumber(rs.getString("batch_number"));
                batch.setExpiryDate(
                    rs.getDate("expiry_date").toLocalDate());
                batch.setAvailableQuantity(
                    rs.getInt("available_quantity"));

                batches.add(batch);
            }
        }

        return batches;
    }

    public List<Batch> getExpiringSoonBatches(int days)
            throws SQLException {

        if (days < 0) {
            throw new IllegalArgumentException(
                "Days cannot be negative.");
        }

        List<Batch> batches = new ArrayList<>();

        String sql = "SELECT * FROM batches "
                   + "WHERE expiry_date >= CURDATE() "
                   + "AND expiry_date <= DATE_ADD(CURDATE(), INTERVAL ? DAY) "
                   + "AND available_quantity > 0 "
                   + "ORDER BY expiry_date ASC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, days);

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {

                    Batch batch = new Batch();

                    batch.setBatchId(rs.getInt("batch_id"));
                    batch.setMedicineId(rs.getInt("medicine_id"));
                    batch.setBatchNumber(rs.getString("batch_number"));
                    batch.setExpiryDate(
                        rs.getDate("expiry_date").toLocalDate());
                    batch.setAvailableQuantity(
                        rs.getInt("available_quantity"));

                    batches.add(batch);
                }
            }
        }

        return batches;
    }
}
