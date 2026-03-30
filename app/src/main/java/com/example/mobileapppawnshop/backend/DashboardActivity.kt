package com.example.mobileapppawnshop.backend

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapppawnshop.R
import com.example.mobileapppawnshop.utils.SessionManager

/**
 * Activity for the Customer Dashboard
 */
class DashboardActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.customer_dashboard)

        sessionManager = SessionManager(this)

        val tvGreeting = findViewById<TextView>(R.id.tvGreeting)
        val btnLogout = findViewById<ImageButton>(R.id.btnLogout)

        val userName = sessionManager.getUserName() ?: "User"
        tvGreeting.text = "Good Morning, $userName"

        btnLogout.setOnClickListener {
            // Navigate to the new LogoutActivity
            val intent = Intent(this, LogoutActivity::class.java)
            startActivity(intent)
        }
    }
}
