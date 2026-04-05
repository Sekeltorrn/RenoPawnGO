package com.example.mobileapppawnshop.data.repository

import com.example.mobileapppawnshop.data.model.User
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log

/**
 * Repository for handling authentication logic with the real database
 */
class AuthRepository {

    // 1. THE FOLDER HIERARCHY URLs
    // Mobile API (Login/Connect Shop) is in 'src'
    private val loginUrl = "https://pawnereno.onrender.com/src/mobile_api.php"

    // Register, Verify, and Send OTP are in 'api'
    private val registerUrl = "https://pawnereno.onrender.com/api/register.php"
    private val verifyUrl = "https://pawnereno.onrender.com/api/verify.php"
    private val sendOtpUrl = "https://pawnereno.onrender.com/api/send_login_otp.php"

    // UPDATED: Now returns a Pair.
    // The first item is the User (if successful), the second item is the Error Message (if failed).
    fun login(email: String, pass: String, schemaName: String): Pair<User?, String?> {
        return try {
            val url = URL(loginUrl) // Points to src/
            val conn = url.openConnection() as HttpURLConnection

            conn.connectTimeout = 60000
            conn.readTimeout = 60000
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val json = JSONObject().apply {
                put("action", "login")
                put("email", email)
                put("password", pass)
                put("schema_name", schemaName)
            }

            conn.outputStream.use { it.write(json.toString().toByteArray()) }

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val resJson = JSONObject(response)

            if (resJson.getString("status") == "success") {
                val userJson = resJson.getJSONObject("user")
                
                // --- THE FIX: Pull the real ID from your PHP JSON ---
                val realId = userJson.optString("id", userJson.optString("customer_id", "0"))
                val user = User(id = realId, fullName = userJson.getString("fullName"), email = userJson.getString("email"))
                
                Pair(user, null) // Success: Return user, no error message
            } else {
                // FAILURE: Grab the exact message from mobile_api.php!
                val errorMessage = resJson.optString("message", "Invalid email or password.")
                Pair(null, errorMessage) // Fail: Return no user, but include the message
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login Error: ${e.message}")
            Pair(null, "Connection error. Please try again.")
        }
    }

    fun register(name: String, email: String, phone: String, pass: String, schemaName: String): Boolean {
        return try {
            val url = URL(registerUrl) // Points to api/
            val conn = url.openConnection() as HttpURLConnection

            conn.connectTimeout = 60000
            conn.readTimeout = 60000
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val json = JSONObject().apply {
                put("full_name", name)
                put("email", email)
                put("phone_number", phone)
                put("password", pass)
                put("schema_name", schemaName)
            }

            conn.outputStream.use { it.write(json.toString().toByteArray()) }

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val resJson = JSONObject(response)

            resJson.getString("status") == "success"
        } catch (e: Exception) {
            Log.e("AuthRepository", "Register Error: ${e.message}")
            false
        }
    }

    fun verifyCode(email: String, code: String, type: String, shopCode: String): Boolean {
        return try {
            val url = URL(verifyUrl) // Points to api/
            val conn = url.openConnection() as HttpURLConnection

            conn.connectTimeout = 60000
            conn.readTimeout = 60000
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val json = JSONObject().apply {
                put("email", email)
                put("code", code)
                put("type", type)
                put("shop_code", shopCode)
            }

            conn.outputStream.use { it.write(json.toString().toByteArray()) }

            val responseCode = conn.responseCode
            responseCode == 200

        } catch (e: Exception) {
            Log.e("AuthRepository", "Verify Error: ${e.message}")
            false
        }
    }

    fun sendLoginOtp(email: String, shopCode: String): Boolean {
        return try {
            val url = URL(sendOtpUrl) // Points to api/
            val conn = url.openConnection() as HttpURLConnection

            conn.connectTimeout = 60000
            conn.readTimeout = 60000
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val json = JSONObject().apply {
                put("email", email)
                put("shop_code", shopCode)
            }

            conn.outputStream.use { it.write(json.toString().toByteArray()) }
            conn.responseCode == 200

        } catch (e: Exception) {
            Log.e("AuthRepository", "Send OTP Error: ${e.message}")
            false
        }
    }

    fun resetPassword(email: String): Boolean {
        return email.isNotEmpty()
    }

    fun updatePassword(password: String): Boolean {
        return password.length >= 8
    }
}