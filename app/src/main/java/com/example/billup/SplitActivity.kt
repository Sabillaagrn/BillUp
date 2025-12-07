package com.example.billup

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox // Ganti RadioButton jadi CheckBox
import android.widget.LinearLayout // Ganti RadioGroup jadi LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.ArrayList

class SplitActivity : AppCompatActivity() {

    private lateinit var rvSplitItems: RecyclerView
    private lateinit var btnFinish: Button
    private lateinit var splitAdapter: SplitAdapter

    private lateinit var receiptData: ReceiptData
    private var members = ArrayList<Contact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_split)

        rvSplitItems = findViewById(R.id.rv_split_items)
        btnFinish = findViewById(R.id.btn_finish_split)

        // Ambil Data
        receiptData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("RECEIPT_DATA", ReceiptData::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("RECEIPT_DATA") as ReceiptData
        }

        val selectedContacts = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("SELECTED_CONTACTS", ArrayList::class.java) as? ArrayList<Contact>
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("SELECTED_CONTACTS") as? ArrayList<Contact>
        }

        members.add(Contact("0", "Saya", "0800000000"))
        if (selectedContacts != null) {
            members.addAll(selectedContacts)
        }

        setupRecyclerView()

        btnFinish.setOnClickListener {
            val intent = Intent(this, SummaryActivity::class.java)
            intent.putExtra("RECEIPT_DATA", receiptData)
            intent.putExtra("MEMBERS", members)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        splitAdapter = SplitAdapter(receiptData.items, members)
        rvSplitItems.layoutManager = LinearLayoutManager(this)
        rvSplitItems.adapter = splitAdapter
    }
}

class SplitAdapter(
    private val items: List<ReceiptItem>,
    private val members: List<Contact>
) : RecyclerView.Adapter<SplitAdapter.SplitViewHolder>() {

    inner class SplitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemName: TextView = itemView.findViewById(R.id.tv_item_name)
        val itemPrice: TextView = itemView.findViewById(R.id.tv_item_price)
        // Ganti RadioGroup menjadi LinearLayout
        val containerMembers: LinearLayout = itemView.findViewById(R.id.ll_members_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SplitViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_split_row, parent, false)
        return SplitViewHolder(view)
    }

    override fun onBindViewHolder(holder: SplitViewHolder, position: Int) {
        val item = items[position]
        holder.itemName.text = "${item.name} (x${item.quantity})"
        holder.itemPrice.text = item.total.replace("Rp ", "")

        // Bersihkan views lama
        holder.containerMembers.removeAllViews()

        // Logic Default: Jika belum ada yang memilih sama sekali, otomatis pilih "Saya" (Index 0)
        if (item.assignedToIds.isEmpty()) {
            item.assignedToIds.add("0") // ID untuk "Saya"
        }

        // Loop untuk membuat CheckBox setiap member
        for (member in members) {
            val checkBox = CheckBox(holder.itemView.context)
            checkBox.text = member.name

            // Layout params untuk memberi jarak antar checkbox
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 16, 0)
            checkBox.layoutParams = params

            // Cek status: Apakah member ini ada di list pemilik item?
            // Kita hapus listener sementara untuk menghindari trigger saat setChecked
            checkBox.setOnCheckedChangeListener(null)

            checkBox.isChecked = item.assignedToIds.contains(member.id)

            // Pasang Listener
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Jika dicentang, tambahkan ID ke list (jika belum ada)
                    if (!item.assignedToIds.contains(member.id)) {
                        item.assignedToIds.add(member.id)
                    }
                } else {
                    // Jika di-uncheck, hapus ID dari list
                    item.assignedToIds.remove(member.id)
                }
            }

            holder.containerMembers.addView(checkBox)
        }
    }

    override fun getItemCount(): Int = items.size
}