package com.example.mobileapppawnshop.backend

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.mobileapppawnshop.R
import com.example.mobileapppawnshop.utils.SessionManager
import com.example.mobileapppawnshop.utils.ValidationUtils
import com.example.mobileapppawnshop.data.model.User
import com.example.mobileapppawnshop.viewmodel.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    private lateinit var viewModel: AuthViewModel
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // --- STRICT ROUTING LOGIC ---
        // 1. Check if User is already logged in
        if (sessionManager.isUserLoggedIn()) {
            startActivity(Intent(this, CustomerDashboardActivity::class.java))
            finish()
            return
        }

        // 2. Check if Shop Code has been set
        val savedShopCode = sessionManager.getShopCode()
        if (savedShopCode.isNullOrEmpty()) {
            startActivity(Intent(this, ShopCodeActivity::class.java))
            finish()
            return
        }

        // 3. Otherwise, proceed to Login UI
        setContentView(R.layout.login_layout)

        // Retrieve schema/shop details from Intent (if coming from ShopCodeActivity)
        // or from SessionManager (if returning to the app)
        val schemaName = intent.getStringExtra("schema_name") ?: sessionManager.getSchemaName() ?: ""
        val shopName = intent.getStringExtra("shop_name") ?: sessionManager.getShopName() ?: "the shop"

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        val etIdentifier = findViewById<TextInputEditText>(R.id.etIdentifier)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnSignIn = findViewById<MaterialButton>(R.id.btnSignIn)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        btnSignIn.setOnClickListener {
            val identifier = etIdentifier.text.toString().trim()
            val pass = etPassword.text.toString()

            if (!ValidationUtils.validateEmptyField(identifier)) {
                etIdentifier.error = "Identifier cannot be empty"
                return@setOnClickListener
            }

            if (!ValidationUtils.validateEmptyField(pass)) {
                etPassword.error = "Password cannot be empty"
                return@setOnClickListener
            }

            if (identifier.contains("@")) {
                viewModel.loginAuth(identifier, pass, schemaName)
            } else {
                viewModel.loginPhone(identifier, pass, schemaName)
            }
        }

        tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java).apply {
                putExtra("schema_name", schemaName)
                putExtra("shop_name", shopName)
            }
            startActivity(intent)
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        val tvChangeShop = findViewById<TextView>(R.id.tvChangeShop)
        tvChangeShop.setOnClickListener {
            sessionManager.logoutUser() // This now clears the shop code explicitly
            startActivity(Intent(this, ShopCodeActivity::class.java))
            finish()
        }


        viewModel.loginAuthResult.observe(this) { resultPair ->
            val success = resultPair.first
            val errorMessage = resultPair.second

            if (success) {
                // PASSWORD CORRECT -> ROUTE TO OTP SCREEN
                val identifier = etIdentifier.text.toString().trim()
                val intent = Intent(this, AccountVerifyActivity::class.java).apply {
                    putExtra("identifier", identifier)
                    putExtra("schema_name", schemaName) 
                    putExtra("is_from_login", true) 
                    putExtra("login_type", if (identifier.contains("@")) "email" else "phone")
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.loginPhoneResult.observe(this) { resultPair ->
            val success = resultPair.first
            val message = resultPair.second // mock_otp or error

            if (success) {
                val identifier = etIdentifier.text.toString().trim()
                Toast.makeText(this, "MOCK SMS: $message", Toast.LENGTH_LONG).show()
                val intent = Intent(this, AccountVerifyActivity::class.java).apply {
                    putExtra("identifier", identifier)
                    putExtra("schema_name", schemaName) 
                    putExtra("is_from_login", true) 
                    putExtra("login_type", if (identifier.contains("@")) "email" else "phone")
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }
}