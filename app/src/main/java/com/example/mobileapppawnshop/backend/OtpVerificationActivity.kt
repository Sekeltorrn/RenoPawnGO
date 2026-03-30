package com.example.mobileapppawnshop.backend

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapppawnshop.R

/**
 * Activity for OTP verification.
 * Created to resolve symbol issues in activity_otp_verification.xml
 */
class OtpVerificationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_verification)
    }
}
