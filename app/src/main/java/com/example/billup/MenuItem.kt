package com.example.billup

import java.io.Serializable

data class MenuItem(
    val id: Int,
    val name: String,
    val quantity: Int,
    val pricePerItem: Double,
    // Map: contactId -> berapa banyak item yang diambil orang itu
    var assignedQuantities: MutableMap<String, Int> = mutableMapOf()
) : Serializable {

    // Cek apakah contact sudah assign item ini (qty > 0)
    fun isAssignedTo(contactId: String): Boolean {
        val qty = assignedQuantities[contactId] ?: 0
        return qty > 0
    }

    // Toggle assignment: set ke 1 atau 0
    fun toggleAssignment(contactId: String) {
        val currentQty = assignedQuantities[contactId] ?: 0
        if (currentQty > 0) {
            // Unassign: set ke 0 tapi JANGAN remove dari map
            assignedQuantities[contactId] = 0
        } else {
            // Assign: set ke 1
            assignedQuantities[contactId] = 1
        }
    }

    // Total quantity yang sudah di-assign (hanya hitung yang > 0)
    fun getTotalAssignedQuantity(): Int {
        return assignedQuantities.values.filter { it > 0 }.sum()
    }

    // Sisa quantity yang belum di-assign
    fun getRemainingQuantity(): Int {
        return quantity - getTotalAssignedQuantity()
    }

    // Cek apakah semua quantity sudah di-assign
    fun isFullyAssigned(): Boolean {
        return getTotalAssignedQuantity() == quantity
    }

    // Get quantity untuk contact tertentu
    fun getQuantityFor(contactId: String): Int {
        return assignedQuantities[contactId] ?: 0
    }
}