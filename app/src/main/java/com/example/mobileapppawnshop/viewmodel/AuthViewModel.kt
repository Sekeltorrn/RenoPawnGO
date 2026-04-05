package com.example.mobileapppawnshop.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mobileapppawnshop.data.model.User
import com.example.mobileapppawnshop.data.repository.AuthRepository
import kotlin.concurrent.thread

/**
 * ViewModel to handle Authentication logic across different screens
 */
class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    // UPDATED: Now holds a Pair.
    // First value is the User (if success), Second value is the Error Message (if failed)
    private val _loginResult = MutableLiveData<Pair<User?, String?>>()
    val loginResult: LiveData<Pair<User?, String?>> = _loginResult

    private val _registerResult = MutableLiveData<Boolean>()
    val registerResult: LiveData<Boolean> = _registerResult

    private val _verificationResult = MutableLiveData<Boolean>()
    val verificationResult: LiveData<Boolean> = _verificationResult

    private val _otpSentResult = MutableLiveData<Boolean>()
    val otpSentResult: LiveData<Boolean> = _otpSentResult

    private val _resetRequestResult = MutableLiveData<Boolean>()
    val resetRequestResult: LiveData<Boolean> = _resetRequestResult

    private val _passwordUpdateResult = MutableLiveData<Boolean>()
    val passwordUpdateResult: LiveData<Boolean> = _passwordUpdateResult

    // UPDATED: Posts the Pair returned from the repository
    fun login(email: String, pass: String, schemaName: String) {
        thread {
            val resultPair = repository.login(email, pass, schemaName)
            _loginResult.postValue(resultPair)
        }
    }

    fun register(name: String, email: String, phone: String, pass: String, schemaName: String) {
        thread {
            val success = repository.register(name, email, phone, pass, schemaName)
            _registerResult.postValue(success)
        }
    }

    /**
     * UPDATED: Now accepts 'type' (signup or recovery) and 'shopCode'
     * This ensures verify.php knows which tenant schema to verify against.
     */
    fun verifyCode(email: String, code: String, type: String = "signup", shopCode: String = "") {
        thread {
            val success = repository.verifyCode(email, code, type, shopCode)
            _verificationResult.postValue(success)
        }
    }

    /**
     * Sends a new 6-digit code for Login (2FA)
     */
    fun sendLoginOtp(email: String, shopCode: String = "") {
        thread {
            val success = repository.sendLoginOtp(email, shopCode)
            _otpSentResult.postValue(success)
        }
    }

    fun requestPasswordReset(email: String) {
        thread {
            val success = repository.resetPassword(email)
            _resetRequestResult.postValue(success)
        }
    }

    fun updatePassword(password: String) {
        thread {
            val success = repository.updatePassword(password)
            _passwordUpdateResult.postValue(success)
        }
    }
}