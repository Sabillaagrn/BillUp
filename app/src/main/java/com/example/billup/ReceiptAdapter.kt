package com.example.billup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class ReceiptAdapter(private val items: List<ReceiptItem>) :
    RecyclerView.Adapter<ReceiptAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_item_name)
        val qtyPrice: TextView = itemView.findViewById(R.id.tv_item_qty_price)
        val total: TextView = itemView.findViewById(R.id.tv_item_total)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receipt_detail, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]

        // Qty text (x2, x3, dll)
        val qtyText = if (item.quantity > 1) "x${item.quantity}" else "x1"

        // Format harga satuan (Rp 12.000)
        val unitPriceText = if (item.unitPrice.isNotBlank() && item.unitPrice != "-") {
            formatCurrency(item.unitPrice)
        } else {
            ""
        }

        // Set nama
        holder.name.text = item.name

        // Set qty & unit price
        holder.qtyPrice.text = "$qtyText · $unitPriceText"

        // Set total item (tanpa prefix Rp)
        holder.total.text = formatCurrency(item.total).removePrefix("Rp ")
    }

    override fun getItemCount(): Int = items.size

    private fun formatCurrency(amount: String): String {
        return try {
            val clean = amount.replace("[^0-9]".toRegex(), "")
            if (clean.isEmpty()) return amount

            val num = clean.toLong()
            val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale("id", "ID")))
            "Rp ${formatter.format(num)}"
        } catch (e: Exception) {
            amount
        }
    }
}
