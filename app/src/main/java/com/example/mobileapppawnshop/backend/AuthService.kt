package com.example.mobileapppawnshop.backend

import com.example.mobileapppawnshop.data.model.TicketResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

/**
 * Interface for Authentication and Ticket related API calls.
 * Note: CheckoutRequest, CheckoutResponse, KycResponse, PaymentHistoryResponse, 
 * and PaymentRecord are defined in ApiService.kt
 */
interface AuthService {

    // 1. THE TICKET BRIDGE
    // Updated to accept status filter
    @GET("api/get_my_tickets.php")
    fun getActiveTickets(
        @Query("customer_id") customerId: String,
        @Query("shop_code") shopCode: String,
        @Query("status") status: String
    ): Call<TicketResponse>

    // 2. Checkout Endpoint
    @POST("api/mobile_checkout.php")
    fun generatePaymentLink(@Body request: CheckoutRequest): Call<CheckoutResponse>

    /**
     * KYC Upload Endpoint (Multipart)
     */
    @Multipart
    @POST("api/upload_kyc.php")
    suspend fun uploadKyc(
        @Part("customer_id") customerId: RequestBody,
        @Part("tenant_schema") tenantSchema: RequestBody,
        @Part("id_type") idType: RequestBody,
        @Part("id_number") idNumber: RequestBody,
        @Part id_document: MultipartBody.Part
    ): Response<KycResponse>

    /**
     * Fetch payment history for a customer
     */
    @GET("api/get_payment_history.php")
    fun getPaymentHistory(
        @Query("customer_id") customerId: String, 
        @Query("tenant_schema") tenantSchema: String
    ): Call<PaymentHistoryResponse>
}