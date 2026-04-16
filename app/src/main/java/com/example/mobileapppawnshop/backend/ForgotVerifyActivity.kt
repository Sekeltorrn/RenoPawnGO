package com.example.mobileapppawnshop.backend

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.mobileapppawnshop.R
import com.example.mobileapppawnshop.viewmodel.AuthViewModel
import com.google.android.material.button.MaterialButton

class ForgotVerifyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forgot_verify_layout)

        // 1. Retrieve the email from the intent
        val email = intent.getStringExtra("email") ?: ""

        val otp1 = findViewById<EditText>(R.id.otp1)
        val otp2 = findViewById<EditText>(R.id.otp2)
        val otp3 = findViewById<EditText>(R.id.otp3)
        val otp4 = findViewById<EditText>(R.id.otp4)
        val otp5 = findViewById<EditText>(R.id.otp5)
        val otp6 = findViewById<EditText>(R.id.otp6)

        val btnVerify = findViewById<MaterialButton>(R.id.btnVerify)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        btnVerify.setOnClickListener {
            val code = "${otp1.text}${otp2.text}${otp3.text}${otp4.text}${otp5.text}${otp6.text}"
            // 2. Check if the code is exactly 6 digits
            if (code.length == 6) {
                // IMMEDIATELY start ResetPasswordActivity passing BOTH email and code
                val intent = Intent(this, ResetPasswordActivity::class.java).apply {
                    putExtra("email", email)
                    putExtra("code", code)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please enter 6-digit code", Toast.LENGTH_SHORT).show()
            }
        }

        tvBackToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }
}