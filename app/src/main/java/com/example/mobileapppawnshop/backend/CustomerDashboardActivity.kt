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
        // Setup UI routing
        setupBottomNavigation()
        setupTopBar()
        setupClickListeners()
        
        // Fetch data
        fetchDashboardTickets()
        checkKycStatus()
        checkEmailSecurityLock() // TRIGGER ON APP LAUNCH
    }

    private fun checkKycStatus() {
        val customerId = sessionManager.getCustomerId() ?: ""
        val tenantSchema = sessionManager.getSchemaName() ?: ""

        if (customerId.isEmpty() || tenantSchema.isEmpty()) return

        ApiClient.apiService.getKycStatus(customerId, tenantSchema).enqueue(object : retrofit2.Callback<com.example.mobileapppawnshop.backend.KycStatusResponse> {
            override fun onResponse(call: retrofit2.Call<com.example.mobileapppawnshop.backend.KycStatusResponse>, response: retrofit2.Response<com.example.mobileapppawnshop.backend.KycStatusResponse>) {
                val res = response.body()
                if (response.isSuccessful && res != null && res.success) {
                    updateKycBanner(res.kyc_status ?: "unverified", res.rejection_reason)
                }
            }
            override fun onFailure(call: retrofit2.Call<com.example.mobileapppawnshop.backend.KycStatusResponse>, t: Throwable) {}
        })
    }

    private fun updateKycBanner(status: String, rejectionReason: String?) {
        val banner = findViewById<MaterialCardView>(R.id.cardKycBanner)
        val message = findViewById<TextView>(R.id.tvBannerMessage)
        val icon = findViewById<android.widget.ImageView>(R.id.ivBannerIcon)
        val arrow = findViewById<android.widget.ImageView>(R.id.ivBannerArrow)

        when (status.lowercase()) {
            "unverified" -> {
                banner.visibility = View.VISIBLE
                banner.setCardBackgroundColor(android.graphics.Color.parseColor("#e11d48")) // Red
                banner.strokeColor = android.graphics.Color.parseColor("#fb7185")
                message.text = "Account Restricted: Tap here to verify your identity."
                icon.setImageResource(android.util.TypedValue().apply { resourceId = android.R.drawable.ic_dialog_alert }.resourceId)
                banner.setOnClickListener {
                    startActivity(Intent(this, KycUploadActivity::class.java))
                }
            }
            "rejected" -> {
                banner.visibility = View.VISIBLE
                banner.setCardBackgroundColor(android.graphics.Color.parseColor("#e11d48")) // Red
                banner.strokeColor = android.graphics.Color.parseColor("#fb7185")
                message.text = "Your ID was rejected: ${rejectionReason ?: "Invalid Document"}. Tap here to re-upload."
                banner.setOnClickListener {
                    startActivity(Intent(this, KycUploadActivity::class.java))
                }
            }
            "pending" -> {
                banner.visibility = View.VISIBLE
                banner.setCardBackgroundColor(android.graphics.Color.parseColor("#eab308")) // Yellow
                banner.strokeColor = android.graphics.Color.parseColor("#facc15")
                message.text = "Identity Under Review. Please wait for employee approval."
                arrow.visibility = View.GONE
                banner.setOnClickListener(null)
                banner.isClickable = false
            }
            "verified" -> {
                banner.visibility = View.GONE
            }
            else -> banner.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        checkKycStatus()
        checkEmailSecurityLock() // TRIGGER WHEN RETURNING TO DASHBOARD
    }

    private fun fetchDashboardTickets() {
        val realCustomerId = sessionManager.getCustomerId() ?: ""
        val realTenantSchema = sessionManager.getTenantSchema() ?: ""

        val rvDashboardTickets = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvDashboardTickets)
        val tvEmptyDashboard = findViewById<TextView>(R.id.tvEmptyDashboard)
        rvDashboardTickets.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        if (realCustomerId.isNotEmpty() && realTenantSchema.isNotEmpty()) {
            ApiClient.apiService.getActiveTickets(realCustomerId, realTenantSchema, "active").enqueue(object : retrofit2.Callback<com.example.mobileapppawnshop.data.model.TicketResponse> {
                override fun onResponse(call: retrofit2.Call<com.example.mobileapppawnshop.data.model.TicketResponse>, response: retrofit2.Response<com.example.mobileapppawnshop.data.model.TicketResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val ticketsList = response.body()?.tickets ?: emptyList()
                        val sortedTickets = ticketsList.sortedBy { it.dueDate }.take(3)
                        
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
                    Toast.makeText(this@CustomerDashboardActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
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

    private fun setupTopBar() {
        // Logout Button
        findViewById<ImageButton>(R.id.btnLogout)?.setOnClickListener {
            startActivity(Intent(this, LogoutActivity::class.java))
        }
        
        // Notifications Button
        findViewById<ImageButton>(R.id.btnNotifications)?.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
    }

    private fun checkEmailSecurityLock() {
        val customerId = sessionManager.getCustomerId() ?: return
        val schema = sessionManager.getSchemaName() ?: return
        
        // Use an independent SharedPreferences file so clearSession() never touches it
        val securityPrefs = getSharedPreferences("SecurityPrefs", android.content.Context.MODE_PRIVATE)

        // Use standard Retrofit enqueue to prevent background context crashes
        ApiClient.apiService.getUserProfile(customerId, schema).enqueue(object : retrofit2.Callback<UserProfileResponse> { 
            override fun onResponse(call: retrofit2.Call<UserProfileResponse>, response: retrofit2.Response<UserProfileResponse>) {
                
                // Safe guard against window leaks
                if (isFinishing || isDestroyed) return

                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()!!
                    val reqStatus = body.latest_request_status
                    val requestedFields = body.requested_fields ?: emptyList()
                    val approvedReqId = body.latest_request_id ?: ""

                    val ackId = securityPrefs.getString("ACK_REQ_ID", "")

                    if (reqStatus == "approved" && requestedFields.contains("email") && approvedReqId != ackId) {
                        androidx.appcompat.app.AlertDialog.Builder(this@CustomerDashboardActivity)
                            .setTitle("Security Update Required")
                            .setMessage("Your email change request was approved. You must log in again with your new email address to continue.")
                            .setCancelable(false) // Traps the user
                            .setPositiveButton("Log Out") { _, _ ->
                                // 1. Save the flag to the independent prefs so they aren't trapped in a loop on next login
                                securityPrefs.edit().putString("ACK_REQ_ID", approvedReqId).apply()
                                
                                // 2. Wipe the main session
                                sessionManager.logoutUser()
                                
                                // 3. INSTANT KILL: Hard redirect directly to LoginActivity
                                val intent = android.content.Intent(this@CustomerDashboardActivity, LoginActivity::class.java)
                                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finishAffinity()
                            }
                            .show()
                    }
                }
            }

            override fun onFailure(call: retrofit2.Call<UserProfileResponse>, t: Throwable) {
                // Fail silently so the app remains usable if network drops
            }
        })
    }
}