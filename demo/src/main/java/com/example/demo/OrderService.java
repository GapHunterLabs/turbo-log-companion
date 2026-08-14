package com.example.demo;

import java.util.List;

public class OrderService {

    private int retryCount = 0;

    public double calculateTotal(List<Double> lineItems, double taxRate) {
        double subtotal = 0.0;
        for (Double item : lineItems) {
            subtotal += item;
        }
        double total = subtotal * (1 + taxRate);
        return total;
    }

    public static void processOrder(String orderId, int quantity) {
        String status = "PENDING";
        System.out.println("Processing " + orderId);
    }

    public void retry() {
        retryCount = retryCount + 1;
    }
}
