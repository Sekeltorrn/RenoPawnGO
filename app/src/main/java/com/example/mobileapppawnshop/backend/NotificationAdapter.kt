package com.example.mobileapppawnshop.backend

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileapppawnshop.R
import com.google.android.material.card.MaterialCardView

class NotificationAdapter(private var items: List<NotificationItem>) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardNotification)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvSubject: TextView = view.findViewById(R.id.tvSubject)
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = items[position]

        // Populate text
        holder.tvCategory.text = item.category
        holder.tvSubject.text = item.subject
        holder.tvMessage.text = item.message
        holder.tvDate.text = item.date_acted

        // Color Logic
        val bgColor: String
        val strokeColor: String
        val mainTextColor: String
        val secondaryTextColor: String

        when (item.color_code.lowercase()) {
            "green" -> {
                bgColor = "#f0fdf4"
                strokeColor = "#bbf7d0"
                mainTextColor = "#166534"
                secondaryTextColor = "#15803d"
            }
            "red" -> {
                bgColor = "#fef2f2"
                strokeColor = "#fecaca"
                mainTextColor = "#991b1b"
                secondaryTextColor = "#b91c1c"
            }
            "yellow" -> {
                bgColor = "#fefce8"
                strokeColor = "#fef08a"
                mainTextColor = "#854d0e"
                secondaryTextColor = "#a16207"
            }
            else -> {
                bgColor = "#ffffff"
                strokeColor = "#e2e8f0"
                mainTextColor = "#1e293b"
                secondaryTextColor = "#475569"
            }
        }

        holder.card.setCardBackgroundColor(Color.parseColor(bgColor))
        holder.card.strokeColor = Color.parseColor(strokeColor)
        
        holder.tvSubject.setTextColor(Color.parseColor(mainTextColor))
        holder.tvCategory.setTextColor(Color.parseColor(mainTextColor))
        holder.tvMessage.setTextColor(Color.parseColor(secondaryTextColor))
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<NotificationItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
