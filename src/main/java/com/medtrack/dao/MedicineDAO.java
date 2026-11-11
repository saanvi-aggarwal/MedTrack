package com.medtrack.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.medtrack.config.DBConnection;
import com.medtrack.model.Medicine;

public class MedicineDAO {

    public List<Medicine> getAllMedicines() {

        List<Medicine> medicines = new ArrayList<>();

        String sql = "SELECT * FROM medicines ORDER BY medicine_id ASC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {

                Medicine medicine = new Medicine();

                medicine.setMedicineId(resultSet.getInt("medicine_id"));
                medicine.setMedicineName(resultSet.getString("medicine_name"));
                medicine.setGenericName(resultSet.getString("generic_name"));
                medicine.setCategory(resultSet.getString("category"));
                medicine.setManufacturer(resultSet.getString("manufacturer"));
                medicine.setUnit(resultSet.getString("unit"));
                medicine.setReorderLevel(resultSet.getInt("reorder_level"));

                medicines.add(medicine);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return medicines;
    }

    public int getTotalMedicines() {

        String sql = "SELECT COUNT(*) FROM medicines";

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

    public boolean addMedicine(Medicine medicine) throws SQLException {

        String sql = "INSERT INTO medicines "
                + "(medicine_name, generic_name, category, "
                + "manufacturer, unit, reorder_level) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, medicine.getMedicineName());
            statement.setString(2, medicine.getGenericName());
            statement.setString(3, medicine.getCategory());
            statement.setString(4, medicine.getManufacturer());
            statement.setString(5, medicine.getUnit());
            statement.setInt(6, medicine.getReorderLevel());

            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateMedicine(Medicine medicine) throws SQLException {

        String sql = "UPDATE medicines SET "
                + "medicine_name = ?, generic_name = ?, category = ?, "
                + "manufacturer = ?, unit = ?, reorder_level = ? "
                + "WHERE medicine_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, medicine.getMedicineName());
            statement.setString(2, medicine.getGenericName());
            statement.setString(3, medicine.getCategory());
            statement.setString(4, medicine.getManufacturer());
            statement.setString(5, medicine.getUnit());
            statement.setInt(6, medicine.getReorderLevel());
            statement.setInt(7, medicine.getMedicineId());

            return statement.executeUpdate() > 0;
        }
    }

    public boolean hasBatches(int medicineId) throws SQLException {

        String sql = "SELECT COUNT(*) FROM batches WHERE medicine_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, medicineId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public boolean deleteMedicine(int medicineId) throws SQLException {

        if (hasBatches(medicineId)) {
            throw new SQLException("Cannot delete medicine: Existing batches are associated with this medicine.");
        }

        String sql = "DELETE FROM medicines WHERE medicine_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, medicineId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean isMedicineNameExists(String name, int excludeId) {

        String sql = "SELECT COUNT(*) FROM medicines WHERE LOWER(TRIM(medicine_name)) = LOWER(TRIM(?)) AND medicine_id != ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setInt(2, excludeId);

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

    public Medicine getMedicineById(int id) {

        String sql = "SELECT * FROM medicines WHERE medicine_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Medicine medicine = new Medicine();
                    medicine.setMedicineId(rs.getInt("medicine_id"));
                    medicine.setMedicineName(rs.getString("medicine_name"));
                    medicine.setGenericName(rs.getString("generic_name"));
                    medicine.setCategory(rs.getString("category"));
                    medicine.setManufacturer(rs.getString("manufacturer"));
                    medicine.setUnit(rs.getString("unit"));
                    medicine.setReorderLevel(rs.getInt("reorder_level"));
                    return medicine;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Object[]> getAllMedicinesWithStock() {

        List<Object[]> list = new ArrayList<>();

        String sql = """
            SELECT m.medicine_id, m.medicine_name, m.generic_name, m.category,
                   m.manufacturer, m.unit, m.reorder_level,
                   COALESCE(SUM(CASE WHEN b.expiry_date >= CURDATE() THEN b.available_quantity ELSE 0 END), 0) AS total_stock
            FROM medicines m
            LEFT JOIN batches b ON m.medicine_id = b.medicine_id
            GROUP BY m.medicine_id, m.medicine_name, m.generic_name, m.category, m.manufacturer, m.unit, m.reorder_level
            ORDER BY m.medicine_id ASC
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("medicine_id"),
                    rs.getString("medicine_name"),
                    rs.getString("generic_name"),
                    rs.getString("category"),
                    rs.getString("manufacturer"),
                    rs.getString("unit"),
                    rs.getInt("reorder_level"),
                    rs.getInt("total_stock")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
