package com.example.mobileapppawnshop.data.model

import com.google.gson.annotations.SerializedName

data class TicketResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("tickets") val tickets: List<PawnTicket>?,
    @SerializedName("message") val message: String?
)

data class PawnTicket(
    @SerializedName("pawn_ticket_no") val pawnTicketNo: Int,
    @SerializedName("reference_no") val referenceNo: String?,
    @SerializedName("principal_amount") val principalAmount: Double,
    @SerializedName("due_date") val dueDate: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("inventory") val inventory: Inventory?
)

data class Inventory(
    @SerializedName("item_name") val itemName: String?
)