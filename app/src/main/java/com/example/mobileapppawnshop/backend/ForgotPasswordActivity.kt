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

/**
 * Activity for requesting a password reset
 */
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
            val email = etResetEmail.text.toString()
            if (ValidationUtils.validateEmail(email)) {
                viewModel.requestPasswordReset(email)
            } else {
                etResetEmail.error = "Valid email is required"
            }
        }

        tvBackToLogin.setOnClickListener {
            finish()
        }

        viewModel.resetRequestResult.observe(this) { success ->
            if (success) {
                val intent = Intent(this, ForgotVerifyActivity::class.java)
                intent.putExtra("email", etResetEmail.text.toString())
                startActivity(intent)
            } else {
                Toast.makeText(this, "Failed to send reset link", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
