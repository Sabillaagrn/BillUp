package com.example.billup

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.io.Serializable
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class SplitActivity : AppCompatActivity() {

    private lateinit var rvSplitItems: RecyclerView
    private lateinit var btnNext: MaterialButton
    private lateinit var containerAvatar1: LinearLayout
    private lateinit var containerAvatar2: LinearLayout
    private lateinit var containerAvatar3: LinearLayout
    private lateinit var avatar1: MaterialCardView
    private lateinit var avatar2: MaterialCardView
    private lateinit var avatar3: MaterialCardView
    private lateinit var tvName1: TextView
    private lateinit var tvName2: TextView
    private lateinit var tvName3: TextView
    private lateinit var tvAvatar1: TextView
    private lateinit var tvAvatar2: TextView
    private lateinit var tvAvatar3: TextView
    private lateinit var tvRestaurantName: TextView
    private lateinit var tvItemCount: TextView
    private lateinit var tvSubtotal: TextView
    private lateinit var tvTax: TextView
    private lateinit var tvGrandTotal: TextView

    private var selectedContact: Contact? = null
    private var contacts = arrayListOf<Contact>()
    private var menuItems = mutableListOf<MenuItem>()
    private lateinit var adapter: SplitItemAdapter
    private lateinit var receiptData: ReceiptData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_split)

        initViews()
        loadDataFromIntent()
        setupRecyclerView()
        setupAvatars()
        updateSummaryUI()
    }

    private fun initViews() {
        rvSplitItems = findViewById(R.id.rv_split_items)
        btnNext = findViewById(R.id.btn_next)
        containerAvatar1 = findViewById(R.id.container_avatar_1)
        containerAvatar2 = findViewById(R.id.container_avatar_2)
        containerAvatar3 = findViewById(R.id.container_avatar_3)
        avatar1 = findViewById(R.id.avatar_1)
        avatar2 = findViewById(R.id.avatar_2)
        avatar3 = findViewById(R.id.avatar_3)
        tvName1 = findViewById(R.id.tv_name_1)
        tvName2 = findViewById(R.id.tv_name_2)
        tvName3 = findViewById(R.id.tv_name_3)
        tvAvatar1 = findViewById(R.id.tv_avatar_1)
        tvAvatar2 = findViewById(R.id.tv_avatar_2)
        tvAvatar3 = findViewById(R.id.tv_avatar_3)
        tvRestaurantName = findViewById(R.id.tv_restaurant_name)
        tvItemCount = findViewById(R.id.tv_item_count)
        tvSubtotal = findViewById(R.id.tv_subtotal)
        tvTax = findViewById(R.id.tv_tax)
        tvGrandTotal = findViewById(R.id.tv_grand_total)
    }

    private fun loadDataFromIntent() {
        receiptData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("RECEIPT_DATA", ReceiptData::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("RECEIPT_DATA") as ReceiptData
        }

        contacts = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("SELECTED_CONTACTS", ArrayList::class.java) as? ArrayList<Contact>
                ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("SELECTED_CONTACTS") as? ArrayList<Contact> ?: arrayListOf()
        }

        if (contacts.isEmpty()) {
            Toast.makeText(this, "Tidak ada kontak tersedia", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Mapping receipt items ke MenuItem
        menuItems = receiptData.items.mapIndexed { index, item ->
            // Parse total price - remove semua karakter non-digit kecuali titik
            val totalString = item.total.replace(Regex("[^0-9.]"), "")
            val totalPrice = totalString.toDoubleOrNull() ?: 0.0

            // Parse unit price jika ada
            val unitString = item.unitPrice.replace(Regex("[^0-9.]"), "")
            val unitPrice = unitString.toDoubleOrNull() ?: 0.0

            // Prioritas: gunakan unitPrice jika ada, kalau tidak hitung dari total/quantity
            val pricePerItem = when {
                unitPrice > 0 -> unitPrice
                item.quantity > 0 && totalPrice > 0 -> totalPrice / item.quantity
                else -> totalPrice
            }

            MenuItem(
                id = index,
                name = item.name,
                quantity = item.quantity,
                pricePerItem = pricePerItem
            )
        }.toMutableList()

        // Set restaurant name
        tvRestaurantName.text = receiptData.storeName
    }

    private fun setupRecyclerView() {
        adapter = SplitItemAdapter(
            items = menuItems,
            selectedContactId = null,
            onItemClick = { item -> onItemClicked(item) }
        )
        rvSplitItems.layoutManager = LinearLayoutManager(this)
        rvSplitItems.adapter = adapter

        btnNext.setOnClickListener { goToSummary() }
    }

    private fun setupAvatars() {
        val containers = listOf(containerAvatar1, containerAvatar2, containerAvatar3)
        val avatars = listOf(avatar1, avatar2, avatar3)
        val names = listOf(tvName1, tvName2, tvName3)
        val avatarTexts = listOf(tvAvatar1, tvAvatar2, tvAvatar3)

        for (i in containers.indices) {
            if (i < contacts.size) {
                val contact = contacts[i]

                // Set nama
                names[i].text = contact.name

                // Set initial avatar
                val initial = contact.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                avatarTexts[i].text = initial

                // Set click listener
                avatars[i].setOnClickListener { selectContact(contact) }

                // Show container
                containers[i].visibility = View.VISIBLE
            } else {
                // Hide unused containers
                containers[i].visibility = View.GONE
            }
        }

        // Default select first contact
        if (contacts.isNotEmpty()) {
            selectContact(contacts[0])
        }
    }

    private fun selectContact(contact: Contact) {
        selectedContact = contact

        // Update avatar colors
        updateAvatarColors()

        // Update adapter dengan selected contact ID
        adapter.updateSelectedContact(contact.id)
    }

    private fun updateAvatarColors() {
        val selectedColor = Color.parseColor("#34656D")
        val unselectedColor = Color.parseColor("#B8B8B8") // Abu-abu untuk semua yang tidak dipilih
        val avatars = listOf(avatar1, avatar2, avatar3)

        for (i in avatars.indices) {
            if (i < contacts.size) {
                // Cek apakah contact ini yang sedang dipilih
                val isSelected = contacts[i].id == selectedContact?.id
                val color = if (isSelected) selectedColor else unselectedColor
                avatars[i].setCardBackgroundColor(color)
            }
        }
    }

    private fun onItemClicked(item: MenuItem) {
        if (selectedContact == null) {
            Toast.makeText(this, "Pilih kontak terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val contactId = selectedContact!!.id

        // Cek current state
        val currentQty = item.getQuantityFor(contactId)

        if (currentQty > 0) {
            // User sudah assign item ini, klik lagi untuk unassign
            item.assignedQuantities[contactId] = 0
        } else {
            // User belum assign, cek apakah masih ada sisa
            if (item.getRemainingQuantity() > 0) {
                // Assign 1 quantity
                item.assignedQuantities[contactId] = 1
            } else {
                Toast.makeText(this, "Semua quantity sudah di-assign orang lain", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Sync dengan ReceiptItem
        syncToReceiptItem(item)

        // Update UI
        adapter.notifyDataSetChanged()
        updateSummaryUI()
    }

    private fun syncToReceiptItem(menuItem: MenuItem) {
        val receiptItem = receiptData.items[menuItem.id]
        receiptItem.assignedToIds.clear()

        // Tambahkan contactId sebanyak qty yang di-assign (hanya yang > 0)
        menuItem.assignedQuantities.forEach { (contactId, qty) ->
            if (qty > 0) {
                repeat(qty) {
                    receiptItem.assignedToIds.add(contactId)
                }
            }
        }
    }

    private fun updateSummaryUI() {
        tvItemCount.text = "${menuItems.size} Items"

        // Parse dengan remove semua non-digit kecuali titik
        val subtotalStr = receiptData.subtotal.replace(Regex("[^0-9.]"), "")
        val taxStr = receiptData.tax.replace(Regex("[^0-9.]"), "")
        val grandTotalStr = receiptData.grandTotal.replace(Regex("[^0-9.]"), "")

        val subtotal = subtotalStr.toDoubleOrNull() ?: 0.0
        val tax = taxStr.toDoubleOrNull() ?: 0.0
        val grandTotal = grandTotalStr.toDoubleOrNull() ?: 0.0

        tvSubtotal.text = ": ${formatPrice(subtotal)}"
        tvTax.text = ": ${formatPrice(tax)}"
        tvGrandTotal.text = formatPrice(grandTotal)
    }

    private fun formatPrice(price: Double): String {
        val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale("id", "ID")))
        return "Rp ${formatter.format(price)}"
    }

    private fun goToSummary() {
        // Validasi: semua quantity harus ter-assign
        val unassignedItems = menuItems.filter { !it.isFullyAssigned() }

        if (unassignedItems.isNotEmpty()) {
            val totalUnassigned = unassignedItems.sumOf { it.getRemainingQuantity() }
            Toast.makeText(
                this,
                "Masih ada $totalUnassigned item yang belum di-assign",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Final sync semua data
        menuItems.forEach { syncToReceiptItem(it) }

        // Navigasi ke Summary
        val intent = Intent(this, SummaryActivity::class.java)
        intent.putExtra("RECEIPT_DATA", receiptData as Serializable)
        intent.putExtra("MEMBERS", contacts as Serializable)
        startActivity(intent)
    }
}