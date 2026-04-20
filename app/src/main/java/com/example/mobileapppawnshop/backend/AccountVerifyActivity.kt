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
    private var resendTimer: android.os.CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.account_verify_layout)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        sessionManager = SessionManager(this)

        val tvEmailInfo = findViewById<TextView>(R.id.tvEmailInfo)

        // 1. Grab parameters from the previous screen
        val identifier = intent.getStringExtra("identifier") ?: intent.getStringExtra("email") ?: ""
        val loginType = intent.getStringExtra("login_type") ?: "email"
        val fullName = intent.getStringExtra("full_name") ?: "Customer"
        val customerId = intent.getStringExtra("customer_id") ?: ""
        val isFromLogin = intent.getBooleanExtra("is_from_login", false)
        val schemaName = intent.getStringExtra("schema_name") ?: sessionManager.getSchemaName() ?: ""

        tvEmailInfo.text = "Enter the 6-digit code sent to \n$identifier"

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
                if (isFromLogin) {
                    if (loginType == "email") {
                        // Standard Supabase Flow
                        viewModel.verifyLoginOtp(identifier, code, schemaName)
                    } else {
                        // Walk-In Mock SMS Flow
                        viewModel.verifyMockSms(identifier, code, schemaName)
                    }
                } else {
                    viewModel.verifyRegisterOtp(identifier, code, schemaName)
                }
            } else {
                Toast.makeText(this, "Please enter 6-digit code", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            finish()
        }

        // OBSERVER A: Register OTP
        viewModel.verifyRegisterResult.observe(this) { resultPair ->
            if (resultPair.first) {
                // Registration Verified! Route to SuccessConfirmationActivity
                val intent = Intent(this, SuccessConfirmationActivity::class.java).apply {
                    putExtra("message", "Email Verified!")
                    putExtra("sub_message", "Your account is active. Please log in with your credentials.")
                }
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, resultPair.second, Toast.LENGTH_SHORT).show()
            }
        }

        // OBSERVER B: Login OTP
        viewModel.verifyLoginResult.observe(this) { resultPair ->
            val user = resultPair.first
            if (user != null) {
                // SECURE LOGIN: Save session data now!
                sessionManager.saveUserLoginFull(user.email, user.fullName, user.id)
                sessionManager.saveKycStatus(user.kycStatus)
                
                // Route to animated Splash Screen
                val intent = Intent(this, LoginSuccessSplashActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, resultPair.second, Toast.LENGTH_SHORT).show()
            }
        }

        // Dynamically update UI based on flow
        if (isFromLogin) {
            btnVerify.text = "Verify & Login"
            tvEmailInfo.text = "Enter the 6-digit login code sent to \n$identifier"
        } else {
            btnVerify.text = "Verify & Create Account"
            tvEmailInfo.text = "Enter the 6-digit registration code sent to \n$identifier"
        }

        val tvResend = findViewById<TextView>(R.id.tvResendCode)

        // 1. START THE TIMER THE MOMENT THE SCREEN OPENS
        startResendTimer(tvResend)

        // 2. SETUP THE CLICK LISTENER
        tvResend.setOnClickListener {
            // Lock UI while talking to the server
            tvResend.isEnabled = false
            tvResend.isClickable = false
            tvResend.text = "Sending..."
            tvResend.alpha = 0.5f 
            
            val type = if (isFromLogin) "login" else "signup"
            viewModel.resendOtp(identifier, type)
        }

        // 3. HANDLE SERVER RESPONSE
        viewModel.resendResult.observe(this) { result ->
            if (result.first) {
                // Success: Restart the 40s countdown
                startResendTimer(tvResend)
            } else {
                // Fail: Unlock so they can try again instantly
                tvResend.isEnabled = true
                tvResend.isClickable = true
                tvResend.text = "Resend Verification Code"
                tvResend.alpha = 1.0f
                Toast.makeText(this, result.second, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startResendTimer(tvResend: TextView) {
        // LOCK THE BUTTON DOWN
        tvResend.isEnabled = false
        tvResend.isClickable = false
        tvResend.alpha = 0.5f 

        resendTimer?.cancel() 
        // INCREASED TO 60 SECONDS TO SYNC WITH SERVER RATE LIMITS
        resendTimer = object : android.os.CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                tvResend.text = "Resend code in ${seconds}s"
            }

            override fun onFinish() {
                // UNLOCK THE BUTTON
                tvResend.isEnabled = true
                tvResend.isClickable = true
                tvResend.text = "Resend Verification Code"
                tvResend.alpha = 1.0f 
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        resendTimer?.cancel()
    }
}