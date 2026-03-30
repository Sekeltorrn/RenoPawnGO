package com.example.mobileapppawnshop.backend

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapppawnshop.R
import com.google.android.material.button.MaterialButton

/**
 * Activity for displaying dynamic success confirmation messages
 */
class SuccessConfirmationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.success_confirmation_layout)

        // 1. Find the views in the layout
        val tvTitle = findViewById<TextView>(R.id.tvSuccessTitle)
        val tvMessage = findViewById<TextView>(R.id.tvSuccessMessage)
        val btnProceedToLogin = findViewById<MaterialButton>(R.id.btnProceedToLogin)

        // 2. Retrieve the custom messages passed from AccountVerifyActivity
        // If no message is passed, it defaults to a generic "Success!"
        val title = intent.getStringExtra("message") ?: "Success!"
        val subMessage = intent.getStringExtra("sub_message") ?: "Your request has been processed successfully."

        // 3. Update the UI text dynamically
        tvTitle.text = title
        tvMessage.text = subMessage

        btnProceedToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}