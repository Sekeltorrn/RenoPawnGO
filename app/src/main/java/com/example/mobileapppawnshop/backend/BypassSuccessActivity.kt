package com.example.mobileapppawnshop.backend

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapppawnshop.R
import com.google.android.material.button.MaterialButton

class BypassSuccessActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bypass_success)

        val message = intent.getStringExtra("bypass_message")?.replace("BYPASS: ", "")
        findViewById<TextView>(R.id.tvBypassMessage).text = message

        findViewById<MaterialButton>(R.id.btnProceedToLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
