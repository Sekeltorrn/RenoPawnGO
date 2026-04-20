package com.example.mobileapppawnshop.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobileapppawnshop.data.model.User
import com.example.mobileapppawnshop.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    // 1. Register
    private val _registerResult = MutableLiveData<Pair<Boolean, String>>()
    val registerResult: LiveData<Pair<Boolean, String>> = _registerResult

    // 2. Register OTP
    private val _verifyRegisterResult = MutableLiveData<Pair<Boolean, String>>()
    val verifyRegisterResult: LiveData<Pair<Boolean, String>> = _verifyRegisterResult

    // 3. Login Auth (Password Check)
    private val _loginAuthResult = MutableLiveData<Pair<Boolean, String>>()
    val loginAuthResult: LiveData<Pair<Boolean, String>> = _loginAuthResult

    // 4. Login OTP (Session Retrieval)
    private val _verifyLoginResult = MutableLiveData<Pair<User?, String>>()
    val verifyLoginResult: LiveData<Pair<User?, String>> = _verifyLoginResult

    fun register(name: String, email: String, phone: String, pass: String, schemaName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _registerResult.postValue(repository.register(name, email, phone, pass, schemaName))
        }
    }

    fun verifyRegisterOtp(email: String, code: String, schemaName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _verifyRegisterResult.postValue(repository.verifyRegisterOtp(email, code, schemaName))
        }
    }

    fun loginAuth(email: String, pass: String, schemaName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loginAuthResult.postValue(repository.loginAuth(email, pass, schemaName))
        }
    }

    fun verifyLoginOtp(email: String, code: String, schemaName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _verifyLoginResult.postValue(repository.verifyLoginOtp(email, code, schemaName) as Pair<User?, String>)
        }
    }

    // --- Phone Login (Mock SMS) ---
    private val _loginPhoneResult = MutableLiveData<Pair<Boolean, String>>()
    val loginPhoneResult: LiveData<Pair<Boolean, String>> = _loginPhoneResult

    fun loginPhone(phone: String, pass: String, schemaName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loginPhoneResult.postValue(repository.loginPhone(phone, pass, schemaName))
        }
    }

    fun verifyMockSms(phone: String, code: String, schemaName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Post result directly into the verifyLoginResult observer so UI flows remain the same
            _verifyLoginResult.postValue(repository.verifyMockSms(phone, code, schemaName) as Pair<User?, String>)
        }
    }

    // --- 5. LEGACY SUPPORT (Forgot Password & Generic Verification) ---
    private val _verificationResult = MutableLiveData<Pair<Boolean, String?>>()
    val verificationResult: LiveData<Pair<Boolean, String?>> = _verificationResult

    private val _otpSentResult = MutableLiveData<Boolean>()
    val otpSentResult: LiveData<Boolean> = _otpSentResult

    private val _forgotPasswordResult = MutableLiveData<Pair<Boolean, String>>()
    val forgotPasswordResult: LiveData<Pair<Boolean, String>> = _forgotPasswordResult

    private val _changePasswordResult = MutableLiveData<Pair<Boolean, String>>()
    val changePasswordResult: LiveData<Pair<Boolean, String>> = _changePasswordResult

    private val _resetPasswordResult = MutableLiveData<Pair<Boolean, String>>()
    val resetPasswordResult: LiveData<Pair<Boolean, String>> = _resetPasswordResult

    fun requestPasswordReset(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _forgotPasswordResult.postValue(repository.requestPasswordReset(email))
        }
    }

    fun changePasswordDirect(email: String, currentPass: String, newPass: String, schemaName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _changePasswordResult.postValue(repository.changePasswordDirect(email, currentPass, newPass, schemaName))
        }
    }

    fun updatePassword(email: String, code: String, newPass: String, schemaName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _resetPasswordResult.postValue(repository.updatePassword(email, code, newPass, schemaName))
        }
    }

    private val _resendResult = MutableLiveData<Pair<Boolean, String>>()
    val resendResult: LiveData<Pair<Boolean, String>> = _resendResult

    fun resendOtp(email: String, type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _resendResult.postValue(repository.resendOtp(email, type))
        }
    }
}