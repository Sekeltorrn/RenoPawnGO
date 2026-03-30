package com.example.mobileapppawnshop.backend

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapppawnshop.R
import com.example.mobileapppawnshop.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView

class CustomerDashboardActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.customer_dashboard)

        sessionManager = SessionManager(this)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        val tvGreeting = findViewById<TextView>(R.id.tvGreeting)
        val userName = sessionManager.getUserName() ?: "User"
        tvGreeting.text = "Welcome, $userName"

        // Keep the beautiful UI routing from the GitHub version
        setupBottomNavigation()
        setupClickListeners()

        // Note: The programmatic setOnClickListener for btnLogout was intentionally removed
        // because we are now relying entirely on the Nuclear XML Function below!
    }

    private fun setupClickListeners() {
        findViewById<MaterialCardView>(R.id.btnLoan).setOnClickListener {
            val intent = Intent(this, PawnTicketActiveActivity::class.java)
            startActivity(intent)
        }
        findViewById<MaterialCardView>(R.id.btnAccount).setOnClickListener {
            val intent = Intent(this, AccountUpdateActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Already on Home/Dashboard
                    true
                }
                R.id.navigation_loans -> {
                    val intent = Intent(this, PawnTicketActiveActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_payments -> {
                    val intent = Intent(this, PaymentsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_appointments -> {
                    val intent = Intent(this, AppointmentActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_account -> {
                    val intent = Intent(this, AccountUpdateActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    // --- THE NUCLEAR XML FUNCTION ---
    // Android will forcefully trigger this exact function when the button is tapped.
    fun onLogoutClick(view: View) {
        Toast.makeText(this, "NATIVE CLICK DETECTED!", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LogoutActivity::class.java)
        startActivity(intent)
        finish() // Added finish() so the user can't hit the 'back' button to return here
    }
}