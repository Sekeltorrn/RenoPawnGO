package com.example.mobileapppawnshop.backend

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.mobileapppawnshop.R
import com.example.mobileapppawnshop.utils.SessionManager
import com.example.mobileapppawnshop.viewmodel.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var viewModel: AuthViewModel
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reset_password_layout)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        sessionManager = SessionManager(this)

        // 1. Retrieve email, code, and session data
        val email = intent.getStringExtra("email") ?: ""
        val code = intent.getStringExtra("code") ?: ""
        val schemaName = sessionManager.getSchemaName() ?: ""

        val etNewPassword = findViewById<TextInputEditText>(R.id.etNewPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val btnUpdatePassword = findViewById<MaterialButton>(R.id.btnUpdatePassword)

        btnUpdatePassword.setOnClickListener {
            val newPass = etNewPassword.text.toString().trim()
            val confirmPass = etConfirmPassword.text.toString().trim()

            // 2. Validate password
            if (newPass.length < 6) {
                etNewPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            // 3. UI State: Updating...
            btnUpdatePassword.isEnabled = false
            btnUpdatePassword.text = "Updating..."
            viewModel.updatePassword(email, code, newPass, schemaName)
        }

        // 4. Observe the result
        viewModel.resetPasswordResult.observe(this) { resultPair ->
            val btnUpdate = findViewById<MaterialButton>(R.id.btnUpdatePassword)
            btnUpdate.isEnabled = true
            btnUpdate.text = "Update Password"

            if (resultPair.first) {
                // STEP 1: DESTROY THE LOCAL SESSION IMMEDIATELY
                sessionManager.logoutUser() 

                // STEP 2: ROUTE TO SUCCESS SCREEN
                val intent = Intent(this, SuccessConfirmationActivity::class.java).apply {
                    putExtra("message", "Password Updated!")
                    putExtra("sub_message", "Security protocol complete. Please log in with your new password.")
                }
                startActivity(intent)
                
                // STEP 3: WIPE THE ACTIVITY STACK
                finishAffinity() 
            } else {
                Toast.makeText(this, resultPair.second, Toast.LENGTH_LONG).show()
            }
        }
    }
}
