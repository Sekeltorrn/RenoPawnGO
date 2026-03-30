package com.example.mobileapppawnshop.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages user session using SharedPreferences
 */
class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = prefs.edit()

    companion object {
        private const val PREF_NAME = "UserSession"
        private const val IS_LOGGED_IN = "IsLoggedIn"
        private const val KEY_EMAIL = "UserEmail"
        private const val KEY_NAME = "UserName"
        private const val KEY_CUSTOMER_ID = "CUSTOMER_ID"
        private const val KEY_SHOP_CODE = "SHOP_CODE"
        private const val KEY_SCHEMA_NAME = "SchemaName"
        private const val KEY_SHOP_NAME = "ShopName"
    }

    fun saveUserLogin(email: String, name: String) {
        editor.putBoolean(IS_LOGGED_IN, true)
        editor.putString(KEY_EMAIL, email)
        editor.putString(KEY_NAME, name)
        editor.apply()
    }

    fun saveUserLoginFull(email: String, name: String, customerId: String) {
        editor.putBoolean(IS_LOGGED_IN, true)
        editor.putString(KEY_EMAIL, email)
        editor.putString(KEY_NAME, name)
        editor.putString(KEY_CUSTOMER_ID, customerId)
        editor.apply()
    }

    fun saveShopDetails(shopCode: String, schemaName: String, shopName: String) {
        editor.putString(KEY_SHOP_CODE, shopCode)
        editor.putString(KEY_SCHEMA_NAME, schemaName)
        editor.putString(KEY_SHOP_NAME, shopName)
        editor.apply()
    }

    // --- USER REQUESTED FUNCTIONS ---

    // Save the Shop Code
    fun saveShopCode(shopCode: String) {
        editor.putString(KEY_SHOP_CODE, shopCode).apply()
    }

    // Get the Shop Code
    fun getShopCode(): String? {
        return prefs.getString(KEY_SHOP_CODE, "")
    }

    // Save the Customer ID
    fun saveCustomerId(customerId: String) {
        editor.putString(KEY_CUSTOMER_ID, customerId).apply()
    }

    // Get the Customer ID
    fun getCustomerId(): String? {
        return prefs.getString(KEY_CUSTOMER_ID, "")
    }

    // --------------------------------

    fun isUserLoggedIn(): Boolean {
        return prefs.getBoolean(IS_LOGGED_IN, false)
    }

    fun getSchemaName(): String? {
        return prefs.getString(KEY_SCHEMA_NAME, null)
    }

    fun getShopName(): String? {
        return prefs.getString(KEY_SHOP_NAME, null)
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_EMAIL, null)
    }

    fun getUserName(): String? {
        return prefs.getString(KEY_NAME, "User")
    }

    fun logoutUser() {
        editor.putBoolean(IS_LOGGED_IN, false)
        editor.remove(KEY_EMAIL)
        editor.remove(KEY_NAME)
        editor.remove(KEY_CUSTOMER_ID)
        editor.apply()
    }

    fun clearAll() {
        editor.clear()
        editor.apply()
    }
}
