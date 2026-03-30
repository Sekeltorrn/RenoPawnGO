package com.example.mobileapppawnshop.backend

import com.example.mobileapppawnshop.data.model.TicketResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

/**
 * Master API Interface for all Pawnshop operations.
 * All shared data models are defined here to prevent redeclaration errors.
 */
interface ApiService {
    @GET("get_tickets.php")
    fun getActiveTickets(
        @Query("customer_id") customerId: String,
        @Query("shop_code") shopCode: String,
        @Query("status") status: String // 🔥 Added status filter
    ): Call<TicketResponse>

    @Multipart
    @POST("api/upload_kyc.php")
    suspend fun uploadKyc(
        @Part("customer_id") customerId: RequestBody,
        @Part("tenant_schema") tenantSchema: RequestBody,
        @Part("id_type") idType: RequestBody,
        @Part("id_number") idNumber: RequestBody,
        @Part idDocument: MultipartBody.Part
    ): Response<KycResponse>

    @POST("api/mobile_checkout.php")
    fun generatePaymentLink(@Body request: CheckoutRequest): Call<CheckoutResponse>

    @GET("api/get_payment_history.php")
    fun getPaymentHistory(
        @Query("customer_id") customerId: String, 
        @Query("tenant_schema") tenantSchema: String
    ): Call<PaymentHistoryResponse>
}

// --- CENTRALIZED DATA MODELS (Used by both ApiService and AuthService) ---

data class CheckoutRequest(
    val ticket_no: String,
    val payment_type: String,
    val tenant_schema: String,
    val amount: String? = null
)

data class CheckoutResponse(
    val success: Boolean,
    val checkout_url: String?,
    val amount_calculated: Double?,
    val message: String?
)

data class KycResponse(
    val success: Boolean,
    val message: String?
)

data class PaymentHistoryResponse(
    val success: Boolean, 
    val history: List<PaymentRecord>, 
    val message: String
)

data class PaymentRecord(
    val pawn_ticket_no: String, 
    val amount: Double, 
    val payment_date: String, 
    val payment_type: String, 
    val payment_method: String
)
