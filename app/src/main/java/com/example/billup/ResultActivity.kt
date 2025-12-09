package com.example.billup

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.ArrayList
import java.util.Locale
import java.util.concurrent.TimeUnit

class ResultActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var contentLayout: View
    private lateinit var lastReceiptData: ReceiptData // Menyimpan data untuk dikirim ke Intent
    private var selectedContacts: ArrayList<Contact>? = null

    // API Key TabScanner dari Anda
    private val API_KEY = "djSaXlBOS47VHbTGWP7Yc1NMHhvmf7PQYyUeFBQRMiqaQVTLQCCpyL4wQIsCvBWd"

    // Client OkHttp dengan timeout yang cukup panjang karena proses OCR butuh waktu
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        progressBar = findViewById(R.id.progressBar)
        contentLayout = findViewById(R.id.contentLayout)

        contentLayout.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        // --- AMBIL DATA KONTAK ---
        selectedContacts = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("SELECTED_CONTACTS", ArrayList::class.java) as? ArrayList<Contact>
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("SELECTED_CONTACTS") as? ArrayList<Contact>
        }

        val photoPath = intent.getStringExtra("PHOTO_PATH")

        if (photoPath != null) {
            val file = File(photoPath)
            if (file.exists()) {
                // Proses langsung menggunakan File, tidak perlu Bitmap untuk TabScanner
                processReceiptWithTabScanner(file)
            } else {
                Toast.makeText(this, "File gambar tidak ditemukan", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "Path gambar null.", Toast.LENGTH_LONG).show()
            finish()
        }

        findViewById<Button>(R.id.btn_retake).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btn_confirm).setOnClickListener {
            goToSplitActivity()
        }
    }

    // --- METHOD BARU: Tambah "Saya" Otomatis ---
    private fun goToSplitActivity() {
        // Pastikan data sudah siap
        if (!this::lastReceiptData.isInitialized) {
            Toast.makeText(this, "Data struk belum siap", Toast.LENGTH_SHORT).show()
            return
        }

        val finalContacts = arrayListOf<Contact>()

        // 1. Tambah "Saya" sebagai contact pertama (current user)
        val currentUser = Contact(
            id = "me_${System.currentTimeMillis()}",
            name = "Saya",
            phoneNumber = "",
            isSelected = true,
            isCurrentUser = true
        )
        finalContacts.add(currentUser)

        // 2. Tambah teman-teman yang dipilih dari MainActivity (jika ada)
        if (selectedContacts != null && selectedContacts!!.isNotEmpty()) {
            finalContacts.addAll(selectedContacts!!)
        }

        // 3. Navigate ke SplitActivity
        val intent = Intent(this, SplitActivity::class.java)
        intent.putExtra("RECEIPT_DATA", lastReceiptData)
        intent.putExtra("SELECTED_CONTACTS", finalContacts)
        startActivity(intent)
    }

    // --- LOGIKA UTAMA TABSCANNER (TIDAK DIUBAH) ---

    private fun processReceiptWithTabScanner(imageFile: File) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. UPLOAD IMAGE
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ResultActivity, "Mengunggah gambar...", Toast.LENGTH_SHORT).show()
                }

                val token = uploadImage(imageFile)

                if (token != null) {
                    // 2. POLLING RESULT (Menunggu hasil)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ResultActivity, "Sedang memproses struk...", Toast.LENGTH_SHORT).show()
                    }

                    val jsonResult = pollResult(token)

                    if (jsonResult != null) {
                        // 3. PARSING & DISPLAY
                        val data = parseTabScannerJson(jsonResult)

                        withContext(Dispatchers.Main) {
                            displayReceiptData(data)
                            progressBar.visibility = View.GONE
                            contentLayout.visibility = View.VISIBLE
                        }
                    } else {
                        showError("Gagal mendapatkan hasil dari TabScanner (Timeout/Error).")
                    }
                } else {
                    showError("Gagal mengunggah gambar. Cek koneksi internet.")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                showError("Terjadi kesalahan: ${e.message}")
            }
        }
    }

    // Fungsi 1: Upload Gambar ke TabScanner
    private fun uploadImage(file: File): String? {
        val mediaType = "image/jpeg".toMediaTypeOrNull()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("receiptImage", file.name, file.asRequestBody(mediaType))
            .build()

        val request = Request.Builder()
            .url("https://api.tabscanner.com/api/2/process")
            .addHeader("apikey", API_KEY)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e("TabScanner", "Upload Failed: ${response.code}")
                return null
            }

            val responseBody = response.body?.string() ?: return null
            val json = JSONObject(responseBody)

            // TabScanner mengembalikan 'token' untuk mengambil hasil
            // Respons sukses biasanya code 200 dan message "Success"
            return if (json.optInt("code") == 200 || json.has("token")) {
                json.optString("token")
            } else {
                Log.e("TabScanner", "Error JSON: $responseBody")
                null
            }
        }
    }

    // Fungsi 2: Cek Hasil (Polling) sampai status 'done'
    private suspend fun pollResult(token: String): JSONObject? {
        val maxRetries = 15 // Coba 15 kali (15 x 2 detik = 30 detik maks tunggu)
        var attempts = 0

        while (attempts < maxRetries) {
            attempts++

            val request = Request.Builder()
                .url("https://api.tabscanner.com/api/result/$token")
                .addHeader("apikey", API_KEY)
                .get()
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val bodyString = response.body?.string()
                    if (bodyString != null) {
                        val json = JSONObject(bodyString)
                        val status = json.optString("status") // 'pending' atau 'done'

                        Log.d("TabScanner", "Attempt $attempts: Status = $status")

                        if (status == "done") {
                            return json.optJSONObject("result")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TabScanner", "Polling error", e)
            }

            // Tunggu 2 detik sebelum cek lagi
            delay(2000)
        }
        return null
    }

    // Fungsi 3: Parsing JSON TabScanner ke Object ReceiptData
    private fun parseTabScannerJson(resultJson: JSONObject): ReceiptData {
        val establishmentStr = resultJson.optString("establishment", "Toko Tidak Dikenal")
        val dateStr = resultJson.optString("date", "-")

        val totalStr = resultJson.optString("total", "0")
        val subTotalStr = resultJson.optString("subTotal", "0")

        // Cek tax & discount, fallback ke alternatif label
        var taxStr = resultJson.optString("tax", "")
        if (taxStr.isEmpty()) {
            // Cek field lain yang mungkin berisi PPN
            taxStr = resultJson.optString("PPN", "0")
        }

        var discountStr = resultJson.optString("discount", "")
        if (discountStr.isEmpty()) {
            // Cek field lain yang mungkin berisi TOTAL DISCOUNT
            discountStr = resultJson.optString("TOTAL DISCOUNT", "0")
        }

        val itemsList = ArrayList<ReceiptItem>()
        val lineItems = resultJson.optJSONArray("lineItems")
        if (lineItems != null) {
            for (i in 0 until lineItems.length()) {
                val itemObj = lineItems.getJSONObject(i)
                var desc = itemObj.optString("descClean")
                if (desc.isEmpty()) desc = itemObj.optString("desc", "Item")

                val qty = itemObj.optInt("qty", 1)
                val lineTotal = itemObj.optString("lineTotal", "0")
                val unitPrice = itemObj.optString("unitPrice", "0")
                val finalUnitPrice = if (unitPrice == "0" || unitPrice.isEmpty()) {
                    try {
                        val totalVal = lineTotal.replace("[^0-9.]".toRegex(), "").toDouble()
                        (totalVal / qty).toString()
                    } catch (e:Exception) { "0" }
                } else unitPrice

                itemsList.add(
                    ReceiptItem(
                        name = desc,
                        quantity = qty,
                        unitPrice = finalUnitPrice,
                        total = lineTotal
                    )
                )
            }
        }

        return ReceiptData(
            storeName = establishmentStr,
            date = dateStr,
            subtotal = if (subTotalStr.isEmpty() || subTotalStr == "0") totalStr else subTotalStr,
            discount = discountStr,
            tax = taxStr,
            grandTotal = totalStr,
            items = itemsList
        )
    }


    // --- HELPER FUNCTIONS ---

    private suspend fun showError(msg: String) {
        withContext(Dispatchers.Main) {
            progressBar.visibility = View.GONE
            Toast.makeText(this@ResultActivity, msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun displayReceiptData(data: ReceiptData) {
        lastReceiptData = data // Simpan ke variabel global untuk Intent

        findViewById<TextView>(R.id.tv_store_name).text = data.storeName
        // Ambil tanggal saja (YYYY-MM-DD) dari string panjang
        findViewById<TextView>(R.id.tv_transaction_date).text = data.date.split(" ")[0]

        findViewById<TextView>(R.id.tv_subtotal_value).text = formatCurrency(data.subtotal)
        findViewById<TextView>(R.id.tv_diskon_value).text = formatCurrency(data.discount)
        findViewById<TextView>(R.id.tv_pajak_value).text = formatCurrency(data.tax)
        findViewById<TextView>(R.id.tv_grand_total_value).text = formatCurrency(data.grandTotal)

        val rvItems = findViewById<RecyclerView>(R.id.rv_items)
        rvItems.adapter = ReceiptAdapter(data.items)
    }

    private fun formatCurrency(amount: String): String {
        return try {
            // Bersihkan string dari simbol mata uang lain & ambil angkanya
            // TabScanner return "10000.00", kita perlu parse ke Double
            val cleanAmount = amount.replace("[^0-9.]".toRegex(), "")
            if (cleanAmount.isEmpty()) return "Rp 0"

            val num = cleanAmount.toDouble()
            val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale("id", "ID")))
            "Rp ${formatter.format(num)}"
        } catch (e: Exception) {
            amount // Kembalikan teks asli jika gagal parsing
        }
    }
}