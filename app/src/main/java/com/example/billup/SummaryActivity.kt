package com.example.billup

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.Serializable
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

data class MemberSummary(
    val contact: Contact,
    val items: List<ReceiptItem>,
    val subtotal: Double,
    val tax: Double,
    val total: Double
) : Serializable

class SummaryActivity : AppCompatActivity() {

    private lateinit var containerAvatar1: LinearLayout
    private lateinit var containerAvatar2: LinearLayout
    private lateinit var containerAvatar3: LinearLayout
    private lateinit var avatar1: MaterialCardView
    private lateinit var avatar2: MaterialCardView
    private lateinit var avatar3: MaterialCardView
    private lateinit var tvAvatar1: TextView
    private lateinit var tvAvatar2: TextView
    private lateinit var tvAvatar3: TextView
    private lateinit var tvName1: TextView
    private lateinit var tvName2: TextView
    private lateinit var tvName3: TextView

    private lateinit var rvSummaryItems: RecyclerView
    private lateinit var tvTaxValue: TextView
    private lateinit var tvGrandTotal: TextView
    private lateinit var fabWhatsapp: FloatingActionButton

    private var selectedContact: Contact? = null
    private var contacts = arrayListOf<Contact>()
    private var memberSummaries = mutableMapOf<String, MemberSummary>()
    private lateinit var receiptData: ReceiptData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        initViews()
        loadDataFromIntent()
        calculateSummaries()
        setupAvatars()

        // Default select first member
        if (contacts.isNotEmpty()) {
            selectContact(contacts[0])
        }
    }

    private fun initViews() {
        containerAvatar1 = findViewById(R.id.container_avatar_1)
        containerAvatar2 = findViewById(R.id.container_avatar_2)
        containerAvatar3 = findViewById(R.id.container_avatar_3)
        avatar1 = findViewById(R.id.avatar_1)
        avatar2 = findViewById(R.id.avatar_2)
        avatar3 = findViewById(R.id.avatar_3)
        tvAvatar1 = findViewById(R.id.tv_avatar_1)
        tvAvatar2 = findViewById(R.id.tv_avatar_2)
        tvAvatar3 = findViewById(R.id.tv_avatar_3)
        tvName1 = findViewById(R.id.tv_name_1)
        tvName2 = findViewById(R.id.tv_name_2)
        tvName3 = findViewById(R.id.tv_name_3)
        rvSummaryItems = findViewById(R.id.rv_summary_items)
        tvTaxValue = findViewById(R.id.tv_tax_value)
        tvGrandTotal = findViewById(R.id.tv_grand_total)
        fabWhatsapp = findViewById(R.id.fab_whatsapp)

        rvSummaryItems.layoutManager = LinearLayoutManager(this)

        fabWhatsapp.setOnClickListener {
            sendWhatsAppMessage()
        }
    }

    private fun loadDataFromIntent() {
        receiptData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("RECEIPT_DATA", ReceiptData::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("RECEIPT_DATA") as ReceiptData
        }

        contacts = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("MEMBERS", ArrayList::class.java) as ArrayList<Contact>
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("MEMBERS") as ArrayList<Contact>
        }

        if (contacts.isEmpty()) {
            Toast.makeText(this, "Tidak ada member", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun calculateSummaries() {
        // Parse tax dari receipt
        val totalTax = receiptData.tax.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0

        // Hitung total subtotal dari semua items
        var grandSubtotal = 0.0
        receiptData.items.forEach { item ->
            val itemTotal = item.total.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
            grandSubtotal += itemTotal
        }

        // Hitung summary untuk setiap member
        contacts.forEach { contact ->
            val memberItems = mutableListOf<ReceiptItem>()
            var memberSubtotal = 0.0

            receiptData.items.forEach { item ->
                // Hitung berapa qty yang diambil member ini
                val qtyForMember = item.assignedToIds.count { it == contact.id }

                if (qtyForMember > 0) {
                    // Parse harga
                    val itemTotal = item.total.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                    val unitPrice = item.unitPrice.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0

                    val pricePerItem = if (unitPrice > 0) {
                        unitPrice
                    } else if (item.quantity > 0) {
                        itemTotal / item.quantity
                    } else {
                        itemTotal
                    }

                    // Total untuk member ini dari item ini
                    val memberItemTotal = pricePerItem * qtyForMember
                    memberSubtotal += memberItemTotal

                    // Tambahkan ke list dengan quantity yang sesuai
                    memberItems.add(
                        ReceiptItem(
                            name = item.name,
                            quantity = qtyForMember,
                            unitPrice = formatNumber(pricePerItem),
                            total = formatNumber(memberItemTotal),
                            assignedToIds = arrayListOf(contact.id)
                        )
                    )
                }
            }

            // Hitung pajak proporsional
            val memberTaxProportion = if (grandSubtotal > 0) {
                (memberSubtotal / grandSubtotal) * totalTax
            } else {
                0.0
            }

            // Total = subtotal + pajak
            val memberTotal = memberSubtotal + memberTaxProportion

            memberSummaries[contact.id] = MemberSummary(
                contact = contact,
                items = memberItems,
                subtotal = memberSubtotal,
                tax = memberTaxProportion,
                total = memberTotal
            )
        }
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

        // Update colors
        updateAvatarColors()
    }

    private fun selectContact(contact: Contact) {
        selectedContact = contact
        updateAvatarColors()
        displaySummaryForContact(contact)
    }

    private fun updateAvatarColors() {
        val selectedColor = Color.parseColor("#34656D")
        val unselectedColor = Color.parseColor("#B8B8B8")
        val avatars = listOf(avatar1, avatar2, avatar3)

        for (i in avatars.indices) {
            if (i < contacts.size) {
                val isSelected = contacts[i].id == selectedContact?.id
                val color = if (isSelected) selectedColor else unselectedColor
                avatars[i].setCardBackgroundColor(color)
            }
        }
    }

    private fun displaySummaryForContact(contact: Contact) {
        val summary = memberSummaries[contact.id] ?: return

        // Display items
        val adapter = SummaryItemAdapter(summary.items)
        rvSummaryItems.adapter = adapter

        // Display tax and total
        tvTaxValue.text = "Pajak : ${formatPrice(summary.tax)}"
        tvGrandTotal.text = "Grand Total : ${formatPrice(summary.total)}"
    }

    private fun sendWhatsAppMessage() {
        val contact = selectedContact ?: return
        val summary = memberSummaries[contact.id] ?: return

        // Build message
        val message = buildString {
            appendLine("🧾 *Split Bill - ${receiptData.storeName}*")
            appendLine("Tanggal: ${receiptData.date.split(" ")[0]}")
            appendLine()
            appendLine("📋 *Tagihan untuk: ${contact.name}*")
            appendLine()

            summary.items.forEach { item ->
                appendLine("• ${item.name}")
                appendLine("  ${item.quantity}x @ ${formatPrice(item.unitPrice.toDoubleOrNull() ?: 0.0)} = ${formatPrice(item.total.toDoubleOrNull() ?: 0.0)}")
            }

            appendLine()
            appendLine("Subtotal: ${formatPrice(summary.subtotal)}")
            appendLine("Pajak: ${formatPrice(summary.tax)}")
            appendLine("━━━━━━━━━━━━━━")
            appendLine("*Total: ${formatPrice(summary.total)}*")
        }

        // Open WhatsApp
        try {
            val phoneNumber = contact.phoneNumber.replace(Regex("[^0-9]"), "")
            val url = "https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp tidak terinstall", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatNumber(number: Double): String {
        return number.toString()
    }

    private fun formatPrice(price: Double): String {
        val formatter = DecimalFormat("#,###.##", DecimalFormatSymbols(Locale("id", "ID")))
        return "Rp ${formatter.format(price)}"
    }
}