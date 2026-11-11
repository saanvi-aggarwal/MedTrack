
package com.medtrack;

import java.sql.SQLException;
import java.util.List;

import com.medtrack.model.Batch;
import com.medtrack.service.ExpiryService;

public class ExpiryTest {

    public static void main(String[] args) {

        ExpiryService service = new ExpiryService();

        try {

            System.out.println("===== EXPIRED BATCHES =====");

            List<Batch> expired =
                service.getExpiredBatches();

            if (expired.isEmpty()) {
                System.out.println("No expired batches found.");
            } else {
                for (Batch batch : expired) {
                    System.out.println(
                        batch.getBatchNumber() + " | " +
                        batch.getExpiryDate() + " | " +
                        batch.getAvailableQuantity()
                    );
                }
            }

            System.out.println("\n===== EXPIRING WITHIN 90 DAYS =====");

            List<Batch> expiring =
                service.getExpiringSoonBatches(90);

            if (expiring.isEmpty()) {
                System.out.println("No batches expiring soon.");
            } else {
                for (Batch batch : expiring) {
                    System.out.println(
                        batch.getBatchNumber() + " | " +
                        batch.getExpiryDate() + " | " +
                        batch.getAvailableQuantity()
                    );
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
