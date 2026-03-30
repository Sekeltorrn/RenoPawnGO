package com.example.mobileapppawnshop.data.model

import com.google.gson.annotations.SerializedName

data class TicketResponse(
    val success: Boolean,
    val tickets: List<PawnTicket>?,
    val message: String?
)

data class PawnTicket(
    val pawn_ticket_no: Int,
    val principal_amount: Double,
    val due_date: String,
    val maturity_date: String?,
    val expiration_date: String?,
    val status: String,
    val inventory: InventoryItem?
)

data class InventoryItem(
    val item_name: String
)