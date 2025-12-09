package com.example.billup

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Langsung pindah ke MainActivity setelah splash
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
