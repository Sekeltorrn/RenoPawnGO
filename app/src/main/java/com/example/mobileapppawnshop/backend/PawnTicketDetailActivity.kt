package com.example.mobileapppawnshop.backend

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapppawnshop.R
import com.example.mobileapppawnshop.utils.SessionManager
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PawnTicketDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pawn_ticket_detail)

        val sessionManager = SessionManager(this)

        // 1. UNPACK DATA
        val ticketNo = intent.getStringExtra("TICKET_NO") ?: ""
        val refNo = intent.getStringExtra("REF_NO") ?: ""
        val principalStr = intent.getStringExtra("PRINCIPAL") ?: "0.0"
        val itemName = intent.getStringExtra("ITEM_NAME") ?: ""
        val dueDateStr = intent.getStringExtra("DUE_DATE") ?: ""
        val statusStr = intent.getStringExtra("STATUS") ?: ""
        val loanDateStr = intent.getStringExtra("LOAN_DATE") ?: ""

        val principalAmount = principalStr.toDoubleOrNull() ?: 0.0
        val serviceCharge = 5.00
        val rateMonth1 = 0.035
        val rateRenewal = 0.05

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
        val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

        val dueDate = try { inputDateFormat.parse(dueDateStr) } catch (e: Exception) { Date() }
        var loanDate = try { inputDateFormat.parse(loanDateStr) } catch (e: Exception) { null }

        if (loanDate == null && dueDate != null) {
            val cal = Calendar.getInstance()
            cal.time = dueDate
            cal.add(Calendar.DAY_OF_MONTH, -30)
            loanDate = cal.time
        } else if (loanDate == null) {
            loanDate = Date()
        }

        val loanCal = Calendar.getInstance()
        loanCal.time = loanDate
        val nowCal = Calendar.getInstance()

        var monthsPassed = 0
        val yearDiff = nowCal.get(Calendar.YEAR) - loanCal.get(Calendar.YEAR)
        val monthDiff = nowCal.get(Calendar.MONTH) - loanCal.get(Calendar.MONTH)
        val dayDiff = nowCal.get(Calendar.DAY_OF_MONTH) - loanCal.get(Calendar.DAY_OF_MONTH)

        monthsPassed = (yearDiff * 12) + monthDiff
        if (dayDiff > 0) { monthsPassed++ }
        if (monthsPassed < 1) { monthsPassed = 1 }

        var totalRatePercent = 0.0
        for (i in 1..monthsPassed) {
            totalRatePercent += if (i == 1) rateMonth1 else rateRenewal
        }

        val interestAmount = principalAmount * totalRatePercent
        val penaltyAmount = if (monthsPassed > 1) { principalAmount * ((monthsPassed - 1) * rateRenewal) } else { 0.0 }

        val currentRenewTotal = interestAmount + serviceCharge
        val currentRedeemTotal = principalAmount + penaltyAmount

        // 2. BIND TO UI
        val displayRef = if (refNo == ticketNo) "PT-$refNo" else refNo
        findViewById<TextView>(R.id.tvTicketNumber).text = displayRef
        findViewById<TextView>(R.id.tvItemName).text = itemName
        findViewById<TextView>(R.id.tvPrincipalVal).text = currencyFormat.format(principalAmount)
        findViewById<TextView>(R.id.tvRateVal).text = "${(rateMonth1 * 100)}% / mo"

        if (dueDate != null) {
            findViewById<TextView>(R.id.tvDueDate).text = outputDateFormat.format(dueDate).uppercase()
        }
        if (loanDate != null) {
            findViewById<TextView>(R.id.tvLoanDate).text = outputDateFormat.format(loanDate).uppercase()
        }

        findViewById<TextView>(R.id.tvTotalRedeem).text = currencyFormat.format(currentRedeemTotal)
        findViewById<TextView>(R.id.tvInterestVal).text = currencyFormat.format(currentRenewTotal)
        findViewById<TextView>(R.id.tvPenaltyVal).text = currencyFormat.format(penaltyAmount)

        val estimatedAppraisal = principalAmount / 0.60
        findViewById<TextView>(R.id.tvAppraisalVal).text = currencyFormat.format(estimatedAppraisal)

        val tvStatusBadge = findViewById<TextView>(R.id.tvStatusBadge)
        tvStatusBadge.text = statusStr.uppercase()
        if (statusStr.lowercase() == "active" || statusStr.lowercase() == "renewed") {
            tvStatusBadge.setTextColor(Color.parseColor("#4ade80"))
        } else {
            tvStatusBadge.setTextColor(Color.parseColor("#ef4444"))
        }

        populateProjectionTable(principalAmount, interestAmount, serviceCharge, (principalAmount * rateRenewal), currencyFormat)

        // 3. FETCH LIVE SYNC (Fix for "Failed to load vault data")
        fetchTicketDetails(ticketNo, sessionManager)

        // --- BACK BUTTON LOGIC ---
        // This handles the click for the entire row (Arrow + Text)
        findViewById<LinearLayout>(R.id.btnBack).setOnClickListener {
            finish() // Returns the user to the PawnTicketActiveActivity list
        }

        val btnPayOnline = findViewById<MaterialButton>(R.id.btnPayOnline)
        val btnPrintTicket = findViewById<MaterialButton>(R.id.btnPrintTicket)
        val tvRenewedMessage = findViewById<TextView>(R.id.tvRenewedMessage)
        val tableProjectionContainer = findViewById<View>(R.id.cardProjectionContainer)
        val tvProjectionTitle = findViewById<View>(R.id.tvProjectionTitle)

        if (statusStr.lowercase() == "renewed" || statusStr.lowercase() == "redeemed") {
            btnPayOnline.visibility = View.GONE
            btnPrintTicket.visibility = View.GONE
            tvRenewedMessage.visibility = View.VISIBLE
            
            // Hide projections for historical data to avoid confusion
            tableProjectionContainer?.visibility = View.GONE
            tvProjectionTitle?.visibility = View.GONE
            
            if (statusStr.lowercase() == "renewed") {
                tvRenewedMessage.text = "This ticket has been renewed. Please check the 'Active' tab for your current balance."
            } else if (statusStr.lowercase() == "redeemed") {
                tvRenewedMessage.text = "This ticket has been fully redeemed and claimed."
            }
        } else {
            // Ensure UI is in active mode
            btnPayOnline.visibility = View.VISIBLE
            btnPrintTicket.visibility = View.VISIBLE
            tvRenewedMessage.visibility = View.GONE
            tableProjectionContainer?.visibility = View.VISIBLE
            tvProjectionTitle?.visibility = View.VISIBLE
        }

        // --- PAY ONLINE LOGIC ---
        btnPayOnline.setOnClickListener {
            val intent = Intent(this, PaymentsActivity::class.java).apply {
                putExtra("PRESELECTED_REF_NO", refNo)
            }
            startActivity(intent)
            finish() // Optional: remove this screen from backstack
        }

        btnPrintTicket.setOnClickListener {
            doPrint(ticketNo, refNo, itemName)
        }
    }

    private fun populateProjectionTable(principal: Double, baseInterest: Double, serviceCharge: Double, penaltyIncrement: Double, formatter: NumberFormat) {
        val tableLayout = findViewById<TableLayout>(R.id.tableProjection)
        var renewAmount = baseInterest + serviceCharge
        var redeemAmount = principal
        for (i in 1..3) {
            if (i > 1) {
                renewAmount += penaltyIncrement
                redeemAmount += penaltyIncrement
            }
            val tableRow = TableRow(this).apply {
                setPadding(12, 24, 12, 24)
                if (i % 2 == 0) setBackgroundColor(Color.parseColor("#f8fafc"))
            }
            val tvCycle = TextView(this).apply {
                text = if (i == 1) "Maturity (M1)" else "Month $i"
                textSize = 10f
                setTextColor(Color.parseColor("#64748b"))
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvRenew = TextView(this).apply {
                text = formatter.format(renewAmount)
                textSize = 11f
                setTextColor(Color.parseColor("#1e293b"))
                gravity = Gravity.END
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvRedeem = TextView(this).apply {
                text = formatter.format(redeemAmount)
                textSize = 11f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#991b1b"))
                gravity = Gravity.END
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            }
            tableRow.addView(tvCycle)
            tableRow.addView(tvRenew)
            tableRow.addView(tvRedeem)
            tableLayout.addView(tableRow)
        }
    }

    private fun doPrint(ticketNo: String, refNo: String, itemName: String) {
        val btnPrint = findViewById<MaterialButton>(R.id.btnPrintTicket)
        btnPrint.isEnabled = false
        btnPrint.text = "GENERATING PDF..."

        val tenantSchema = SessionManager(this).getTenantSchema() ?: ""
        val customerId = SessionManager(this).getCustomerId() ?: ""
        val request = TicketHtmlRequest(tenantSchema, ticketNo, customerId)

        ApiClient.apiService.getTicketHtml(request).enqueue(object : Callback<TicketHtmlResponse> {
            override fun onResponse(call: Call<TicketHtmlResponse>, response: Response<TicketHtmlResponse>) {
                btnPrint.isEnabled = true
                btnPrint.text = "PRINT TICKET"

                if (response.isSuccessful && response.body()?.success == true) {
                    val htmlContent = response.body()?.html ?: return
                    val webView = WebView(this@PawnTicketDetailActivity)
                    
                    // Enable JavaScript so the Tailwind CDN can load properly
                    webView.settings.javaScriptEnabled = true 
                    
                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            createWebPrintJob(view, ticketNo)
                        }
                    }
                    webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
                } else {
                    val msg = response.body()?.message ?: "Failed to generate ticket format."
                    Toast.makeText(this@PawnTicketDetailActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<TicketHtmlResponse>, t: Throwable) {
                btnPrint.isEnabled = true
                btnPrint.text = "PRINT TICKET"
                Toast.makeText(this@PawnTicketDetailActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun createWebPrintJob(webView: WebView, ticketNo: String) {
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "Pawn_Ticket_$ticketNo"
        val printAdapter = webView.createPrintDocumentAdapter(jobName)
        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
    }

    private fun fetchTicketDetails(ticketNo: String, sessionManager: SessionManager) {
        val tenantSchema = sessionManager.getTenantSchema() ?: ""
        if (ticketNo.isEmpty() || tenantSchema.isEmpty()) return

        ApiClient.apiService.getVaultData(ticketNo, tenantSchema).enqueue(object : Callback<SingleTicketResponse> {
            override fun onResponse(call: Call<SingleTicketResponse>, response: Response<SingleTicketResponse>) {
                val body = response.body()
                if (response.isSuccessful && body?.success == true && body.ticket != null) {
                    // Update any dynamic fields if necessary. 
                    // For now, we confirm the vault data has loaded correctly.
                    Toast.makeText(this@PawnTicketDetailActivity, "Vault data sync active.", Toast.LENGTH_SHORT).show()
                } else {
                    val errorMsg = body?.message ?: "Ticket not found in this vault."
                    Toast.makeText(this@PawnTicketDetailActivity, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SingleTicketResponse>, t: Throwable) {
                Toast.makeText(this@PawnTicketDetailActivity, "Failed to load vault data.", Toast.LENGTH_LONG).show()
            }
        })
    }
}
