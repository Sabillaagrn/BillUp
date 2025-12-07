package com.example.billup

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.billup.Contact
import com.example.billup.ContactAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.ArrayList

class MainActivity : AppCompatActivity() {

    private lateinit var rvContacts: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var fabScan: FloatingActionButton

    private lateinit var contactAdapter: ContactAdapter
    private val contactList = mutableListOf<Contact>()

    private var currentPhotoPath: String? = null

    private val requestContactsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                loadContacts()
            } else {
                Toast.makeText(this, "Izin baca kontak ditolak", Toast.LENGTH_SHORT).show()
            }
        }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                captureImage()
            } else {
                Toast.makeText(this, "Izin Kamera ditolak", Toast.LENGTH_SHORT).show()
            }
        }

    private lateinit var takePictureLauncher: ActivityResultLauncher<android.net.Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvContacts = findViewById(R.id.rv_contacts)
        etSearch = findViewById(R.id.et_search)
        fabScan = findViewById(R.id.fab_scan)

        setupRecyclerView()
        checkPermissionAndLoadContacts()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                contactAdapter.filter.filter(s)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                currentPhotoPath?.let { path ->
                    val selectedContacts = contactAdapter.getSelectedContacts()
                    val intent = Intent(this, ResultActivity::class.java).apply {
                        putExtra("PHOTO_PATH", path)
                        putExtra("SELECTED_CONTACTS", ArrayList(selectedContacts))
                    }
                    startActivity(intent)
                }
            }
        }

        fabScan.setOnClickListener {
            val selected = contactAdapter.getSelectedContacts()
            if (selected.isEmpty()) {
                Toast.makeText(this, "Pilih minimal 1 kontak", Toast.LENGTH_SHORT).show()
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun setupRecyclerView() {
        contactAdapter = ContactAdapter(contactList)
        rvContacts.layoutManager = LinearLayoutManager(this)
        rvContacts.adapter = contactAdapter
    }

    private fun checkPermissionAndLoadContacts() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                loadContacts()
            }

            else -> {
                requestContactsPermission.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    private fun loadContacts() {
        contactList.clear()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val id = it.getString(idColumn)
                val name = it.getString(nameColumn)
                val number = it.getString(numberColumn)

                if (!contactList.any { c -> c.id == id }) {
                    contactList.add(Contact(id, name, number))
                }
            }
        }

        contactAdapter = ContactAdapter(contactList)
        rvContacts.adapter = contactAdapter
    }

    private fun createImageFile(): File {
        val timestamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timestamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun captureImage() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            Toast.makeText(this, "Error occurred while creating the file", Toast.LENGTH_SHORT)
                .show()
            null
        }
        photoFile?.also {
            val photoUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                it
            )
            takePictureLauncher.launch(photoUri)
        }
    }
}