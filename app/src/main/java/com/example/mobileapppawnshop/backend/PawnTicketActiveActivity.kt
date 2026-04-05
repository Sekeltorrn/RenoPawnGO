package com.example.mobileapppawnshop.backend

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileapppawnshop.R
import com.example.mobileapppawnshop.data.model.PawnTicket
import com.example.mobileapppawnshop.data.model.TicketResponse
import com.example.mobileapppawnshop.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PawnTicketActiveActivity : AppCompatActivity() {

    private lateinit var cardTabActive: MaterialCardView
    private lateinit var cardTabRenewed: MaterialCardView
    private lateinit var cardTabRedeemed: MaterialCardView
    private lateinit var cardTabExpired: MaterialCardView

    private lateinit var btnTabActive: TextView
    private lateinit var btnTabRenewed: TextView
    private lateinit var btnTabRedeemed: TextView
    private lateinit var btnTabExpired: TextView

    private lateinit var infoIcon: ImageView
    private lateinit var infoTitle: TextView
    private lateinit var infoDescription: TextView

    private lateinit var rvActiveTickets: RecyclerView
    private lateinit var ticketAdapter: PawnTicketAdapter
    private var ticketsList: List<PawnTicket> = emptyList()
    private var currentTab: Tab = Tab.ACTIVE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pawn_ticket_active)

        // Initialize Views
        cardTabActive = findViewById(R.id.cardTabActive)
        cardTabRenewed = findViewById(R.id.cardTabRenewed)
        cardTabRedeemed = findViewById(R.id.cardTabRedeemed)
        cardTabExpired = findViewById(R.id.cardTabExpired)

        btnTabActive = findViewById(R.id.btnTabActive)
        btnTabRenewed = findViewById(R.id.btnTabRenewed)
        btnTabRedeemed = findViewById(R.id.btnTabRedeemed)
        btnTabExpired = findViewById(R.id.btnTabExpired)

        infoIcon = findViewById(R.id.infoIcon)
        infoTitle = findViewById(R.id.infoTitle)
        infoDescription = findViewById(R.id.infoDescription)

        // Initialize RecyclerView
        rvActiveTickets = findViewById(R.id.rvActiveTickets)
        rvActiveTickets.layoutManager = LinearLayoutManager(this)
        ticketAdapter = PawnTicketAdapter(emptyList())
        rvActiveTickets.adapter = ticketAdapter

        // Setup Tab Clicks
        cardTabActive.setOnClickListener { updateUI(Tab.ACTIVE) }
        cardTabRenewed.setOnClickListener { updateUI(Tab.RENEWED) }
        cardTabRedeemed.setOnClickListener { updateUI(Tab.REDEEMED) }
        cardTabExpired.setOnClickListener { updateUI(Tab.EXPIRED) }
        btnTabActive.setOnClickListener { updateUI(Tab.ACTIVE) }
        btnTabRenewed.setOnClickListener { updateUI(Tab.RENEWED) }
        btnTabRedeemed.setOnClickListener { updateUI(Tab.REDEEMED) }
        btnTabExpired.setOnClickListener { updateUI(Tab.EXPIRED) }

        // Bottom Navigation
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

        // Initial Fetch
        updateUI(Tab.ACTIVE)
    }

    private enum class Tab { ACTIVE, RENEWED, REDEEMED, EXPIRED }

    private fun updateUI(tab: Tab) {
        currentTab = tab

        val tabs = listOf(cardTabActive, cardTabRenewed, cardTabRedeemed, cardTabExpired)
        val texts = listOf(btnTabActive, btnTabRenewed, btnTabRedeemed, btnTabExpired)

        for (i in tabs.indices) {
            tabs[i].setCardBackgroundColor(Color.TRANSPARENT)
            tabs[i].cardElevation = 0f
            texts[i].setTextColor(Color.parseColor("#64748b"))
        }

        // Apply tab styles
        val statusParam = when (tab) {
            Tab.ACTIVE -> {
                cardTabActive.setCardBackgroundColor(Color.WHITE)
                cardTabActive.cardElevation = 4f
                btnTabActive.setTextColor(Color.parseColor("#00327d"))
                "active"
            }
            Tab.RENEWED -> {
                cardTabRenewed.setCardBackgroundColor(Color.WHITE)
                cardTabRenewed.cardElevation = 4f
                btnTabRenewed.setTextColor(Color.parseColor("#00327d"))
                "renewed"
            }
            Tab.REDEEMED -> {
                cardTabRedeemed.setCardBackgroundColor(Color.WHITE)
                cardTabRedeemed.cardElevation = 4f
                btnTabRedeemed.setTextColor(Color.parseColor("#00327d"))
                "redeemed"
            }
            Tab.EXPIRED -> {
                cardTabExpired.setCardBackgroundColor(Color.WHITE)
                cardTabExpired.cardElevation = 4f
                btnTabExpired.setTextColor(Color.parseColor("#00327d"))
                "expired"
            }
        }

        // Fetch data for the selected status
        fetchTicketsByStatus(statusParam)
    }

    private fun fetchTicketsByStatus(status: String) {
        val sessionManager = SessionManager(this)
        val realCustomerId = sessionManager.getCustomerId() ?: ""
        val realShopCode = sessionManager.getShopCode() ?: ""

        if (realCustomerId.isEmpty() || realShopCode.isEmpty()) {
            Toast.makeText(this, "Session Error: Missing Data", Toast.LENGTH_SHORT).show()
            return
        }

        android.util.Log.d("DEBUG_LOANS", "Fetching loans - Customer ID: $realCustomerId | Shop Code: $realShopCode | Status: $status")

        ApiClient.apiService.getActiveTickets(realCustomerId, realShopCode, status).enqueue(object : Callback<TicketResponse> {
            override fun onResponse(call: Call<TicketResponse>, response: Response<TicketResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    ticketsList = response.body()?.tickets ?: emptyList()
                    
                    // NEW: Ensure tickets are properly sorted by Due Date before pushing to UI
                    val sortedTickets = ticketsList.sortedBy { it.due_date }
                    ticketAdapter.updateData(sortedTickets)

                    if (sortedTickets.isNotEmpty()) {
                        rvActiveTickets.visibility = View.VISIBLE
                        findViewById<View>(R.id.infoContainer).visibility = View.GONE
                    } else {
                        rvActiveTickets.visibility = View.GONE
                        findViewById<View>(R.id.infoContainer).visibility = View.VISIBLE
                        showEmptyState(status)
                    }
                } else {
                    rvActiveTickets.visibility = View.GONE
                    findViewById<View>(R.id.infoContainer).visibility = View.VISIBLE
                    showEmptyState(status)
                }
            }

            override fun onFailure(call: Call<TicketResponse>, t: Throwable) {
                Toast.makeText(this@PawnTicketActiveActivity, "Network Error.", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun showEmptyState(status: String) {
        when (status) {
            "active" -> {
                infoIcon.setImageResource(android.R.drawable.ic_menu_agenda)
                infoTitle.text = "No Active Tickets"
                infoDescription.text = "You don't have any active pawn tickets at the moment."
            }
            "renewed" -> {
                infoIcon.setImageResource(android.R.drawable.ic_menu_revert)
                infoTitle.text = "No Renewed Tickets"
                infoDescription.text = "Your history of renewed pawn tickets will appear here."
            }
            "redeemed" -> {
                infoIcon.setImageResource(android.R.drawable.ic_menu_save)
                infoTitle.text = "No Redeemed Tickets"
                infoDescription.text = "Tickets you have fully paid and claimed will be listed here."
            }
            "expired" -> {
                infoIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                infoTitle.text = "No Expired Tickets"
                infoDescription.text = "Tickets that have passed the maturity and grace period will appear here."
            }
        }
    }
}