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
        val nameQty: TextView = itemView.findViewById(R.id.tv_item_name_qty)
        val total: TextView = itemView.findViewById(R.id.tv_item_total)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receipt_detail, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]

        // Format: Nama Barang xQty HargaSatuan
        val qtyText = if (item.quantity > 0 && item.quantity != 1) " x${item.quantity}" else ""

        // Gunakan item.unitPrice untuk unit price, tapi pastikan tidak berantakan
        val unitPriceText = if (item.unitPrice.isNotBlank() && item.unitPrice != "-") {
            " ${formatCurrency(item.unitPrice)}"
        } else {
            ""
        }

        holder.nameQty.text = "${item.name}${qtyText}${unitPriceText}"

        // Total per item, tanpa prefix 'Rp'
        holder.total.text = formatCurrency(item.total).removePrefix("Rp ")
    }

    override fun getItemCount(): Int = items.size

    private fun formatCurrency(amount: String): String {
        return try {
            val numString = amount.replace("[^0-9]".toRegex(), "")
            if (numString.isEmpty()) return amount

            val num = numString.toLong()
            val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale("id", "ID")))
            "Rp ${formatter.format(num)}"
        } catch (e: Exception) {
            amount
        }
    }
}