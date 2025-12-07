package com.example.billup

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.ArrayList

class SummaryActivity : AppCompatActivity() {

    private lateinit var rvSummary: RecyclerView
    private lateinit var receiptData: ReceiptData
    private lateinit var members: List<Contact>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        rvSummary = findViewById(R.id.rv_summary_cards)


        val btnFinish: Button = findViewById(R.id.btn_finish_summary)

        btnFinish.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            // Menghapus history activity agar user memulai fresh di halaman utama
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Ambil data dari Intent
        receiptData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("RECEIPT_DATA", ReceiptData::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("RECEIPT_DATA") as ReceiptData
        }

        members = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("MEMBERS", ArrayList::class.java) as ArrayList<Contact>
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("MEMBERS") as ArrayList<Contact>
        }

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val adapter = SummaryAdapter(receiptData, members)
        rvSummary.layoutManager = LinearLayoutManager(this)
        rvSummary.adapter = adapter
    }
}

class SummaryAdapter(
    private val receiptData: ReceiptData,
    private val members: List<Contact>
) : RecyclerView.Adapter<SummaryAdapter.SummaryViewHolder>() {

    inner class SummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_member_name)
        val tvItems: TextView = itemView.findViewById(R.id.tv_items_list)
        val tvTotal: TextView = itemView.findViewById(R.id.tv_member_total)
        val btnWA: Button = itemView.findViewById(R.id.btn_send_wa)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_summary_card, parent, false)
        return SummaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        val member = members[position]

        // Gunakan Double untuk perhitungan agar hasil pembagian akurat
        var totalAmount: Double = 0.0
        val itemsStringBuilder = StringBuilder()

        // Loop semua item yang ada di struk
        receiptData.items.forEach { item ->
            // Cek apakah member ini termasuk dalam list pemilik item tersebut
            if (item.assignedToIds.contains(member.id)) {

                // Parsing harga original item (bersihkan karakter non-digit)
                val priceFull = item.total.replace("[^0-9]".toRegex(), "").toDoubleOrNull() ?: 0.0

                // Hitung berapa orang yang menanggung item ini
                val sharersCount = item.assignedToIds.size

                // Hitung harga per orang
                val pricePerPerson = if (sharersCount > 0) priceFull / sharersCount else 0.0

                // Tambahkan ke total tagihan member ini
                totalAmount += pricePerPerson

                // Format teks rincian
                val formattedPrice = formatCurrency(pricePerPerson.toLong())

                if (sharersCount > 1) {
                    // Jika item patungan
                    itemsStringBuilder.append("- ${item.name} (Patungan $sharersCount org): $formattedPrice\n")
                } else {
                    // Jika item sendiri
                    itemsStringBuilder.append("- ${item.name}: $formattedPrice\n")
                }
            }
        }

        // --- Tampilkan ke UI ---
        holder.tvName.text = member.name
        holder.tvItems.text = if (itemsStringBuilder.isNotEmpty()) itemsStringBuilder.toString() else "Tidak ada item."

        // Convert total akhir kembali ke Long untuk format Rupiah
        holder.tvTotal.text = formatCurrency(totalAmount.toLong())

        // Konfigurasi Tombol WA
        if (member.id == "0") {
            // Jika "Saya", tombol sembunyikan (karena user sendiri yang bayar kasir)
            holder.btnWA.visibility = View.GONE
        } else {
            holder.btnWA.visibility = View.VISIBLE
            holder.btnWA.setOnClickListener {
                sendWhatsApp(holder.itemView.context, member, itemsStringBuilder.toString(), totalAmount.toLong())
            }
        }
    }

    override fun getItemCount(): Int = members.size

    private fun sendWhatsApp(context: android.content.Context, member: Contact, itemsDetails: String, total: Long) {
        try {
            // 1. Format nomor HP: Ganti 08xxx jadi 628xxx
            var phone = member.phoneNumber.replace(Regex("[^0-9]"), "")
            if (phone.startsWith("0")) {
                phone = "62" + phone.substring(1)
            }

            // 2. Format Pesan WhatsApp yang Lebih Rapi
            // Tips: Anda bisa menambahkan info No. Rekening / E-Wallet di bagian bawah
            val message = """
                Halo *${member.name}*👋,
                
                Berikut rincian patungan kamu di *${receiptData.storeName}*:
                
                $itemsDetails
                💰*TOTAL: ${formatCurrency(total)}*
                
                Mohon segera ditransfer ya. Terima kasih! 🙏
                
                _(Dikirim via BillUp)_
            """.trimIndent()

            // 3. Encode pesan agar karakter khusus (spasi, enter, emoji) terbaca oleh URL
            val encodedMessage = java.net.URLEncoder.encode(message, "UTF-8")

            // 4. Intent ke WhatsApp
            val url = "https://api.whatsapp.com/send?phone=$phone&text=$encodedMessage"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            context.startActivity(intent)

        } catch (e: Exception) {
            // Menangani error jika WhatsApp tidak terinstall atau error lainnya
            Toast.makeText(context, "Gagal membuka WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun formatCurrency(amount: Long): String {
        val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale("id", "ID")))
        return "Rp ${formatter.format(amount)}"
    }
}