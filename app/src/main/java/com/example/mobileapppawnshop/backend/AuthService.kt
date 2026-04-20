package com.example.mobileapppawnshop.backend

import com.google.gson.annotations.SerializedName
import com.example.mobileapppawnshop.data.model.TicketResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

/**
 * Single Consolidated Interface for all API calls.
 */
interface AuthService {

    // 1. THE TICKET BRIDGE
    @FormUrlEncoded
    @POST("api/get_my_tickets.php")
    fun getActiveTickets(
        @Field("customer_id") customerId: String,
        @Field("tenant_schema") tenantSchema: String,
        @Field("status") status: String
    ): Call<TicketResponse>

    @FormUrlEncoded
    @POST("api/get_my_tickets.php")
    fun getPawnTickets(
        @Field("customer_id") customerId: String,
        @Field("tenant_schema") tenantSchema: String,
        @Field("status") status: String
    ): Call<TicketResponse>

    // 2. Checkout Endpoints
    @POST("api/mobile_checkout.php")
    fun generatePaymentLink(@Body request: CheckoutRequest): Call<CheckoutResponse>

    @POST("api/mobile_checkout.php")
    fun checkout(@Body request: CheckoutRequest): Call<CheckoutResponse>

    @FormUrlEncoded
    @POST("api/mobile_checkout.php")
    fun payOnline(
        @Field("customer_id") customerId: String,
        @Field("ticket_no") ticketNo: String,
        @Field("payment_type") paymentType: String,
        @Field("amount") amount: String,
        @Field("tenant_schema") tenantSchema: String
    ): Call<CheckoutResponse>

    // 3. User & KYC Endpoints
    @FormUrlEncoded
    @POST("api/get_user_profile.php")
    fun getUserProfile(
        @Field("customer_id") customerId: String,
        @Field("tenant_schema") tenantSchema: String
    ): Call<UserProfileResponse>

    @FormUrlEncoded
    @POST("api/request_profile_change.php")
    fun requestProfileChange(
        @Field("customer_id") customerId: String,
        @Field("tenant_schema") tenantSchema: String,
        @Field("email") email: String,
        @Field("contact_no") contactNo: String,
        @Field("address") address: String
    ): Call<ProfileChangeResponse>

    @Multipart
    @POST("api/upload_kyc.php")
    suspend fun uploadKyc(
        @Part("customer_id") customerId: RequestBody,
        @Part("tenant_schema") tenantSchema: RequestBody,
        @Part("id_type") idType: RequestBody,
        @Part("id_number") idNumber: RequestBody,
        @Part id_document: MultipartBody.Part,
        @Part id_document_back: MultipartBody.Part
    ): Response<KycResponse>

    @FormUrlEncoded
    @POST("api/get_kyc_status.php")
    fun getKycStatus(
        @Field("customer_id") customerId: String,
        @Field("tenant_schema") tenantSchema: String
    ): Call<KycStatusResponse>

    @FormUrlEncoded
    @POST("api/get_ticket_details.php")
    fun getVaultData(
        @Field("ticket_no") ticketNo: String,
        @Field("tenant_schema") tenantSchema: String
    ): Call<SingleTicketResponse>

    @FormUrlEncoded
    @POST("api/get_payment_history.php")
    fun getPaymentHistory(
        @Field("customer_id") customerId: String, 
        @Field("tenant_schema") tenantSchema: String
    ): Call<PaymentHistoryResponse>

    // --- 4. 2FA AUTHENTICATION ENDPOINTS ---
    
    @POST("api/register.php")
    fun registerUser(@Body request: RegisterRequest): Call<BasicAuthResponse>

    @POST("api/register_otp.php")
    fun verifyRegisterOtp(@Body request: RegisterOtpRequest): Call<BasicAuthResponse>

    @POST("api/login_auth.php")
    fun loginAuth(@Body request: LoginAuthRequest): Call<BasicAuthResponse>

    @POST("api/login_otp.php")
    fun verifyLoginOtp(@Body request: LoginOtpRequest): Call<LoginOtpResponse>

    @FormUrlEncoded
    @POST("api/login_phone.php")
    fun loginPhone(
        @Field("phone") phone: String,
        @Field("password") password: String,
        @Field("tenant_schema") tenantSchema: String
    ): Call<LoginOtpResponse> 

    @FormUrlEncoded
    @POST("api/mock_sms_verify.php")
    fun verifyMockSms(
        @Field("phone_number") phone: String,
        @Field("code") code: String,
        @Field("tenant_schema") tenantSchema: String
    ): Call<LoginOtpResponse>

    @POST("api/resend_otp.php")
    fun resendOtp(@Body request: ResendOtpRequest): Call<BasicAuthResponse>

    // --- 5. LEGACY / PASSWORD RESET ENDPOINTS ---
    
    @POST("api/forgot_password.php")
    fun forgotPassword(@Body request: ForgotPasswordRequest): Call<BasicAuthResponse>

    @POST("api/reset_password.php")
    fun resetPassword(@Body request: ResetPasswordRequest): Call<BasicAuthResponse>

    @POST("api/change_password.php")
    fun changePasswordDirect(@Body request: DirectChangeRequest): Call<BasicAuthResponse>

    @POST("api/verify.php")
    fun verifyLegacyCode(@Body request: LegacyVerifyRequest): Call<LegacyVerifyResponse>

    @POST("api/send_login_otp.php")
    fun sendLoginOtpLegacy(@Body request: LegacyOtpRequest): Call<BasicAuthResponse>

    @POST("api/request_change_email_otp.php")
    fun requestChangeEmailOtp(@Body request: RequestEmailChangeRequest): Call<BasicAuthResponse>

    @POST("api/verify_change_email_otp.php")
    fun verifyChangeEmailOtp(@Body request: VerifyEmailChangeRequest): Call<BasicAuthResponse>

    @POST("api/get_store_hours.php")
    fun getStoreHours(@Body request: TenantRequest): Call<StoreHoursResponse>

    @POST("api/book_appointment.php")
    fun bookAppointment(@Body request: BookAppointmentRequest): Call<BasicAuthResponse>

    @POST("api/get_booked_slots.php")
    fun getBookedSlots(@Body request: BookedSlotsRequest): Call<BookedSlotsResponse>

    @POST("api/get_notifications.php")
    fun getNotifications(@Body req: TenantCustomerRequest): Call<NotificationResponse>

    @POST("api/cancel_appointment.php")
    fun cancelAppointment(@Body request: CancelAppointmentRequest): Call<BasicAuthResponse>

    @POST("api/get_ticket_html.php")
    fun getTicketHtml(@Body req: TicketHtmlRequest): Call<TicketHtmlResponse>

} // <-- End of AuthService interface





// --- NEW AUTH DATA MODELS ---
data class ForgotPasswordRequest(val email: String)
data class ResetPasswordRequest(val email: String, val code: String, val new_password: String, val tenant_schema: String)
data class DirectChangeRequest(val email: String, val current_password: String, val new_password: String, val tenant_schema: String)

data class LegacyVerifyRequest(val email: String, val code: String, val type: String, val shop_code: String)
data class LegacyOtpRequest(val email: String, val shop_code: String)

data class ResendOtpRequest(val email: String, val type: String)

data class RequestEmailChangeRequest(val new_email: String, val tenant_schema: String)
data class VerifyEmailChangeRequest(val email: String, val code: String)

data class LegacyVerifyResponse(
    val status: String,
    val message: String?,
    val kyc_status: String?
)

data class RegisterRequest(val full_name: String, val email: String, val phone_number: String, val password: String, val tenant_schema: String)
data class RegisterOtpRequest(val email: String, val code: String, val tenant_schema: String)
data class LoginAuthRequest(val email: String, val password: String, val tenant_schema: String)
data class LoginOtpRequest(val email: String, val code: String, val tenant_schema: String)

data class BasicAuthResponse(val status: String, val message: String)

data class LoginOtpResponse(
    val status: String, 
    val message: String, 
    val customer_id: String?, 
    val full_name: String?, 
    val kyc_status: String?, 
    val tenant_schema: String?
)

data class SingleTicketResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("ticket") val ticket: com.example.mobileapppawnshop.data.model.PawnTicket?,
    @SerializedName("message") val message: String?
)

// --- DATA MODELS ---

data class UserProfileResponse(
    val success: Boolean,
    val kyc_status: String?,
    val first_name: String?,
    val middle_name: String?,
    val last_name: String?,
    val address: String?,
    val birthday: String?,
    val email: String?,
    val contact_no: String?,
    val id_photo_front_url: String?,
    val id_photo_back_url: String?,
    val rejection_reason: String?,
    val pending_fields: List<String>?,
    val requested_fields: List<String>?, // ADD THIS
    val latest_request_status: String?, // ADD THIS
    val latest_request_id: String?      // ADD THIS
)

data class ProfileChangeResponse(
    val success: Boolean,
    val message: String,
    val request_id: String? = null
)

data class CheckoutRequest(
    val customer_id: String,
    val tenant_schema: String,
    val amount: String,
    val ticket_no: String,
    val payment_type: String
)

data class CheckoutResponse(
    val success: Boolean,
    val checkout_url: String?,
    val message: String?
)

data class KycStatusResponse(
    val success: Boolean,
    val kyc_status: String?,
    val rejection_reason: String? = null,
    val id_photo_front_url: String? = null,
    val id_photo_back_url: String? = null,
    val message: String? = null
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
    val payment_id: String?,
    val pawn_ticket_no: String, 
    val reference_no: String? = null,
    val amount: Double, 
    val payment_date: String, 
    val payment_type: String, 
    val payment_method: String
)

data class TenantRequest(val tenant_schema: String)
data class BookedSlotsRequest(val tenant_schema: String, val customer_id: String)


data class StoreHoursData(
    val store_open_time: String?,
    val store_close_time: String?,
    val closed_days: Any?
)

data class StoreHoursResponse(
    val status: String,
    val data: StoreHoursData?
)


data class BookAppointmentRequest(
    val tenant_schema: String,
    val customer_id: String,
    val appointment_date: String,
    val appointment_time: String,
    val purpose: String,
    val item_description: String
)

data class BookedSlot(
    val appointment_id: String?, 
    val appointment_date: String,
    val appointment_time: String,
    val status: String,
    val customer_id: String,
    val purpose: String?
)

data class CancelAppointmentRequest(
    val appointment_id: String,
    val tenant_schema: String
)

data class BookedSlotsResponse(
    val success: Boolean,
    val appointments: List<BookedSlot>
)

data class NotificationItem(
    val category: String,
    val subject: String,
    val message: String,
    val date_acted: String,
    val color_code: String
)

data class NotificationResponse(
    val status: String,
    val data: List<NotificationItem>
)

data class TenantCustomerRequest(
    val tenant_schema: String,
    val customer_id: String
)

data class TicketHtmlRequest(val tenant_schema: String, val ticket_no: String, val customer_id: String)

data class TicketHtmlResponse(val success: Boolean, val html: String?, val message: String?)