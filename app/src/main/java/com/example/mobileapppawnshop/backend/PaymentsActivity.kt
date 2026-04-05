package com.example.mobileapppawnshop.backend

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileapppawnshop.R
import com.example.mobileapppawnshop.data.model.PawnTicket
import com.example.mobileapppawnshop.data.model.TicketResponse
import com.example.mobileapppawnshop.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PaymentsActivity : AppCompatActivity() {

    private lateinit var etAmount: EditText
    private lateinit var spinnerTicketSelection: AutoCompleteTextView
    private lateinit var sessionManager: SessionManager
    private lateinit var historyAdapter: PaymentHistoryAdapter
    
    private var activeTickets: List<PawnTicket> = emptyList()
    private var activeTicket: PawnTicket? = null 
    private var selectedPaymentType = "renewal" // 'renewal', 'principal', or 'redemption'

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.payments)

        sessionManager = SessionManager(this)
        etAmount = findViewById(R.id.etAmount)
        spinnerTicketSelection = findViewById(R.id.spinnerTicketSelection)

        // Setup Buttons
        val btnInterestOnly = findViewById<MaterialCardView>(R.id.btnInterestOnly)
        val btnPartial = findViewById<MaterialCardView>(R.id.btnPartial)
        val btnRedeem = findViewById<MaterialCardView>(R.id.btnRedeem)

        btnInterestOnly.setOnClickListener {
            selectedPaymentType = "renewal"
            updateSelection(btnInterestOnly, btnPartial, btnRedeem)
            calculateAmount()
        }
        btnPartial.setOnClickListener {
            selectedPaymentType = "principal"
            updateSelection(btnPartial, btnInterestOnly, btnRedeem)
            calculateAmount()
        }
        btnRedeem.setOnClickListener {
            selectedPaymentType = "redemption"
            updateSelection(btnRedeem, btnInterestOnly, btnPartial)
            calculateAmount()
        }

        // Handle Dropdown Selection
        spinnerTicketSelection.setOnItemClickListener { parent, _, position, _ ->
            activeTicket = activeTickets[position]
            calculateAmount() 
        }

        findViewById<MaterialButton>(R.id.btnProceed).setOnClickListener {
            processPayment()
        }
        
        findViewById<ImageButton>(R.id.btnLogout).setOnClickListener {
            startActivity(Intent(this, LogoutActivity::class.java))
        }

        setupHistoryRecyclerView()
        setupBottomNav()
        fetchLiveTickets()
        fetchPaymentHistory()
    }

    private fun setupHistoryRecyclerView() {
        val rvHistory = findViewById<RecyclerView>(R.id.rvPaymentHistory)
        rvHistory.layoutManager = LinearLayoutManager(this)
        historyAdapter = PaymentHistoryAdapter(emptyList())
        rvHistory.adapter = historyAdapter

        findViewById<TextView>(R.id.tvViewAll).setOnClickListener {
            historyAdapter.isExpanded = !historyAdapter.isExpanded
            val tv = it as TextView
            tv.text = if (historyAdapter.isExpanded) "Hide" else "View All"
            historyAdapter.notifyDataSetChanged()
        }
    }

    private fun fetchLiveTickets() {
        val customerId = sessionManager.getCustomerId() ?: ""
        val shopCode = sessionManager.getShopCode() ?: ""

        if (customerId.isEmpty() || shopCode.isEmpty()) {
            Toast.makeText(this, "Session Error. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        // Added "active" as the default status parameter to match AuthService updated definition
        ApiClient.apiService.getActiveTickets(customerId, shopCode, "active").enqueue(object : Callback<TicketResponse> {
            override fun onResponse(call: Call<TicketResponse>, response: Response<TicketResponse>) {
                val body = response.body()
                if (response.isSuccessful && body != null && body.success && body.tickets != null) {
                    activeTickets = body.tickets
                    
                    val ticketLabels = activeTickets.map { "PT-${it.pawn_ticket_no} - ${it.inventory?.item_name ?: "Item"}" }
                    val adapter = ArrayAdapter(this@PaymentsActivity, android.R.layout.simple_dropdown_item_1line, ticketLabels)
                    spinnerTicketSelection.setAdapter(adapter)

                    // Auto-Select logic from Intent
                    val passedTicketNo = intent.getStringExtra("TICKET_NO")
                    if (passedTicketNo != null) {
                        val index = activeTickets.indexOfFirst { it.pawn_ticket_no.toString() == passedTicketNo }
                        if (index != -1) {
                            activeTicket = activeTickets[index]
                            spinnerTicketSelection.setText(ticketLabels[index], false)
                            calculateAmount()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<TicketResponse>, t: Throwable) {
                Toast.makeText(this@PaymentsActivity, "Failed to load vault data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchPaymentHistory() {
        val customerId = sessionManager.getCustomerId() ?: ""
        val shopCode = sessionManager.getShopCode() ?: ""

        if (customerId.isEmpty() || shopCode.isEmpty()) return

        android.util.Log.d("DEBUG_PAYMENTS", "Fetching payments - Customer ID: $customerId | Shop Code: $shopCode")

        ApiClient.apiService.getPaymentHistory(customerId, shopCode).enqueue(object : Callback<PaymentHistoryResponse> {
            override fun onResponse(call: Call<PaymentHistoryResponse>, response: Response<PaymentHistoryResponse>) {
                val body = response.body()
                
                if (response.isSuccessful && body != null) {
                    if (body.success && body.history.isNotEmpty()) {
                        // We have data! Hide the empty card and show the list.
                        findViewById<View>(R.id.cardEmptyHistory).visibility = View.GONE
                        findViewById<RecyclerView>(R.id.rvPaymentHistory).visibility = View.VISIBLE
                        historyAdapter.updateData(body.history)
                    } else {
                        if (!body.success) {
                            android.util.Log.e("API_ERROR", "Payment History API returned success=false: ${body.message}")
                            Toast.makeText(this@PaymentsActivity, "API Error: ${body.message}", Toast.LENGTH_LONG).show()
                        }
                        // API responded, but history is empty (or success is false)
                        findViewById<View>(R.id.cardEmptyHistory).visibility = View.VISIBLE
                        findViewById<RecyclerView>(R.id.rvPaymentHistory).visibility = View.GONE
                    }
                } else {
                    android.util.Log.e("API_ERROR", "Server Error: ${response.code()} ${response.message()}")
                    Toast.makeText(this@PaymentsActivity, "Server Error: Could not load history", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PaymentHistoryResponse>, t: Throwable) {
                android.util.Log.e("API_ERROR", "Network Failure: ${t.message}", t)
                Toast.makeText(this@PaymentsActivity, "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun updateSelection(selected: MaterialCardView, vararg others: MaterialCardView) {
        val activeColor = ContextCompat.getColor(this, R.color.primary)
        val inactiveColor = ContextCompat.getColor(this, R.color.bg_divider)
        
        selected.strokeColor = activeColor
        selected.strokeWidth = 5 
        
        for (other in others) {
            other.strokeColor = inactiveColor
            other.strokeWidth = 1
        }
    }

    private fun calculateAmount() {
        val ticket = activeTicket ?: return

        val principal = ticket.principal_amount
        val systemInterestRate = 0.035 // 3.5%
        val systemServiceFee = 5.00

        when (selectedPaymentType) {
            "renewal" -> {
                val totalRenew = (principal * systemInterestRate) + systemServiceFee
                etAmount.setText(String.format("%.2f", totalRenew))
                etAmount.isEnabled = false 
            }
            "redemption" -> {
                val totalRedeem = principal + (principal * systemInterestRate) + systemServiceFee
                etAmount.setText(String.format("%.2f", totalRedeem))
                etAmount.isEnabled = false 
            }
            "principal" -> {
                etAmount.setText("")
                etAmount.isEnabled = true 
                etAmount.hint = "Enter Custom Amount"
                etAmount.requestFocus()
            }
        }
    }

    private fun processPayment() {
        val ticket = activeTicket
        if (ticket == null) {
            Toast.makeText(this, "Please select a ticket first.", Toast.LENGTH_SHORT).show()
            return
        }

        val amountStr = etAmount.text.toString()
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Amount cannot be blank.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Securing PayMongo Gateway...", Toast.LENGTH_SHORT).show()

        val schemaName = sessionManager.getSchemaName() ?: ""

        val request = CheckoutRequest(
            ticket_no = ticket.pawn_ticket_no.toString(),
            payment_type = selectedPaymentType,
            tenant_schema = schemaName,
            amount = amountStr
        )

        ApiClient.apiService.generatePaymentLink(request).enqueue(object : Callback<CheckoutResponse> {
            override fun onResponse(call: Call<CheckoutResponse>, response: Response<CheckoutResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success && body.checkout_url != null) {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(body.checkout_url))
                        startActivity(browserIntent)
                    } else {
                        Toast.makeText(this@PaymentsActivity, "Gateway Refused: ${body?.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val rawError = response.errorBody()?.string()
                    println("CRASH REPORT: $rawError") 
                    Toast.makeText(this@PaymentsActivity, "Server Crashed! Check Logcat.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<CheckoutResponse>, t: Throwable) {
                Toast.makeText(this@PaymentsActivity, "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun setupBottomNav() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.navigation_payments

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, CustomerDashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_loans -> {
                    startActivity(Intent(this, PawnTicketActiveActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_payments -> true
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
    }
}
