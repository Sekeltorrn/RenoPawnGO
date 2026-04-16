package com.example.mobileapppawnshop.backend

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.mobileapppawnshop.R
import com.example.mobileapppawnshop.utils.ValidationUtils
import com.example.mobileapppawnshop.viewmodel.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forgot_password_layout)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        val etResetEmail = findViewById<TextInputEditText>(R.id.etResetEmail)
        val btnSendResetLink = findViewById<MaterialButton>(R.id.btnSendResetLink)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        btnSendResetLink.setOnClickListener {
            val email = etResetEmail.text.toString().trim()
            if (ValidationUtils.validateEmail(email)) {
                // UI State: Disable
                btnSendResetLink.isEnabled = false
                btnSendResetLink.text = "Sending..."
                viewModel.requestPasswordReset(email)
            } else {
                etResetEmail.error = "Valid email is required"
            }
        }

        tvBackToLogin.setOnClickListener {
            finish()
        }

        viewModel.forgotPasswordResult.observe(this) { result ->
            // Re-enable
            btnSendResetLink.isEnabled = true
            btnSendResetLink.text = "Send Reset Link"

            if (result.first) {
                val intent = Intent(this, ForgotVerifyActivity::class.java)
                intent.putExtra("email", etResetEmail.text.toString().trim())
                startActivity(intent)
            } else {
                Toast.makeText(this, result.second, Toast.LENGTH_LONG).show()
            }
        }
    }
}
