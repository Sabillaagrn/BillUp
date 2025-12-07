package com.example.billup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

fun String.getInitials(): String {
    val parts = this.trim().split("\\s+".toRegex())

    return when {
        parts.size == 1 && parts[0].isNotEmpty() -> parts[0].substring(0, 1).uppercase(Locale.getDefault())
        parts.size >= 2 -> {
            val firstInitial = parts[0].firstOrNull()?.toString() ?: ""
            val secondInitial = parts[1].firstOrNull()?.toString() ?: ""
            (firstInitial + secondInitial).uppercase(Locale.getDefault())
        }
        else -> ""
    }
}
class ContactAdapter(private var allContacts: List<Contact>) :
    RecyclerView.Adapter<ContactAdapter.ContactViewHolder>(), Filterable {

    private var filteredContacts: MutableList<Contact> = allContacts.toMutableList()

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val initials: TextView = itemView.findViewById(R.id.tv_initials)
        val name: TextView = itemView.findViewById(R.id.tv_name)
        val phoneNumber: TextView = itemView.findViewById(R.id.tv_phone_number)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_contact)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = filteredContacts[position]

        val initialsText = contact.name.getInitials()
        holder.initials.text = initialsText

        // --- LOGIKA DETAIL DAN CHECKBOX LAMA ---

        holder.name.text = contact.name
        holder.phoneNumber.text = contact.phoneNumber

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = contact.isSelected

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            contact.isSelected = isChecked
            // Logika tambahan jika diperlukan
        }
    }

    override fun getItemCount(): Int = filteredContacts.size

    fun getSelectedContacts(): List<Contact> {
        return allContacts.filter { it.isSelected }
    }


    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString()?.lowercase(Locale.getDefault()) ?: ""
                val newFilteredList = if (charString.isEmpty()) {
                    allContacts.toMutableList()
                } else {
                    allContacts.filter {
                        it.name.lowercase(Locale.getDefault()).contains(charString) ||
                                it.phoneNumber.contains(charString)
                    }.toMutableList()
                }

                val filterResults = FilterResults()
                filterResults.values = newFilteredList
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredContacts = (results?.values as? MutableList<Contact>) ?: mutableListOf()
                notifyDataSetChanged()
            }
        }
    }
}