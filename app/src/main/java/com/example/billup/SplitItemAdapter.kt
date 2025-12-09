package com.example.billup

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class SplitItemAdapter(
    private var items: MutableList<MenuItem>,
    private var selectedContactId: String? = null,
    private val onItemClick: (MenuItem) -> Unit
) : RecyclerView.Adapter<SplitItemAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rbItem: RadioButton = view.findViewById(R.id.rb_item)
        val tvItemName: TextView = view.findViewById(R.id.tv_item_name)
        val tvItemQuantity: TextView = view.findViewById(R.id.tv_item_quantity)
        val tvItemTotal: TextView = view.findViewById(R.id.tv_item_total)
        val tvRemaining: TextView = view.findViewById(R.id.tv_remaining)

        fun bind(item: MenuItem) {
            tvItemName.text = item.name

            // Format quantity dan price
            val qtyText = "x${item.quantity} @ ${formatPrice(item.pricePerItem)}"
            tvItemQuantity.text = qtyText

            // Total price untuk item ini
            val totalPrice = item.pricePerItem * item.quantity
            tvItemTotal.text = formatPrice(totalPrice)

            // CRITICAL: Radio button state berdasarkan assignedQuantities
            val contactQty = selectedContactId?.let { item.getQuantityFor(it) } ?: 0
            val isChecked = contactQty > 0

            rbItem.isChecked = isChecked

            // Warna text hitam ketika di-check
            if (isChecked) {
                tvItemName.setTextColor(Color.BLACK)
                tvItemQuantity.setTextColor(Color.parseColor("#666666"))
                tvItemTotal.setTextColor(Color.BLACK)
            } else {
                tvItemName.setTextColor(Color.parseColor("#999999"))
                tvItemQuantity.setTextColor(Color.parseColor("#BBBBBB"))
                tvItemTotal.setTextColor(Color.parseColor("#999999"))
            }

            // Show remaining quantity atau assigned quantity
            val remaining = item.getRemainingQuantity()

            if (contactQty > 0) {
                tvRemaining.visibility = View.VISIBLE
                tvRemaining.text = "Kamu ambil: $contactQty"
                tvRemaining.setTextColor(Color.parseColor("#4CAF50"))
            } else if (remaining > 0) {
                tvRemaining.visibility = View.VISIBLE
                tvRemaining.text = "Sisa: $remaining belum di-assign"
                tvRemaining.setTextColor(Color.parseColor("#FF9800"))
            } else if (remaining == 0 && contactQty == 0) {
                tvRemaining.visibility = View.VISIBLE
                tvRemaining.text = "Sudah di-assign orang lain"
                tvRemaining.setTextColor(Color.parseColor("#F44336"))
            } else {
                tvRemaining.visibility = View.GONE
            }

            // Click handler
            itemView.setOnClickListener {
                onItemClick(item)
            }

            // Radio button click juga trigger item click
            rbItem.setOnClickListener {
                onItemClick(item)
            }
        }

        private fun formatPrice(price: Double): String {
            val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale("id", "ID")))
            return "Rp ${formatter.format(price)}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_split, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    // Update selected contact dan refresh UI
    fun updateSelectedContact(contactId: String) {
        selectedContactId = contactId
        notifyDataSetChanged()
    }

    // Update items
    fun updateItems(newItems: MutableList<MenuItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}