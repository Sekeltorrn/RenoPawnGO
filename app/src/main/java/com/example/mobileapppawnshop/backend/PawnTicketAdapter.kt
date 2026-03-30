package com.example.mobileapppawnshop.backend

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileapppawnshop.R
import com.example.mobileapppawnshop.data.model.PawnTicket

class PawnTicketAdapter(private var tickets: List<PawnTicket>) : RecyclerView.Adapter<PawnTicketAdapter.TicketViewHolder>() {

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

        // Populate the UI text fields
        holder.tvItemName.text = ticket.inventory?.item_name ?: "Vault Item"
        holder.tvTicketNo.text = "Ticket No: PT-${ticket.pawn_ticket_no}"
        holder.tvPrincipal.text = "₱${ticket.principal_amount}"
        holder.tvDueDate.text = "Due: ${ticket.due_date}"

        // --- CLICK LISTENER: Pass the data to the Detail Screen ---
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, PawnTicketDetailActivity::class.java).apply {
                putExtra("TICKET_NO", ticket.pawn_ticket_no.toString())
                putExtra("PRINCIPAL", ticket.principal_amount.toString())
                // Ensure field names match get_my_tickets.php response
                putExtra("ITEM_NAME", ticket.inventory?.item_name ?: "Vault Item")
                putExtra("DUE_DATE", ticket.due_date)
                putExtra("STATUS", ticket.status)
                putExtra("MATURITY_DATE", ticket.maturity_date)
                putExtra("EXPIRATION_DATE", ticket.expiration_date)
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