package com.example.billup

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build // <-- IMPORT INI
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.billup.Contact
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.ArrayList

private val API_KEY = "AIzaSyD0-xdnhf0ezR1lLPnw_U_ZtIrHrQQVgEI"

class ResultActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var contentLayout: View
    private var selectedContacts: ArrayList<Contact>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        progressBar = findViewById(R.id.progressBar)
        contentLayout = findViewById(R.id.contentLayout)

        contentLayout.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        // --- INI ADALAH BAGIAN YANG DIPERBARUI ---
        selectedContacts = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("SELECTED_CONTACTS", ArrayList::class.java) as? ArrayList<Contact>
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("SELECTED_CONTACTS") as? ArrayList<Contact>
        }
        // ------------------------------------------

        val photoPath = intent.getStringExtra("PHOTO_PATH")

        if (photoPath != null) {
            val bitmap = BitmapFactory.decodeFile(photoPath)
            if (bitmap != null) {
                recognizeText(bitmap)
            } else {
                Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
        } else {
            Toast.makeText(this, "Path gambar tidak ditemukan.", Toast.LENGTH_LONG).show()
            progressBar.visibility = View.GONE
        }


        findViewById<Button>(R.id.btn_retake).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btn_confirm).setOnClickListener {

        }
    }

    private fun recognizeText(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { ocrText ->
                val fullText = ocrText.text ?: ""

                if (fullText.isNotBlank()) {
                    parseReceiptWithGemini(fullText)
                } else {
                    Toast.makeText(this, "Tidak ada teks yang terdeteksi", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to recognize text: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                progressBar.visibility = View.GONE
            }
    }

    private fun parseReceiptWithGemini(recognizedText: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prompt = """
                Kamu adalah asisten yang ahli membaca struk belanja Indonesia.

                Teks struk (hasil OCR):
                ---
                $recognizedText
                ---

                Tugas:
                1. Abaikan teks yang bukan rincian belanja.
                2. Ambil hanya bagian yang biasanya ada di invoice:
                    - Nama toko
                    - Tanggal dan/atau jam transaksi
                    - Daftar item belanja (nama barang, qty, harga satuan, total per item)
                    - Subtotal
                    - Diskon (jika ada, baik berupa potongan harga atau diskun item)
                    - Pajak / PPN (jika ada)
                    - Total bayar
                3. Tampilkan hasil dalam format teks yang dipisahkan oleh delimiter | (JANGAN JSON):

                // Format Header: NamaToko|Tanggal|Subtotal|Diskon|Pajak|Total
                NamaToko|Tanggal|Subtotal|Diskon|Pajak|Total

                // Format Item (ulangi untuk setiap item):
                ITEM|NAMA BARANG|QTY|HARGA SATUAN|TOTAL PER ITEM

                Jika suatu nilai tidak ditemukan di teks, isi dengan tanda "-".
                Kirim HANYA teks yang dipisahkan delimiter tersebut, tanpa penjelasan lain.
                """.trimIndent()

                val requestJson = JSONObject()
                requestJson.put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })

                val url = URL(
                    "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent?key=$API_KEY"
                )

                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.doOutput = true

                val os = OutputStreamWriter(conn.outputStream, Charsets.UTF_8)
                os.write(requestJson.toString())
                os.flush()
                os.close()

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStream = conn.inputStream.bufferedReader().use { it.readText() }
                    val responseJson = JSONObject(responseStream)

                    val candidates = responseJson.optJSONArray("candidates")
                    val invoiceText = if (candidates != null && candidates.length() > 0) {
                        val content = candidates
                            .getJSONObject(0)
                            .getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        parts.getJSONObject(0).getString("text")
                    } else {
                        "Tidak ada respons dari Gemini."
                    }

                    withContext(Dispatchers.Main) {
                        val receiptData = parseGeminiOutput(invoiceText)
                        displayReceiptData(receiptData)

                        progressBar.visibility = View.GONE
                        contentLayout.visibility = View.VISIBLE
                    }
                } else {
                    val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        Toast.makeText(
                            this@ResultActivity,
                            "Gemini API error ($responseCode): $err",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                conn.disconnect()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ResultActivity,
                        "Parsing error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    private fun parseGeminiOutput(rawText: String): ReceiptData {
        val lines = rawText.trim().split("\n")
        var headerLine: String? = null
        val itemLines = mutableListOf<String>()

        for (line in lines) {
            if (line.startsWith("ITEM|")) {
                itemLines.add(line.substring(5))
            } else if (headerLine == null && line.contains("|")) {
                headerLine = line
            }
        }

        val headerParts = headerLine?.split("|") ?: listOf("-", "-", "-", "-", "-", "-")

        val items = itemLines.map { itemLine ->
            val parts = itemLine.split("|")
            ReceiptItem(
                name = parts.getOrElse(0) { "-" },
                quantity = parts.getOrElse(1) { "0" }.toIntOrNull() ?: 0,
                unitPrice = parts.getOrElse(2) { "-" },
                total = parts.getOrElse(3) { "-" }
            )
        }

        return ReceiptData(
            storeName = headerParts.getOrElse(0) { "Nama Toko Tidak Ditemukan" },
            date = headerParts.getOrElse(1) { "Tanggal Tidak Ditemukan" },
            subtotal = headerParts.getOrElse(2) { "0" },
            discount = headerParts.getOrElse(3) { "0" },
            tax = headerParts.getOrElse(4) { "0" },
            grandTotal = headerParts.getOrElse(5) { "0" },
            items = items
        )
    }

    private fun displayReceiptData(data: ReceiptData) {
        findViewById<TextView>(R.id.tv_store_name).text = data.storeName
        findViewById<TextView>(R.id.tv_transaction_date).text = data.date

        findViewById<TextView>(R.id.tv_subtotal_value).text = formatCurrency(data.subtotal)

        val diskonRaw = data.discount.replace("[^0-9]".toRegex(), "")
        val diskonValue = if (diskonRaw.isEmpty() || diskonRaw == "0") "0,00" else "-${formatCurrency(diskonRaw).removePrefix("Rp ")}"
        findViewById<TextView>(R.id.tv_diskon_value).text = diskonValue

        findViewById<TextView>(R.id.tv_pajak_value).text = formatCurrency(data.tax)
        findViewById<TextView>(R.id.tv_grand_total_value).text = formatCurrency(data.grandTotal)

        val rvItems = findViewById<RecyclerView>(R.id.rv_items)
        rvItems.adapter = ReceiptAdapter(data.items)
    }

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