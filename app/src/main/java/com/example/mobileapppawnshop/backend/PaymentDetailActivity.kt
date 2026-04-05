package com.example.mobileapppawnshop.backend

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapppawnshop.R
import com.example.mobileapppawnshop.utils.SessionManager
import java.text.NumberFormat
import java.util.Locale

class PaymentDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_detail)

        val transactionId = intent.getStringExtra("TRANSACTION_ID") ?: "---"
        val paymentAmountStr = intent.getStringExtra("PAYMENT_AMOUNT") ?: "0.0"
        val paymentDate = intent.getStringExtra("PAYMENT_DATE") ?: "---"
        val paymentType = intent.getStringExtra("PAYMENT_TYPE") ?: "---"
        val ticketNo = intent.getStringExtra("TICKET_NO") ?: "---"

        val sessionManager = SessionManager(this)
        findViewById<TextView>(R.id.tvShopName).text = sessionManager.getShopName() ?: "Pawnshop"

        val amount = paymentAmountStr.toDoubleOrNull() ?: 0.0
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
        findViewById<TextView>(R.id.tvAmountPaid).text = formatter.format(amount)

        findViewById<TextView>(R.id.tvPaymentDate).text = paymentDate
        findViewById<TextView>(R.id.tvTicketNo).text = "PT-$ticketNo"
        findViewById<TextView>(R.id.tvPaymentType).text = paymentType.replaceFirstChar { it.uppercase() }
        findViewById<TextView>(R.id.tvReferenceNo).text = "TXN-$transactionId"

        val vHeaderColor = findViewById<View>(R.id.vHeaderColor)
        val typeLocal = paymentType.lowercase()
        when {
            typeLocal.contains("renewal") -> vHeaderColor.setBackgroundColor(Color.parseColor("#4CAF50"))
            typeLocal.contains("partial") || typeLocal.contains("principal") -> vHeaderColor.setBackgroundColor(Color.parseColor("#FF9800"))
            typeLocal.contains("redemption") -> vHeaderColor.setBackgroundColor(Color.parseColor("#F44336"))
            else -> vHeaderColor.setBackgroundColor(Color.parseColor("#64748B"))
        }

        findViewById<Button>(R.id.btnClose).setOnClickListener {
            finish()
        }
    }
}
