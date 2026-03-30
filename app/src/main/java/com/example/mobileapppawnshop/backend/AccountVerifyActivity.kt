package com.example.mobileapppawnshop.backend

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.mobileapppawnshop.R
import com.example.mobileapppawnshop.utils.SessionManager
import com.example.mobileapppawnshop.viewmodel.AuthViewModel
import com.google.android.material.button.MaterialButton

/**
 * Activity for account verification after registration OR 2FA Login
 */
class AccountVerifyActivity : AppCompatActivity() {

    private lateinit var viewModel: AuthViewModel
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.account_verify_layout)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        sessionManager = SessionManager(this)

        val tvEmailInfo = findViewById<TextView>(R.id.tvEmailInfo)

        // 1. Grab parameters from the previous screen
        val email = intent.getStringExtra("email") ?: ""
        val fullName = intent.getStringExtra("full_name") ?: "Customer"
        val customerId = intent.getStringExtra("customer_id") ?: ""
        val isFromLogin = intent.getBooleanExtra("is_from_login", false)

        tvEmailInfo.text = "Enter the 6-digit code sent to \n$email"

        val otp1 = findViewById<EditText>(R.id.otp1)
        val otp2 = findViewById<EditText>(R.id.otp2)
        val otp3 = findViewById<EditText>(R.id.otp3)
        val otp4 = findViewById<EditText>(R.id.otp4)
        val otp5 = findViewById<EditText>(R.id.otp5)
        val otp6 = findViewById<EditText>(R.id.otp6)

        val btnVerify = findViewById<MaterialButton>(R.id.btnVerify)
        val btnCancel = findViewById<MaterialButton>(R.id.btnCancel)

        btnVerify.setOnClickListener {
            val code = "${otp1.text}${otp2.text}${otp3.text}${otp4.text}${otp5.text}${otp6.text}"

            if (code.length == 6) {
                /**
                 * CRITICAL FIX:
                 * For new registrations, type MUST be "signup" so verify.php
                 * triggers the SQL INSERT into your customers table.
                 */
                val verificationType = if (isFromLogin) "magiclink" else "signup"

                viewModel.verifyCode(email, code, verificationType)
            } else {
                Toast.makeText(this, "Please enter 6-digit code", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            finish()
        }

        // --- NAVIGATION LOGIC (The Traffic Cop) ---
        viewModel.verificationResult.observe(this) { success ->
            if (success) {
                if (isFromLogin) {
                    // Path A: 2FA Login Success
                    // --- THE FIX: Save the FULL session including the Customer ID ---
                    sessionManager.saveUserLoginFull(email, fullName, customerId)
                    
                    // FEEDBACK: Show success toast
                    Toast.makeText(this, "Login Verified!", Toast.LENGTH_SHORT).show()
                    
                    // ANIMATION: Go to Splash screen for the "Animated Dashboard" entry
                    val intent = Intent(this, LoginSuccessSplashActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    /**
                     * Path B: NEW REGISTRATION SUCCESS
                     */
                    val intent = Intent(this, SuccessConfirmationActivity::class.java)
                    intent.putExtra("message", "Registration Successful!")
                    intent.putExtra("sub_message", "Your account is now pending admin approval. Please wait for the shop to verify your details.")
                    startActivity(intent)
                    finish()
                }
            } else {
                Toast.makeText(this, "Invalid or expired code. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}