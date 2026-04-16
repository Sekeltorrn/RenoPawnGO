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
    private lateinit var btnProceed: MaterialButton
    private lateinit var spinnerTicketSelection: AutoCompleteTextView
    private lateinit var sessionManager: SessionManager
    private lateinit var historyAdapter: PaymentHistoryAdapter
    
    private var activeTickets: List<PawnTicket> = emptyList()
    private var activeTicket: PawnTicket? = null 
    private var selectedPaymentType = "renewal" // 'renewal', 'principal', or 'redemption'
    private var minPaymentRequired = 0.0 // Interest + Service Fee

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.payments)

        sessionManager = SessionManager(this)
        etAmount = findViewById(R.id.etAmount)
        btnProceed = findViewById(R.id.btnProceed)
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

        // Handle Dropdown Selection (Safely handles filtered search results)
        spinnerTicketSelection.setOnItemClickListener { parent, _, position, _ ->
            val selectedLabel = parent.getItemAtPosition(position) as String
            
            // Find the ticket that matches the exact string the user clicked
            activeTicket = activeTickets.find { ticket -> 
                val label = "${ticket.referenceNo ?: "PT-"+ticket.pawnTicketNo} - ${ticket.inventory?.itemName ?: "Item"}"
                label == selectedLabel 
            }
            
            calculateAmount() 
        }

        // Initialize Proceed Button State (Greyed out until ticket is selected)
        btnProceed.isEnabled = false
        btnProceed.alpha = 0.5f
        
        btnProceed.setOnClickListener {
            processPayment()
        }
        
        etAmount.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                validateAmount()
            }
        })
        
        setupTopBar()

        setupHistoryRecyclerView()
        setupBottomNav()
        fetchLiveTickets()
        fetchPaymentHistory()
    }

    private fun validateAmount() {
        val amountStr = etAmount.text.toString()
        val principal = activeTicket?.principalAmount ?: 0.0
        val enteredAmount = amountStr.toDoubleOrNull() ?: 0.0

        // 1. Instantly Update the Breakdown Total based on Payment Type
        when (selectedPaymentType) {
            "renewal" -> {
                findViewById<TextView>(R.id.tvBreakdownPrincipal).text = "₱0.00"
                findViewById<TextView>(R.id.tvBreakdownTotal).text = String.format("₱%.2f", minPaymentRequired)
            }
            "redemption" -> {
                // UPDATE: Lock the breakdown principal to the exact principal, don't read the main box
                findViewById<TextView>(R.id.tvBreakdownPrincipal).text = String.format("₱%.2f", principal)
                val redemptionTotal = principal + minPaymentRequired
                findViewById<TextView>(R.id.tvBreakdownTotal).text = String.format("₱%.2f", redemptionTotal)
            }
            "principal" -> {
                // For partial, the main box shows the principal chunk you want to eliminate
                findViewById<TextView>(R.id.tvBreakdownPrincipal).text = String.format("₱%.2f", enteredAmount)
                val grandTotal = enteredAmount + minPaymentRequired
                findViewById<TextView>(R.id.tvBreakdownTotal).text = String.format("₱%.2f", grandTotal)
            }
        }

        // 2. Strict Validation Logic
        if (selectedPaymentType == "principal") {
            when {
                enteredAmount <= 0 -> {
                    etAmount.error = "Enter an amount greater than 0"
                    btnProceed.isEnabled = false
                    btnProceed.alpha = 0.5f
                }
                enteredAmount >= principal -> {
                    etAmount.error = "To pay in full, please select Redemption"
                    btnProceed.isEnabled = false
                    btnProceed.alpha = 0.5f
                }
                else -> {
                    etAmount.error = null
                    btnProceed.isEnabled = true
                    btnProceed.alpha = 1.0f
                }
            }
        } else {
            // Renewal and Redemption are automatically valid because the fields are locked
            etAmount.error = null
            btnProceed.isEnabled = true
            btnProceed.alpha = 1.0f
        }
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
        val tenantSchema = sessionManager.getTenantSchema() ?: ""

        if (customerId.isEmpty() || tenantSchema.isEmpty()) {
            Toast.makeText(this, "Session Error. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        // Added "active" as the default status parameter to match AuthService updated definition
        ApiClient.apiService.getActiveTickets(customerId, tenantSchema, "active").enqueue(object : Callback<TicketResponse> {
            override fun onResponse(call: Call<TicketResponse>, response: Response<TicketResponse>) {
                val body = response.body()
                if (response.isSuccessful && body != null && body.success && body.tickets != null) {
                    // Step 2: Only show active tickets
                    activeTickets = body.tickets.filter { it.status?.lowercase() == "active" }
                    
                    if (activeTickets.isEmpty()) {
                        Toast.makeText(this@PaymentsActivity, "No active loans found. All loans are currently renewed or redeemed.", Toast.LENGTH_LONG).show()
                        spinnerTicketSelection.setText("No Active Tickets Available", false)
                        spinnerTicketSelection.isEnabled = false
                        btnProceed.isEnabled = false
                        btnProceed.alpha = 0.5f
                        return
                    }

                    val ticketLabels = activeTickets.map { "${it.referenceNo ?: "PT-"+it.pawnTicketNo} - ${it.inventory?.itemName ?: "Item"}" }
                    val adapter = ArrayAdapter(this@PaymentsActivity, android.R.layout.simple_dropdown_item_1line, ticketLabels)
                    spinnerTicketSelection.setAdapter(adapter)

                    // NEW TWEAK: Make it act like a true searchable combobox
                    spinnerTicketSelection.setOnClickListener {
                        spinnerTicketSelection.showDropDown() // Forces the full list to show on click
                    }
                    spinnerTicketSelection.setOnFocusChangeListener { _, hasFocus ->
                        if (hasFocus) spinnerTicketSelection.showDropDown()
                    }

                    // Auto-Select logic from Intent (Using Reference Number or Ticket Number)
                    val preselectedRefNo = intent.getStringExtra("PRESELECTED_REF_NO")
                    val passedTicketNo = intent.getStringExtra("TICKET_NO")

                    val index = when {
                        preselectedRefNo != null -> activeTickets.indexOfFirst { it.referenceNo == preselectedRefNo || it.pawnTicketNo.toString() == preselectedRefNo }
                        passedTicketNo != null -> activeTickets.indexOfFirst { it.pawnTicketNo.toString() == passedTicketNo }
                        else -> -1
                    }

                    if (index != -1) {
                        activeTicket = activeTickets[index]
                        spinnerTicketSelection.setText(ticketLabels[index], false)
                        calculateAmount()
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
        val tenantSchema = sessionManager.getTenantSchema() ?: ""

        if (customerId.isEmpty() || tenantSchema.isEmpty()) return

        android.util.Log.d("DEBUG_PAYMENTS", "Fetching payments - Customer ID: $customerId | Tenant Schema: $tenantSchema")

        ApiClient.apiService.getPaymentHistory(customerId, tenantSchema).enqueue(object : Callback<PaymentHistoryResponse> {
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

        // 1. Unhide the breakdown card now that a ticket is selected
        findViewById<View>(R.id.cardBreakdown).visibility = View.VISIBLE

        val principal = ticket.principalAmount
        val systemInterestRate = 0.035 // 3.5%
        val systemServiceFee = 5.00
        
        val interestDue = principal * systemInterestRate
        minPaymentRequired = interestDue + systemServiceFee

        // 2. Populate the static fees in the breakdown
        findViewById<TextView>(R.id.tvBreakdownInterest).text = String.format("₱%.2f", interestDue)
        findViewById<TextView>(R.id.tvBreakdownFee).text = String.format("₱%.2f", systemServiceFee)

        when (selectedPaymentType) {
            "renewal" -> {
                // UPDATE: Show the actual renewal cost in the big box instead of 0.00
                etAmount.setText(String.format("%.2f", minPaymentRequired))
                etAmount.isEnabled = false 
                findViewById<TextView>(R.id.tvBreakdownPrincipalLabel).text = "Principal Reduction"
            }
            "redemption" -> {
                // UPDATE: Calculate the grand total and show it in the big box
                val redemptionTotal = principal + minPaymentRequired
                etAmount.setText(String.format("%.2f", redemptionTotal)) 
                etAmount.isEnabled = false 
                findViewById<TextView>(R.id.tvBreakdownPrincipalLabel).text = "Full Principal"
            }
            "principal" -> {
                etAmount.setText("")
                etAmount.isEnabled = true 
                etAmount.hint = "Enter principal to pay off"
                findViewById<TextView>(R.id.tvBreakdownPrincipalLabel).text = "Principal Reduction"
                etAmount.requestFocus()
            }
        }
        
        validateAmount() // Trigger the live total calculation
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

        val tenantSchema = sessionManager.getTenantSchema() ?: ""
        val customerId = sessionManager.getCustomerId() ?: ""
        val amountFormatted = String.format("%.2f", amountStr.toDoubleOrNull() ?: 0.0)

        val request = CheckoutRequest(
            customer_id = customerId,
            ticket_no = ticket.pawnTicketNo.toString(), // MUST BE THE RAW SERIAL ID
            payment_type = selectedPaymentType,
            tenant_schema = tenantSchema,
            amount = amountFormatted
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
}
