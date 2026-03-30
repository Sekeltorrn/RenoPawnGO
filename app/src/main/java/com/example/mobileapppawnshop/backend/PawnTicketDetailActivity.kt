package com.example.mobileapppawnshop.backend

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.Gravity
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
        val currentRedeemTotal = principalAmount + penaltyAmount + serviceCharge

        // 2. BIND TO UI
        findViewById<TextView>(R.id.tvTicketNumber).text = "PT-${ticketNo.padStart(5, '0')}"
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

        // --- BACK BUTTON LOGIC ---
        // This handles the click for the entire row (Arrow + Text)
        findViewById<LinearLayout>(R.id.btnBack).setOnClickListener {
            finish() // Returns the user to the PawnTicketActiveActivity list
        }

        // --- PAY ONLINE LOGIC ---
        findViewById<MaterialButton>(R.id.btnPayOnline).setOnClickListener {
            val schemaName = sessionManager.getSchemaName() ?: ""
            val request = CheckoutRequest(
                ticket_no = ticketNo, 
                payment_type = "renewal", 
                tenant_schema = schemaName
            )
            
            ApiClient.apiService.generatePaymentLink(request).enqueue(object : Callback<CheckoutResponse> {
                override fun onResponse(call: Call<CheckoutResponse>, response: Response<CheckoutResponse>) {
                    val body = response.body()
                    if (response.isSuccessful && body != null && body.success && !body.checkout_url.isNullOrEmpty()) {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(body.checkout_url))
                        startActivity(browserIntent)
                    } else {
                        Toast.makeText(this@PawnTicketDetailActivity, "Gateway Refused: ${body?.message}", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(call: Call<CheckoutResponse>, t: Throwable) {
                    Toast.makeText(this@PawnTicketDetailActivity, "Network Error.", Toast.LENGTH_SHORT).show()
                }
            })
        }

        findViewById<MaterialButton>(R.id.btnPrintTicket).setOnClickListener {
            doPrint(ticketNo, itemName)
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

    private fun doPrint(ticketNo: String, itemName: String) {
        val webView = WebView(this)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                createWebPrintJob(view, ticketNo)
            }
        }
        val htmlContent = """
            <html>
                <body style="font-family: sans-serif; padding: 20px;">
                    <div style="background-color: #0f172a; color: white; padding: 20px; text-align: right;">
                        <h2 style="margin: 0; font-size: 14px; color: #94a3b8;">OFFICIAL VAULT RECORD</h2>
                        <h1 style="margin-top: 10px; font-size: 32px;">PT-${ticketNo.padStart(5, '0')}</h1>
                    </div>
                    <div style="padding: 20px; border: 1px solid #e2e8f0; margin-top: 20px;">
                        <p style="color: #64748b; font-size: 12px; margin: 0;">PAWNED COLLATERAL</p>
                        <p style="font-weight: bold; font-size: 18px; margin-top: 5px;">$itemName</p>
                    </div>
                </body>
            </html>
        """.trimIndent()
        webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
    }

    private fun createWebPrintJob(webView: WebView, ticketNo: String) {
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "Pawn_Ticket_$ticketNo"
        val printAdapter = webView.createPrintDocumentAdapter(jobName)
        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
    }
}
