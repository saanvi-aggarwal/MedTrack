package com.medtrack.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.medtrack.config.DBConnection;

public class StockAlertService {

    public Map<String, Integer> getLowStockMedicines() {

        Map<String, Integer> lowStock = new HashMap<>();

        String sql = """
            SELECT m.medicine_name,
                   m.reorder_level,
                   COALESCE(SUM(b.available_quantity), 0) AS total_stock
            FROM medicines m
            LEFT JOIN batches b
                ON m.medicine_id = b.medicine_id
                AND b.expiry_date >= CURDATE()
            GROUP BY m.medicine_id, m.medicine_name, m.reorder_level
            HAVING total_stock <= m.reorder_level
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lowStock.put(
                    rs.getString("medicine_name"),
                    rs.getInt("total_stock")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lowStock;
    }
}