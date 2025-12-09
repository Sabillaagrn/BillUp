package com.example.billup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class SummaryItemAdapter(
    private val items: List<ReceiptItem>
) : RecyclerView.Adapter<SummaryItemAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvItemName: TextView = view.findViewById(R.id.tv_item_name)
        val tvItemPrice: TextView = view.findViewById(R.id.tv_item_price)

        fun bind(item: ReceiptItem) {
            tvItemName.text = item.name

            // Parse total price
            val totalPrice = item.total.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
            tvItemPrice.text = formatPrice(totalPrice)
        }

        private fun formatPrice(price: Double): String {
            val formatter = DecimalFormat("#,###.##", DecimalFormatSymbols(Locale("id", "ID")))
            return formatter.format(price)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_summary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}