
package com.medtrack;

import java.util.Map;
import com.medtrack.service.StockAlertService;

public class StockAlertTest {

    public static void main(String[] args) {

        StockAlertService service = new StockAlertService();

        Map<String, Integer> lowStock =
            service.getLowStockMedicines();

        System.out.println("===== LOW STOCK MEDICINES =====");

        if (lowStock.isEmpty()) {

            System.out.println("No low-stock medicines found.");

        } else {

            for (Map.Entry<String, Integer> entry :
                    lowStock.entrySet()) {

                System.out.println(
                    entry.getKey() +
                    " | Available Stock: " +
                    entry.getValue()
                );
            }
        }
    }
}
