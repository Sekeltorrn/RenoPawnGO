package com.example.mobileapppawnshop.utils

import android.util.Patterns

/**
 * Utility class for input validation
 */
object ValidationUtils {

    fun validateEmptyField(text: String): Boolean {
        return text.isNotBlank()
    }

    fun validateEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun validatePassword(password: String): Boolean {
        return password.length >= 8
    }

    fun validatePasswordMatch(password: String, confirm: String): Boolean {
        return password == confirm
    }
}
