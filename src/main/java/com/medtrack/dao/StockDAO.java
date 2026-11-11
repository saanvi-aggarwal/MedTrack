
package com.medtrack.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.medtrack.config.DBConnection;
import com.medtrack.model.StockTransaction;

public class StockDAO {

    public List<StockTransaction> getAllTransactions() {

        List<StockTransaction> transactions = new ArrayList<>();

        String sql = "SELECT * FROM stock_transactions "
                   + "ORDER BY transaction_date DESC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {

                StockTransaction transaction = new StockTransaction();

                transaction.setTransactionId(
                    resultSet.getInt("transaction_id"));

                transaction.setBatchId(
                    resultSet.getInt("batch_id"));

                transaction.setTransactionType(
                    resultSet.getString("transaction_type"));

                transaction.setQuantity(
                    resultSet.getInt("quantity"));

                transaction.setReferenceNumber(
                    resultSet.getString("reference_number"));

                transaction.setNotes(
                    resultSet.getString("notes"));

                if (resultSet.getTimestamp("transaction_date") != null) {
                    transaction.setTransactionDate(
                        resultSet.getTimestamp("transaction_date").toLocalDateTime());
                }

                transactions.add(transaction);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return transactions;
    }
}
