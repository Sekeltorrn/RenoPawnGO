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

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTicketNo: TextView = view.findViewById(R.id.tvTicketNo)
        val tvPaymentType: TextView = view.findViewById(R.id.tvType)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
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
    }

    override fun getItemCount() = historyList.size

    fun updateData(newList: List<PaymentRecord>) {
        historyList = newList
        notifyDataSetChanged()
    }
}
