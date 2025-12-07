package com.example.billup

data class ReceiptItem(
    val name: String,
    val quantity: Int,
    val unitPrice: String,
    val total: String
)

data class ReceiptData(
    val storeName: String,
    val date: String,
    val subtotal: String,
    val discount: String,
    val tax: String,
    val grandTotal: String,
    val items: List<ReceiptItem>
)