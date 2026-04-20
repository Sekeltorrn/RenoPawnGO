package com.example.mobileapppawnshop.backend

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
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
 * Activity for user registration
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_layout)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // 1. UNPACK THE SUITCASE: Grab the schema name passed from LoginActivity
        val schemaName = intent.getStringExtra("schema_name") ?: ""

        val etFullName = findViewById<TextInputEditText>(R.id.etFullName)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPhone = findViewById<TextInputEditText>(R.id.etPhone)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val cbTerms = findViewById<CheckBox>(R.id.cbTerms)
        val btnCreateAccount = findViewById<MaterialButton>(R.id.btnCreateAccount)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        btnCreateAccount.setOnClickListener {
            val name = etFullName.text.toString()
            val email = etEmail.text.toString()
            val phone = etPhone.text.toString()
            val pass = etPassword.text.toString()
            val confirmPass = etConfirmPassword.text.toString()

            if (!ValidationUtils.validateEmptyField(name)) {
                etFullName.error = "Name is required"
                return@setOnClickListener
            }

            if (!ValidationUtils.validateEmail(email)) {
                etEmail.error = "Valid email is required"
                return@setOnClickListener
            }

            if (!ValidationUtils.validateEmptyField(phone)) {
                etPhone.error = "Phone number is required"
                return@setOnClickListener
            }

            if (!ValidationUtils.validatePassword(pass)) {
                etPassword.error = "Password must be at least 8 characters"
                return@setOnClickListener
            }

            if (!ValidationUtils.validatePasswordMatch(pass, confirmPass)) {
                etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            if (!cbTerms.isChecked) {
                Toast.makeText(this, "Please agree to terms", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. SEND TO DATABASE: Pass the schemaName as the 5th argument!
            viewModel.register(name, email, phone, pass, schemaName)
        }

        tvLogin.setOnClickListener {
            finish()
        }

        // --- UPDATED NAVIGATION LOGIC ---
        viewModel.registerResult.observe(this) { resultPair ->
            val success = resultPair.first
            val message = resultPair.second
            
            if (success) {
                if (message != null && message.startsWith("BYPASS:")) {
                    // CUSTOMER B: ROUTE TO PROCEED TO LOGIN SCREEN
                    val intent = Intent(this, BypassSuccessActivity::class.java).apply {
                        putExtra("bypass_message", message)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    // CUSTOMER A: ROUTE TO OTP SCREEN
                    val intent = Intent(this, AccountVerifyActivity::class.java).apply {
                        putExtra("email", etEmail.text.toString())
                        putExtra("full_name", etFullName.text.toString())
                        putExtra("schema_name", schemaName) 
                        putExtra("is_from_login", false) 
                    }
                    startActivity(intent)
                    finish() 
                }
            } else {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }
}