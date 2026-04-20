package com.example.mobileapppawnshop.data.repository

import android.util.Log
import com.example.mobileapppawnshop.backend.*
import com.example.mobileapppawnshop.data.model.User

/**
 * Modernized Repository using Retrofit for 2FA Authentication
 */
class AuthRepository {

    // 1. REGISTRATION (Step 1)
    fun register(name: String, email: String, phone: String, pass: String, schemaName: String): Pair<Boolean, String> {
        return try {
            val request = RegisterRequest(name, email, phone, pass, schemaName)
            val response = ApiClient.apiService.registerUser(request).execute()
            
            if (response.isSuccessful && response.body()?.status == "success") {
                Pair(true, response.body()?.message ?: "OTP Sent")
            } else {
                Pair(false, "Registration failed. Email may already exist.")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Register Error: ${e.message}")
            Pair(false, "Network error during registration.")
        }
    }

    // 2. VERIFY REGISTRATION OTP (Step 2)
    fun verifyRegisterOtp(email: String, code: String, schemaName: String): Pair<Boolean, String> {
        return try {
            val request = RegisterOtpRequest(email, code, schemaName)
            val response = ApiClient.apiService.verifyRegisterOtp(request).execute()

            if (response.isSuccessful && response.body()?.status == "success") {
                Pair(true, response.body()?.message ?: "Verified")
            } else {
                Pair(false, "Invalid or expired code.")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Verify Reg OTP Error: ${e.message}")
            Pair(false, "Network error during verification.")
        }
    }

    // 3. LOGIN CREDENTIAL CHECK (Step 1)
    fun loginAuth(email: String, pass: String, schemaName: String): Pair<Boolean, String> {
        return try {
            val request = LoginAuthRequest(email, pass, schemaName)
            val response = ApiClient.apiService.loginAuth(request).execute()

            if (response.isSuccessful && response.body()?.status == "success") {
                Pair(true, response.body()?.message ?: "OTP Sent")
            } else {
                Pair(false, "Invalid email or password.")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login Auth Error: ${e.message}")
            Pair(false, "Network error during login.")
        }
    }

    // 3.1 LOGIN PHONE (Mock SMS)
    fun loginPhone(phone: String, pass: String, schemaName: String): Pair<Boolean, String> {
        return try {
            val response = ApiClient.apiService.loginPhone(phone, pass, schemaName).execute()
            if (response.isSuccessful && response.body()?.status == "success") {
                Pair(true, response.body()?.message ?: "OTP Sent")
            } else {
                Pair(false, response.body()?.message ?: "Invalid credentials.")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login Phone Error: ${e.message}")
            Pair(false, "Network error during phone login.")
        }
    }

    // 4. VERIFY LOGIN OTP & GET SESSION DATA (Step 2)
    fun verifyLoginOtp(email: String, code: String, schemaName: String): Pair<User?, String> {
        return try {
            val request = LoginOtpRequest(email, code, schemaName)
            val response = ApiClient.apiService.verifyLoginOtp(request).execute()

            if (response.isSuccessful && response.body()?.status == "success") {
                val body = response.body()!!
                val user = User(
                    id = body.customer_id.toString(),
                    fullName = body.full_name ?: "Customer",
                    email = email,
                    kycStatus = body.kyc_status ?: "unverified"
                )
                Pair(user, "Login successful")
            } else {
                Pair(null, "Invalid or expired login code.")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Verify Login OTP Error: ${e.message}")
            Pair(null, "Network error during login verification.")
        }
    }

    // 4.1 VERIFY MOCK SMS OTP
    fun verifyMockSms(phone: String, code: String, schemaName: String): Pair<User?, String> {
        return try {
            val response = ApiClient.apiService.verifyMockSms(phone, code, schemaName).execute()
            if (response.isSuccessful && response.body()?.status == "success") {
                val body = response.body()!!
                val user = User(
                    id = body.customer_id.toString(),
                    fullName = body.full_name ?: "Walk-in Customer",
                    email = phone, // Using phone as the email property for session
                    kycStatus = body.kyc_status ?: "unverified"
                )
                Pair(user, "Login successful")
            } else {
                Pair(null, response.body()?.message ?: "Invalid or expired mock code.")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Verify Mock SMS Error: ${e.message}")
            Pair(null, "Network error during mock sms verification.")
        }
    }

    // 5. LEGACY VERIFY (Type-based)
    fun verifyCode(email: String, code: String, type: String, shopCode: String): Pair<Boolean, String?> {
        return try {
            val request = LegacyVerifyRequest(email, code, type, shopCode)
            val response = ApiClient.apiService.verifyLegacyCode(request).execute()
            if (response.isSuccessful && (response.body()?.status == "success" || response.body()?.status == "verified")) {
                Pair(true, response.body()?.kyc_status ?: "unverified")
            } else {
                Pair(false, response.body()?.message ?: "Verification failed")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Verify Code Error: ${e.message}")
            Pair(false, "Network error during verification.")
        }
    }

    // 6. SEND LOGIN OTP (Legacy)
    fun sendLoginOtp(email: String, shopCode: String): Boolean {
        return try {
            val request = LegacyOtpRequest(email, shopCode)
            val response = ApiClient.apiService.sendLoginOtpLegacy(request).execute()
            response.isSuccessful && response.body()?.status == "success"
        } catch (e: Exception) {
            Log.e("AuthRepository", "Send OTP Error: ${e.message}")
            false
        }
    }

    // 7. PASSWORD RESET PIPELINE
    fun requestPasswordReset(email: String): Pair<Boolean, String> {
        return try {
            val response = ApiClient.apiService.forgotPassword(ForgotPasswordRequest(email)).execute()
            if (response.isSuccessful && response.body()?.status == "success") {
                Pair(true, response.body()?.message ?: "Reset email sent.")
            } else {
                val errorJson = response.errorBody()?.string()
                val errorMessage = try { org.json.JSONObject(errorJson).getString("message") } catch (e: Exception) { "Failed to send reset link." }
                Pair(false, errorMessage)
            }
        } catch (e: Exception) {
            Pair(false, "Network error.")
        }
    }

    fun updatePassword(email: String, code: String, newPass: String, schemaName: String): Pair<Boolean, String> {
        return try {
            val request = ResetPasswordRequest(email, code, newPass, schemaName)
            val response = ApiClient.apiService.resetPassword(request).execute()
            if (response.isSuccessful && response.body()?.status == "success") {
                Pair(true, response.body()?.message ?: "Password updated successfully!")
            } else {
                val errorJson = response.errorBody()?.string()
                val errorMessage = try { org.json.JSONObject(errorJson).getString("message") } catch (e: Exception) { "Failed to reset password." }
                Pair(false, errorMessage)
            }
        } catch (e: Exception) {
            Pair(false, "Network error.")
        }
    }

    fun changePasswordDirect(email: String, currentPass: String, newPass: String, schemaName: String): Pair<Boolean, String> {
        return try {
            val request = DirectChangeRequest(email, currentPass, newPass, schemaName)
            val response = ApiClient.apiService.changePasswordDirect(request).execute()
            if (response.isSuccessful && response.body()?.status == "success") {
                Pair(true, response.body()?.message ?: "Password changed successfully!")
            } else {
                val errorJson = response.errorBody()?.string()
                val errorMessage = try { org.json.JSONObject(errorJson).getString("message") } catch (e: Exception) { "Failed to change password." }
                Pair(false, errorMessage)
            }
        } catch (e: Exception) {
            Pair(false, "Network error during password update.")
        }
    }

    // 8. RESEND OTP
    fun resendOtp(email: String, type: String): Pair<Boolean, String> {
        return try {
            val response = ApiClient.apiService.resendOtp(ResendOtpRequest(email, type)).execute()
            if (response.isSuccessful && response.body()?.status == "success") {
                Pair(true, response.body()?.message ?: "Code resent successfully!")
            } else {
                // Extract the REAL error message from PHP
                val errorJson = response.errorBody()?.string()
                val errorMessage = try {
                    if (errorJson != null) {
                        org.json.JSONObject(errorJson).getString("message")
                    } else {
                        "Failed to resend code."
                    }
                } catch (e: Exception) {
                    "Failed to resend code."
                }
                Pair(false, errorMessage)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Resend Error: ${e.message}")
            Pair(false, "Network error during resend.")
        }
    }
}