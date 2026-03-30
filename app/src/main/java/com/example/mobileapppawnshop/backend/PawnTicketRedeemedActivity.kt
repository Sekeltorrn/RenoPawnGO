package com.example.mobileapppawnshop.backend

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapppawnshop.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class PawnTicketRedeemedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pawn_ticket_redeemed)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.navigation_loans

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, CustomerDashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_loans -> true
                R.id.navigation_payments -> {
                    startActivity(Intent(this, PaymentsActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_appointments -> {
                    startActivity(Intent(this, AppointmentActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_account -> {
                    startActivity(Intent(this, AccountUpdateActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        findViewById<ImageButton>(R.id.btnLogout).setOnClickListener {
            startActivity(Intent(this, LogoutActivity::class.java))
        }

        // Tab Navigation
        findViewById<View>(R.id.tabActive)?.setOnClickListener {
            startActivity(Intent(this, PawnTicketActiveActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.tabRenewed)?.setOnClickListener {
            startActivity(Intent(this, PawnTicketRenewedActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.tabExpired)?.setOnClickListener {
            startActivity(Intent(this, PawnTicketExpiredActivity::class.java))
            finish()
        }
    }
}
