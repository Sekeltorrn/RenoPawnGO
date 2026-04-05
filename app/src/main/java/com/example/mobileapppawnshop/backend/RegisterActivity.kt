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
        viewModel.registerResult.observe(this) { success ->
            if (success) {
                // SUCCESS: Grab the email they just typed
                val emailStr = etEmail.text.toString()

                /* TODO: AUTH BYPASS ACTIVE. Uncomment this block when Email OTP is re-enabled.
                Toast.makeText(this, "Registration successful! Please verify your email.", Toast.LENGTH_SHORT).show()

                // Open the Verification Screen and hand it the email
                val intent = Intent(this, AccountVerifyActivity::class.java).apply {
                    putExtra("email", emailStr)
                }
                startActivity(intent)
                finish() // Close the register screen so they can't go "back" to it
                */

                // TEMPORARY BYPASS NAVIGATION
                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, SuccessConfirmationActivity::class.java).apply {
                    putExtra("message", "Registration Successful!")
                    putExtra("sub_message", "Your account is now pending admin approval. Please wait for the shop to verify your details.")
                }
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Registration failed or Email already exists", Toast.LENGTH_LONG).show()
            }
        }
    }
}