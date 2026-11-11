
package com.medtrack;

import java.sql.SQLException;

import com.medtrack.service.FEFOService;

public class FEFORollbackTest {

    public static void main(String[] args) {

        FEFOService service = new FEFOService();

        try {

            service.allocateStock(1, 300);

            System.out.println(
                "ERROR: Allocation unexpectedly succeeded!"
            );

        } catch (SQLException e) {

            System.out.println(
                "Expected allocation failure: " + e.getMessage()
            );

        }
    }
}
