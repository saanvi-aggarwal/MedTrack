
package com.medtrack;

import com.medtrack.service.FEFOService;

public class FEFOTest {

    public static void main(String[] args) {

        FEFOService fefoService = new FEFOService();

        try {
            fefoService.allocateStock(1, 70);

            System.out.println("FEFO test completed!");

        } catch (Exception e) {
            System.out.println("FEFO test failed!");
            e.printStackTrace();
        }
    }
}
