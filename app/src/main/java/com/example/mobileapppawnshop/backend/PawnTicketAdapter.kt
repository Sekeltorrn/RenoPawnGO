package com.example.mobileapppawnshop.backend

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileapppawnshop.R
import com.example.mobileapppawnshop.data.model.PawnTicket

class PawnTicketAdapter(private var tickets: List<PawnTicket>, private val isDashboard: Boolean = false) : RecyclerView.Adapter<PawnTicketAdapter.TicketViewHolder>() {

    class TicketViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvItemName: TextView = view.findViewById(R.id.tvItemName)
        val tvTicketNo: TextView = view.findViewById(R.id.tvTicketNo)
        val tvPrincipal: TextView = view.findViewById(R.id.tvPrincipal)
        val tvDueDate: TextView = view.findViewById(R.id.tvDueDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pawn_ticket, parent, false)
        return TicketViewHolder(view)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        val ticket = tickets[position]

        // 1. Fix Item Name: Handle empty strings, nulls, and blanks
        val rawItemName = ticket.inventory?.itemName
        val itemName = if (rawItemName.isNullOrBlank()) "Vault Item" else rawItemName
        holder.tvItemName.text = itemName

        // 2. Format Currency safely
        val formattedPrincipal = try {
            val currencyFormat = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "PH"))
            currencyFormat.format(ticket.principalAmount)
        } catch (e: Exception) {
            "₱${ticket.principalAmount}"
        }

        // 3. Format Date safely
        val formattedDate = try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val outputFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
            val date = inputFormat.parse(ticket.dueDate ?: "")
            if (date != null) outputFormat.format(date) else "----"
        } catch (e: Exception) {
            ticket.dueDate ?: "----"
        }

        // Populate the UI text fields
        holder.tvTicketNo.text = "Ticket No: ${ticket.referenceNo ?: "PT-"+ticket.pawnTicketNo}"
        holder.tvPrincipal.text = formattedPrincipal
        holder.tvDueDate.text = "Due: $formattedDate"

        // 4. Fix Dashboard Mode: Use INVISIBLE to maintain layout structure or trust ConstraintLayout
        if (isDashboard) {
            holder.tvPrincipal.visibility = View.INVISIBLE
        } else {
            holder.tvPrincipal.visibility = View.VISIBLE
        }

        // --- CLICK LISTENER: Pass the data to the Detail Screen ---
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, PawnTicketDetailActivity::class.java).apply {
                putExtra("TICKET_NO", ticket.pawnTicketNo.toString())
                putExtra("REF_NO", ticket.referenceNo ?: ticket.pawnTicketNo.toString())
                putExtra("PRINCIPAL", ticket.principalAmount.toString())
                putExtra("ITEM_NAME", itemName)
                putExtra("DUE_DATE", ticket.dueDate)
                putExtra("STATUS", ticket.status)
                putExtra("LOAN_DATE", "") 
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = tickets.size

    fun updateData(newTickets: List<PawnTicket>) {
        tickets = newTickets
        notifyDataSetChanged() // Tells the UI to refresh
    }
}