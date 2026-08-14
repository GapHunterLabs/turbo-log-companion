package com.example.demo

class OrderServiceKt {

    fun calculateTotal(lineItems: List<Double>, taxRate: Double): Double {
        val subtotal = lineItems.sum()
        val total = subtotal * (1 + taxRate)
        return total
    }

    fun processOrder(orderId: String, quantity: Int) {
        val status = "PENDING"
        println("Processing $orderId")
    }
}
