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
        
        // Fetch active tickets for the preview pane
        fetchDashboardTickets()
    }

    private fun fetchDashboardTickets() {
        val realCustomerId = sessionManager.getCustomerId() ?: ""
        val realShopCode = sessionManager.getShopCode() ?: ""

        val rvDashboardTickets = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvDashboardTickets)
        val tvEmptyDashboard = findViewById<TextView>(R.id.tvEmptyDashboard)
        rvDashboardTickets.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        if (realCustomerId.isNotEmpty() && realShopCode.isNotEmpty()) {
            ApiClient.apiService.getActiveTickets(realCustomerId, realShopCode, "active").enqueue(object : retrofit2.Callback<com.example.mobileapppawnshop.data.model.TicketResponse> {
                override fun onResponse(call: retrofit2.Call<com.example.mobileapppawnshop.data.model.TicketResponse>, response: retrofit2.Response<com.example.mobileapppawnshop.data.model.TicketResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val ticketsList = response.body()?.tickets ?: emptyList()
                        val sortedTickets = ticketsList.sortedBy { it.due_date }.take(3)
                        
                        if (sortedTickets.isNotEmpty()) {
                            rvDashboardTickets.adapter = PawnTicketAdapter(sortedTickets, true)
                            rvDashboardTickets.visibility = View.VISIBLE
                            tvEmptyDashboard.visibility = View.GONE
                        } else {
                            rvDashboardTickets.visibility = View.GONE
                            tvEmptyDashboard.visibility = View.VISIBLE
                        }
                    } else {
                        rvDashboardTickets.visibility = View.GONE
                        tvEmptyDashboard.visibility = View.VISIBLE
                    }
                }

                override fun onFailure(call: retrofit2.Call<com.example.mobileapppawnshop.data.model.TicketResponse>, t: Throwable) {
                    rvDashboardTickets.visibility = View.GONE
                    tvEmptyDashboard.visibility = View.VISIBLE
                }
            })
        } else {
            tvEmptyDashboard.visibility = View.VISIBLE
        }
    }

    private fun setupClickListeners() {
        findViewById<MaterialCardView>(R.id.btnLoan).setOnClickListener {
            val intent = Intent(this, PawnTicketActiveActivity::class.java)
            startActivity(intent)
        }
        findViewById<MaterialCardView>(R.id.btnPayments).setOnClickListener {
            val intent = Intent(this, PaymentsActivity::class.java)
            startActivity(intent)
        }
        findViewById<MaterialCardView>(R.id.btnAppointments).setOnClickListener {
            val intent = Intent(this, AppointmentActivity::class.java)
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