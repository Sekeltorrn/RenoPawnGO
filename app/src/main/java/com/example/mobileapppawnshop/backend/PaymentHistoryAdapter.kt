package com.example.mobileapppawnshop.backend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileapppawnshop.R
import java.text.NumberFormat
import java.util.Locale

class PaymentHistoryAdapter(private var historyList: List<PaymentRecord>) :
    RecyclerView.Adapter<PaymentHistoryAdapter.ViewHolder>() {

    var isExpanded = false

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTicketNo: TextView = view.findViewById(R.id.tvTicketNo)
        val tvPaymentType: TextView = view.findViewById(R.id.tvType)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val cardIconBackground: com.google.android.material.card.MaterialCardView = view.findViewById(R.id.cardIconBackground)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = historyList[position]
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

        holder.tvTicketNo.text = "PT-${record.pawn_ticket_no}"
        holder.tvPaymentType.text = record.payment_type.replaceFirstChar { it.uppercase() }
        holder.tvAmount.text = formatter.format(record.amount)
        holder.tvDate.text = record.payment_date

        val type = record.payment_type.lowercase()
        when {
            type.contains("renewal") -> {
                holder.cardIconBackground.setCardBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
                holder.tvPaymentType.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
            }
            type.contains("partial") || type.contains("principal") -> {
                holder.cardIconBackground.setCardBackgroundColor(android.graphics.Color.parseColor("#FFF3E0"))
                holder.tvPaymentType.setTextColor(android.graphics.Color.parseColor("#E65100"))
            }
            type.contains("redemption") -> {
                holder.cardIconBackground.setCardBackgroundColor(android.graphics.Color.parseColor("#FFEBEE"))
                holder.tvPaymentType.setTextColor(android.graphics.Color.parseColor("#C62828"))
            }
            else -> {
                holder.cardIconBackground.setCardBackgroundColor(android.graphics.Color.parseColor("#F0F9FF"))
                holder.tvPaymentType.setTextColor(android.graphics.Color.parseColor("#64748B"))
            }
        }

        holder.itemView.setOnClickListener {
            val intent = android.content.Intent(holder.itemView.context, PaymentDetailActivity::class.java).apply {
                putExtra("TRANSACTION_ID", record.payment_id.toString())
                putExtra("PAYMENT_AMOUNT", record.amount.toString())
                putExtra("PAYMENT_DATE", record.payment_date)
                putExtra("PAYMENT_TYPE", record.payment_type)
                putExtra("TICKET_NO", record.pawn_ticket_no.toString())
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = if (isExpanded) historyList.size else kotlin.math.min(3, historyList.size)

    fun updateData(newList: List<PaymentRecord>) {
        historyList = newList
        notifyDataSetChanged()
    }
}
