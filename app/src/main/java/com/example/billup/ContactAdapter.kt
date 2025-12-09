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

class ContactAdapter(private var allContacts: List<Contact>) :
    RecyclerView.Adapter<ContactAdapter.ContactViewHolder>(), Filterable {

    private var filteredContacts: MutableList<Contact> = allContacts.toMutableList()

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_contact_name)
        val phoneNumber: TextView = itemView.findViewById(R.id.tv_contact_phone)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkSelect)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = filteredContacts[position]

        holder.name.text = contact.name
        holder.phoneNumber.text = contact.phoneNumber

        // Reset listener to avoid recycled checkbox state bugs
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = contact.isSelected

        // Update model
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            contact.isSelected = isChecked
        }
    }

    override fun getItemCount(): Int = filteredContacts.size

    fun getSelectedContacts(): List<Contact> {
        return allContacts.filter { it.isSelected }
    }

    // Search filter
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase(Locale.getDefault()) ?: ""

                val newList = if (query.isEmpty()) {
                    allContacts.toMutableList()
                } else {
                    allContacts.filter {
                        it.name.lowercase(Locale.getDefault()).contains(query) ||
                                it.phoneNumber.contains(query)
                    }.toMutableList()
                }

                val results = FilterResults()
                results.values = newList
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredContacts = results?.values as? MutableList<Contact> ?: mutableListOf()
                notifyDataSetChanged()
            }
        }
    }
}
