package com.example.billup

import java.io.Serializable
import java.util.ArrayList

data class ReceiptItem(
    val name: String,
    val quantity: Int,
    val unitPrice: String,
    val total: String,
    // UBAH DARI String? MENJADI ArrayList<String>
    var assignedToIds: ArrayList<String> = ArrayList()
) : Serializable

data class ReceiptData(
    val storeName: String,
    val date: String,
    val subtotal: String,
    val discount: String,
    val tax: String,
    val grandTotal: String,
    val items: List<ReceiptItem>
) : Serializable