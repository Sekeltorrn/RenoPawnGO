package com.example.mobileapppawnshop.backend

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import coil.load
import com.example.mobileapppawnshop.R
import com.example.mobileapppawnshop.utils.SessionManager
import com.example.mobileapppawnshop.viewmodel.AuthViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class AccountUpdateActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var authViewModel: AuthViewModel
    private var selectedImageUri: Uri? = null
    private var selectedImageUriBack: Uri? = null

    // For Revert Logic
    private var originalEmail = ""
    private var originalMobile = ""
    private var originalAddress = ""
    private var currentRequestId: String? = null
    private var kycStatus = "unverified"
    private var pendingFields: List<String> = emptyList()

    // Photo Picker Launcher (FRONT)
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            val ivPreview = findViewById<ImageView>(R.id.ivIdPhotoPreview)
            ivPreview.setImageURI(it)
            ivPreview.scaleType = ImageView.ScaleType.CENTER_CROP
            ivPreview.setPadding(0, 0, 0, 0)
            findViewById<TextView>(R.id.tvPhotoHint).text = "Front Photo Selected"
        }
    }

    // Photo Picker Launcher (BACK)
    private val pickImageLauncherBack = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUriBack = it
            val ivPreviewBack = findViewById<ImageView>(R.id.ivIdPhotoPreviewBack)
            ivPreviewBack.setImageURI(it)
            ivPreviewBack.scaleType = ImageView.ScaleType.CENTER_CROP
            ivPreviewBack.setPadding(0, 0, 0, 0)
            findViewById<TextView>(R.id.tvPhotoHintBack).text = "Back Photo Selected"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.account_update)

        sessionManager = SessionManager(this)
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        setupBottomNavigation()
        setupTopBar()
        setupUserDetails()
        setupIdTypeDropdown()
        setupClickListeners()

        // Recovery Path (OTP Flow)
        authViewModel.forgotPasswordResult.observe(this) { result ->
            if (result.first) {
                Toast.makeText(this, "Security code sent to your email.", Toast.LENGTH_LONG).show()
                val intent = Intent(this, ForgotVerifyActivity::class.java).apply {
                    putExtra("email", originalEmail) 
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, result.second, Toast.LENGTH_LONG).show()
            }
        }

        // Card Dismissal Logic removal (handled by state machine)
    }

    override fun onResume() {
        super.onResume()
        fetchUserProfile()
    }

    private fun fetchUserProfile() {
        val customerId = sessionManager.getCustomerId() ?: ""
        val tenantSchema = sessionManager.getSchemaName() ?: ""

        if (customerId.isEmpty() || tenantSchema.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getUserProfile(customerId, tenantSchema).execute()
                if (response.isSuccessful && response.body()?.success == true) {
                    val profile = response.body()!!
                    withContext(Dispatchers.Main) {
                        // Profile Information Mapping
                        findViewById<EditText>(R.id.etFirstName).setText(profile.first_name ?: "")
                        findViewById<EditText>(R.id.etMiddleName).setText(profile.middle_name ?: "")
                        findViewById<EditText>(R.id.etLastName).setText(profile.last_name ?: "")
                        findViewById<EditText>(R.id.etAddress).setText(profile.address ?: "Not set")
                        
                        // ADD THIS LINE FOR BIRTHDAY
                        findViewById<TextView>(R.id.tvDOB).text = profile.birthday ?: "--/--/----"

                        findViewById<EditText>(R.id.etEmail).setText(profile.email ?: "")
                        findViewById<EditText>(R.id.etMobile).setText(profile.contact_no ?: "")
                        
                        originalEmail = profile.email ?: ""
                        originalMobile = profile.contact_no ?: ""
                        originalAddress = profile.address ?: ""

                        // Field Logic Implementation
                        val kycStatus = profile.kyc_status ?: "unverified"
                        val pending = profile.pending_fields ?: emptyList()
                        val requested = profile.requested_fields ?: emptyList()
                        val reqStatus = profile.latest_request_status
                        val reqId = profile.latest_request_id
                        val isAnyPending = pending.isNotEmpty()
                        
                        currentRequestId = reqId
                        
                        // 1. Lock/Hide all Pen icons if ANY request is pending
                        findViewById<ImageButton>(R.id.btnEditEmail).visibility = if (isAnyPending) View.GONE else View.VISIBLE
                        findViewById<ImageButton>(R.id.btnEditMobile).visibility = if (isAnyPending) View.GONE else View.VISIBLE
                        findViewById<ImageButton>(R.id.btnEditAddress).visibility = if (isAnyPending) View.GONE else View.VISIBLE
 
                        // 2. Surgical Highlighting (Only highlight what is actually changed)
                        findViewById<EditText>(R.id.etEmail).setBackgroundResource(if (pending.contains("email")) R.drawable.bg_field_pending_yellow else R.drawable.bg_field_normal)
                        findViewById<EditText>(R.id.etMobile).setBackgroundResource(if (pending.contains("contact_no")) R.drawable.bg_field_pending_yellow else R.drawable.bg_field_normal)
                        findViewById<EditText>(R.id.etAddress).setBackgroundResource(if (pending.contains("address")) R.drawable.bg_field_pending_yellow else R.drawable.bg_field_normal)
 
                        // 3. Dynamic Alert State Machine
                        val cardAlert = findViewById<MaterialCardView>(R.id.cardDynamicAlert)
                        val tvAlertTitle = findViewById<TextView>(R.id.tvAlertTitle)
                        val tvAlertMessage = findViewById<TextView>(R.id.tvAlertMessage)
                        val btnAlertClose = findViewById<ImageButton>(R.id.btnAlertClose)
 
                        // Helper function to inject state
                        fun showDynamicAlert(title: String, message: String, bgColor: String, strokeColor: String, titleColor: String) {
                            cardAlert.visibility = View.VISIBLE
                            tvAlertTitle.text = title
                            tvAlertMessage.text = message
                            cardAlert.setCardBackgroundColor(Color.parseColor(bgColor))
                            cardAlert.strokeColor = Color.parseColor(strokeColor)
                            tvAlertTitle.setTextColor(Color.parseColor(titleColor))
                            tvAlertMessage.setTextColor(Color.parseColor(titleColor))
                        }
 
                        // Retrieve session states
                        val currentAlertState = if (reqId != null && reqStatus != null) "${reqId}_${reqStatus}" else ""
                        val lastDismissedState = sessionManager.getString("LAST_DISMISSED_ALERT_STATE") ?: ""
                        val isKycDismissed = sessionManager.getBoolean(SessionManager.IS_KYC_CARD_DISMISSED)

                        // Update member variables (if still needed)
                        this@AccountUpdateActivity.kycStatus = kycStatus
                        this@AccountUpdateActivity.pendingFields = pending
                        this@AccountUpdateActivity.currentRequestId = reqId

                        when {
                            // PRIORITY 1: PENDING APPROVAL (Yellow)
                            reqStatus == "pending" -> {
                                if (currentAlertState != lastDismissedState) {
                                    showDynamicAlert(
                                        title = "PENDING APPROVAL",
                                        message = "Change requested for: ${pending.joinToString(", ").uppercase()}.",
                                        bgColor = "#FFF9C4", strokeColor = "#FBC02D", titleColor = "#F57F17"
                                    )
                                    btnAlertClose.setOnClickListener {
                                        cardAlert.visibility = View.GONE
                                        sessionManager.saveString("LAST_DISMISSED_ALERT_STATE", currentAlertState)
                                    }
                                } else cardAlert.visibility = View.GONE
                            }

                            // PRIORITY 2: APPROVED
                            reqStatus == "approved" -> {
                                // Check the independent SecurityPrefs flag to prevent infinite loops
                                val securityPrefs = getSharedPreferences("SecurityPrefs", android.content.Context.MODE_PRIVATE)
                                val ackId = securityPrefs.getString("ACK_REQ_ID", "")

                                if (requested.contains("email") && reqId != ackId) {
                                    // The user's email was approved, and they HAVEN'T logged out for this specific request yet.
                                    androidx.appcompat.app.AlertDialog.Builder(this@AccountUpdateActivity)
                                        .setTitle("Security Update Required")
                                        .setMessage("Your email change request was approved. You must log in again with your new email address to continue.")
                                        .setCancelable(false) // Trap the user
                                        .setPositiveButton("Log Out") { _, _ ->
                                            // 1. Save the flag so they aren't trapped when they log back in
                                            if (reqId != null) {
                                                securityPrefs.edit().putString("ACK_REQ_ID", reqId).apply()
                                            }
                                            
                                            // 2. Wipe the session completely
                                            sessionManager.logoutUser()
                                            
                                            // 3. INSTANT KILL: Hard redirect directly to LoginActivity (bypassing LogoutActivity)
                                            val intent = Intent(this@AccountUpdateActivity, LoginActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                            finishAffinity()
                                        }
                                        .show()
                                } else if (!requested.contains("email")) {
                                    // Standard Blue Alert for Mobile/Address approval (unchanged)
                                    if (currentAlertState != lastDismissedState) {
                                        showDynamicAlert(
                                            title = "REQUEST APPROVED",
                                            message = "Your change for ${requested.joinToString(", ").uppercase()} was approved.",
                                            bgColor = "#EFF6FF", strokeColor = "#3B82F6", titleColor = "#1D4ED8"
                                        )
                                        btnAlertClose.setOnClickListener {
                                            cardAlert.visibility = View.GONE
                                            sessionManager.saveString("LAST_DISMISSED_ALERT_STATE", currentAlertState)
                                        }
                                    } else cardAlert.visibility = View.GONE
                                } else {
                                    // The request was an email, BUT the user already logged out and logged back in (reqId == ackId).
                                    // Silently hide the alert so they can continue using the app normally.
                                    cardAlert.visibility = View.GONE
                                }
                            }

                            // PRIORITY 3: REJECTED (Red)
                            reqStatus == "rejected" -> {
                                if (currentAlertState != lastDismissedState) {
                                    showDynamicAlert(
                                        title = "REQUEST DENIED",
                                        message = "Your profile change request was not approved by staff.",
                                        bgColor = "#FEF2F2", // Light Red
                                        strokeColor = "#EF4444", // Solid Red
                                        titleColor = "#B91C1C" // Dark Red text
                                    )
                                    btnAlertClose.setOnClickListener {
                                        cardAlert.visibility = View.GONE
                                        sessionManager.saveString("LAST_DISMISSED_ALERT_STATE", currentAlertState)
                                    }
                                } else cardAlert.visibility = View.GONE
                            }

                            // PRIORITY 4: ACTION REQUIRED (KYC Not Done)
                            kycStatus != "verified" -> {
                                showDynamicAlert(
                                    title = "ACTION REQUIRED",
                                    message = "Please complete your government ID verification.",
                                    bgColor = "#FEE2E2", strokeColor = "#F87171", titleColor = "#B91C1C"
                                )
                                btnAlertClose.visibility = View.GONE // Mandatory, cannot dismiss
                            }

                            // PRIORITY 5: 100% VERIFIED (Green)
                            kycStatus == "verified" && !isKycDismissed -> {
                                showDynamicAlert(
                                    title = "100% VERIFIED",
                                    message = "Your identity is fully verified.",
                                    bgColor = "#DCFCE7", strokeColor = "#4ADE80", titleColor = "#15803D"
                                )
                                btnAlertClose.visibility = View.VISIBLE
                                btnAlertClose.setOnClickListener {
                                    cardAlert.visibility = View.GONE
                                    sessionManager.setBoolean(SessionManager.IS_KYC_CARD_DISMISSED, true)
                                }
                            }

                            // DEFAULT: Hide everything
                            else -> cardAlert.visibility = View.GONE
                        }

                        updateKycUiState(
                            kycStatus, 
                            profile.rejection_reason, 
                            profile.id_photo_front_url, 
                            profile.id_photo_back_url
                        )
                        
                        // Header greeting synchronization
                        findViewById<TextView>(R.id.tvUsername).text = "${profile.first_name} ${profile.last_name}"
                        updateHeaderGreeting(profile.first_name, profile.middle_name, profile.last_name)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AccountUpdateActivity, "Identity Sync Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateHeaderGreeting(first: String?, middle: String?, last: String?) {
        val fullName = listOfNotNull(first, middle, last).joinToString(" ").replace("  ", " ").trim()
        if (fullName.isNotEmpty()) {
            findViewById<TextView>(R.id.tvUsername).text = fullName
            findViewById<TextView>(R.id.tvFullName).apply {
                text = fullName
                visibility = View.VISIBLE
            }
        }
    }

    private fun updateKycUiState(status: String, rejectionReason: String?, frontUrl: String?, backUrl: String?) {
        val layoutIdInput = findViewById<LinearLayout>(R.id.layoutIdInput)
        val layoutIdVerified = findViewById<LinearLayout>(R.id.layoutIdVerified)
        val tvIdVerificationStatus = findViewById<TextView>(R.id.tvIdVerificationStatus)
        val tvIdVerifiedMessage = findViewById<TextView>(R.id.tvIdVerifiedMessage)
        
        val cardFront = findViewById<MaterialCardView>(R.id.cardVerifiedIdFront)
        val cardBack = findViewById<MaterialCardView>(R.id.cardVerifiedIdBack)
        val ivFront = findViewById<ImageView>(R.id.ivVerifiedIdFront)
        val ivBack = findViewById<ImageView>(R.id.ivVerifiedIdBack)
        
        when (status.lowercase()) {
            "pending", "verified" -> {
                val isVerified = status.lowercase() == "verified"
                val accentColor = if (isVerified) Color.parseColor("#16a34a") else Color.parseColor("#f59e0b")
                val bgColor = if (isVerified) Color.parseColor("#dcfce7") else Color.parseColor("#fef3c7")
                val textColor = if (isVerified) Color.parseColor("#16a34a") else Color.parseColor("#b45309")

                layoutIdInput.visibility = View.GONE
                layoutIdVerified.visibility = View.VISIBLE
                
                tvIdVerificationStatus.text = if (isVerified) "VERIFIED" else "PENDING REVIEW"
                tvIdVerificationStatus.setBackgroundColor(bgColor)
                tvIdVerificationStatus.setTextColor(textColor)

                tvIdVerifiedMessage.text = if (isVerified) "Identity Verified. Thank you." else "Document Submitted. Pending Admin Review."
                tvIdVerifiedMessage.setTextColor(accentColor)
                
                // Update Card Borders
                cardFront.strokeColor = accentColor
                cardBack.strokeColor = accentColor
                
                // Load Images via Coil
                ivFront.load(frontUrl) {
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_gallery)
                    crossfade(true)
                }
                ivBack.load(backUrl) {
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_gallery)
                    crossfade(true)
                }
            }
            "rejected", "unverified" -> {
                val isRejected = status.lowercase() == "rejected"
                val accentColor = if (isRejected) Color.parseColor("#dc2626") else Color.parseColor("#64748b")
                
                layoutIdInput.visibility = View.VISIBLE
                layoutIdVerified.visibility = View.GONE
                
                tvIdVerificationStatus.text = if (isRejected) "REJECTED" else "UNVERIFIED"
                tvIdVerificationStatus.setTextColor(accentColor)

                if (status == "rejected" && !rejectionReason.isNullOrEmpty()) {
                    Toast.makeText(this, "ID Rejected: $rejectionReason", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.navigation_account

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, CustomerDashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_loans -> {
                    startActivity(Intent(this, PawnTicketActiveActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_payments -> {
                    startActivity(Intent(this, PaymentsActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_appointments -> {
                    startActivity(Intent(this, AppointmentActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_account -> true
                else -> false
            }
        }
    }

    private fun setupUserDetails() {
        val tvUsername = findViewById<TextView>(R.id.tvUsername)
        val tvFullName = findViewById<TextView>(R.id.tvFullName)
        val userName = sessionManager.getUserName() ?: "Not set"
        tvUsername.text = userName
        tvFullName.text = userName
    }

    private fun setupIdTypeDropdown() {
        val idTypes = arrayOf(
            "Passport",
            "Driver’s License",
            "Unified Multi-Purpose ID (UMID)",
            "Philippine Identification (PhilID/ePhilID)",
            "Philippine Postal ID",
            "Social Security System (SSS)"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, idTypes)
        val actvIdType = findViewById<AutoCompleteTextView>(R.id.actvIdType)
        actvIdType.setAdapter(adapter)
    }

    private fun setupClickListeners() {


        // Edit Profile Buttons
        setupEditControls(
            findViewById(R.id.etEmail),
            findViewById(R.id.btnEditEmail),
            findViewById(R.id.layoutConfirmEmail),
            findViewById(R.id.btnApproveEmail),
            findViewById(R.id.btnCancelEmail),
            { originalEmail }
        )

        setupEditControls(
            findViewById(R.id.etMobile),
            findViewById(R.id.btnEditMobile),
            findViewById(R.id.layoutConfirmMobile),
            findViewById(R.id.btnApproveMobile),
            findViewById(R.id.btnCancelMobile),
            { originalMobile }
        )

        setupEditControls(
            findViewById(R.id.etAddress),
            findViewById(R.id.btnEditAddress),
            findViewById(R.id.layoutConfirmAddress),
            findViewById(R.id.btnApproveAddress),
            findViewById(R.id.btnCancelAddress),
            { originalAddress }
        )



        // Photo Upload (Front)
        findViewById<MaterialCardView>(R.id.btnSelectPhoto).setSmoothClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Photo Upload (Back)
        findViewById<MaterialCardView>(R.id.btnSelectPhotoBack).setSmoothClickListener {
            pickImageLauncherBack.launch("image/*")
        }

        // ID Submission
        findViewById<MaterialButton>(R.id.btnSubmitId).setSmoothClickListener {
            handleIdSubmission()
        }



        // 4. RECOVERY PATH: Forgot Password (Triggers existing OTP flow)
        findViewById<MaterialButton>(R.id.btnForgotPasswordInApp).setOnClickListener {
            if (originalEmail.isNotEmpty()) {
                val btn = it as MaterialButton
                btn.isEnabled = false
                btn.text = "SENDING CODE..."
                authViewModel.requestPasswordReset(originalEmail)
            }
        }
    }

    private fun handleIdSubmission() {
        val idType = findViewById<AutoCompleteTextView>(R.id.actvIdType).text.toString()
        
        if (idType.isEmpty() || selectedImageUri == null || selectedImageUriBack == null) {
            Toast.makeText(this, "Please select ID type and both photos.", Toast.LENGTH_SHORT).show()
            return
        }

        // STRICT PRODUCTION CHECK: Ensure we actually have a logged-in user
        val currentCustomerId = sessionManager.getCustomerId()
        if (currentCustomerId.isNullOrEmpty()) {
            Toast.makeText(this, "Authentication Error. Please log in again.", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(this, "Encrypting and uploading to secure server...", Toast.LENGTH_LONG).show()
        
        // Disable button to prevent double-uploading
        val submitBtn = findViewById<MaterialButton>(R.id.btnSubmitId)
        submitBtn.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Process the physical files from the Android URIs
                val contentResolver = applicationContext.contentResolver
                
                // Front Image
                val inputStreamFront = contentResolver.openInputStream(selectedImageUri!!)
                val tempFileFront = File(cacheDir, "kyc_front_${System.currentTimeMillis()}.jpg")
                val outputStreamFront = FileOutputStream(tempFileFront)
                inputStreamFront?.copyTo(outputStreamFront)
                inputStreamFront?.close()
                outputStreamFront.close()

                // Back Image
                val inputStreamBack = contentResolver.openInputStream(selectedImageUriBack!!)
                val tempFileBack = File(cacheDir, "kyc_back_${System.currentTimeMillis()}.jpg")
                val outputStreamBack = FileOutputStream(tempFileBack)
                inputStreamBack?.copyTo(outputStreamBack)
                inputStreamBack?.close()
                outputStreamBack.close()

                // 2. Prepare the exact Multipart Data the PHP server expects
                val requestFileFront = tempFileFront.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imageBodyFront = MultipartBody.Part.createFormData("id_document", tempFileFront.name, requestFileFront)
                
                val requestFileBack = tempFileBack.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val imageBodyBack = MultipartBody.Part.createFormData("id_document_back", tempFileBack.name, requestFileBack)
                
                val customerIdBody = currentCustomerId.toRequestBody("text/plain".toMediaTypeOrNull())
                
                // Pull schema from session
                val schemaName = sessionManager.getSchemaName() ?: "tenant_pwn_18e601"
                val tenantSchemaBody = schemaName.toRequestBody("text/plain".toMediaTypeOrNull())

                // Hardcoded ID number for API compatibility since the field was removed
                val idTypeBody = idType.toRequestBody("text/plain".toMediaTypeOrNull())
                val idNumberBody = "N/A".toRequestBody("text/plain".toMediaTypeOrNull())

                // 3. Fire the secure network request with ALL parameters
                val response = ApiClient.apiService.uploadKyc(
                    customerIdBody, 
                    tenantSchemaBody, 
                    idTypeBody, 
                    idNumberBody, 
                    imageBodyFront,
                    imageBodyBack
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        // Success! Clean up temp files
                        tempFileFront.delete()
                        tempFileBack.delete()

                        Toast.makeText(this@AccountUpdateActivity, "Document securely submitted. Pending Admin Review.", Toast.LENGTH_LONG).show()
                        
                        // Let result logic handle the UI swap via refresh
                        onResume()
                    } else {
                        Toast.makeText(this@AccountUpdateActivity, "Server Rejected Request: Code ${response.code()}", Toast.LENGTH_LONG).show()
                        submitBtn.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AccountUpdateActivity, "Upload Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    submitBtn.isEnabled = true
                }
            }
        }
    }

    private fun handleProfileChangeRequest(email: String, mobile: String, address: String, fieldName: String) {
        val customerId = sessionManager.getCustomerId() ?: ""
        val tenantSchema = sessionManager.getSchemaName() ?: ""

        // THE IRONCLAD PATCH: Only validate the specific field being updated
        if ((fieldName == "email" && email.trim().isEmpty()) ||
            (fieldName == "contact_no" && mobile.trim().isEmpty()) ||
            (fieldName == "address" && address.trim().isEmpty())) {
            Toast.makeText(this, "The field you are changing cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.requestProfileChange(customerId, tenantSchema, email, mobile, address).execute()
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val body = response.body()!!
                        currentRequestId = body.request_id
                        Toast.makeText(this@AccountUpdateActivity, "Update request sent.", Toast.LENGTH_SHORT).show()
                        
                        // Wipe the alert memory so the new pending card MUST show
                        sessionManager.saveString("LAST_DISMISSED_ALERT_STATE", "")
                        
                        applyPendingState(fieldName)
                        
                        // Refresh the screen to show the alert card immediately
                        onResume()
                    } else {
                        Toast.makeText(this@AccountUpdateActivity, "Request Failed: ${response.body()?.message ?: response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AccountUpdateActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun applyPendingState(fieldName: String) {
        val (editText, layoutOriginal, tvOriginalValue) = when (fieldName) {
            "email" -> Triple(findViewById<EditText>(R.id.etEmail), findViewById<LinearLayout>(R.id.layoutOriginalEmail), findViewById<TextView>(R.id.tvOriginalEmailValue))
            "contact_no" -> Triple(findViewById<EditText>(R.id.etMobile), findViewById<LinearLayout>(R.id.layoutOriginalMobile), findViewById<TextView>(R.id.tvOriginalMobileValue))
            "address" -> Triple(findViewById<EditText>(R.id.etAddress), null, null)
            else -> Triple(null, null, null)
        }

        editText?.let {
            it.isEnabled = false
            it.isFocusable = false
            it.isFocusableInTouchMode = false
            it.setBackgroundResource(R.drawable.bg_field_pending_yellow)
            it.setPadding(16, 8, 16, 8)
            
            // Task 3: Force visibility in pending state
            layoutOriginal?.visibility = View.VISIBLE
            tvOriginalValue?.text = when(fieldName) {
                "email" -> originalEmail
                "contact_no" -> originalMobile
                else -> "---"
            }
        }
    }

    private fun setupEditControls(
        editText: EditText,
        btnEdit: ImageButton,
        layoutConfirm: LinearLayout,
        btnApprove: ImageButton,
        btnCancel: ImageButton,
        originalValueProvider: () -> String
    ) {
        val (layoutOriginal, tvOriginalValue) = when (editText.id) {
            R.id.etEmail -> findViewById<LinearLayout>(R.id.layoutOriginalEmail) to findViewById<TextView>(R.id.tvOriginalEmailValue)
            R.id.etMobile -> findViewById<LinearLayout>(R.id.layoutOriginalMobile) to findViewById<TextView>(R.id.tvOriginalMobileValue)
            else -> null to null
        }

        btnEdit.setOnClickListener {
            editText.isEnabled = true
            editText.isFocusable = true
            editText.isFocusableInTouchMode = true
            editText.requestFocus()
            
            // Show Keyboard
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            
            // Clear yellow background and set to editable style
            editText.setBackgroundResource(R.drawable.input_outline_blue)
            editText.setPadding(16, 8, 16, 8)

            // Task 2: Show original value layout
            layoutOriginal?.visibility = View.VISIBLE
            tvOriginalValue?.text = originalValueProvider()

            btnEdit.visibility = View.GONE
            layoutConfirm.visibility = View.VISIBLE
        }

        btnCancel.setOnClickListener {
            editText.setText(originalValueProvider())
            editText.isEnabled = false
            editText.isFocusable = false
            editText.isFocusableInTouchMode = false
            
            // Task 2: Hide original value layout
            layoutOriginal?.visibility = View.GONE
            
            onResume()
            btnEdit.visibility = View.VISIBLE
            layoutConfirm.visibility = View.GONE
        }

        btnApprove.setOnClickListener {
            val newValue = editText.text.toString().trim()
            val originalValue = originalValueProvider()

            if (newValue == originalValue || newValue.isEmpty()) {
                Toast.makeText(this@AccountUpdateActivity, "No changes detected.", Toast.LENGTH_SHORT).show()
                btnCancel.performClick()
                return@setOnClickListener
            }

            val fieldName = when (editText.id) {
                R.id.etEmail -> "email"
                R.id.etMobile -> "contact_no"
                R.id.etAddress -> "address"
                else -> "unknown"
            }

            if (fieldName == "email") {
                // 1. Request OTP
                Toast.makeText(this@AccountUpdateActivity, "Requesting security code...", Toast.LENGTH_SHORT).show()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val schema = sessionManager.getSchemaName() ?: ""
                        val response = ApiClient.apiService.requestChangeEmailOtp(RequestEmailChangeRequest(newValue, schema)).execute()
                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful && response.body()?.status == "success") {
                                // 2. Show OTP Input Dialog
                                val input = EditText(this@AccountUpdateActivity).apply {
                                    hint = "Enter 6-digit OTP"
                                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                                    setPadding(50, 40, 50, 40)
                                }
                                androidx.appcompat.app.AlertDialog.Builder(this@AccountUpdateActivity)
                                    .setTitle("Verify New Email")
                                    .setMessage("Enter the code sent to $newValue")
                                    .setView(input)
                                    .setCancelable(false)
                                    .setPositiveButton("Verify") { _, _ ->
                                        val code = input.text.toString().trim()
                                        // 3. Verify OTP
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val verifyRes = ApiClient.apiService.verifyChangeEmailOtp(VerifyEmailChangeRequest(newValue, code)).execute()
                                            withContext(Dispatchers.Main) {
                                                if (verifyRes.isSuccessful && verifyRes.body()?.status == "success") {
                                                    // 4. CRITICAL FIX: Pass newValue as the email, original values for the rest!
                                                    handleProfileChangeRequest(newValue, originalMobile, originalAddress, "email")
                                                } else {
                                                    Toast.makeText(this@AccountUpdateActivity, "Invalid Code.", Toast.LENGTH_LONG).show()
                                                    btnCancel.performClick()
                                                }
                                            }
                                        }
                                    }
                                    .setNegativeButton("Cancel") { _, _ -> btnCancel.performClick() }
                                    .show()
                            } else {
                                Toast.makeText(this@AccountUpdateActivity, "Email unavailable or already exists.", Toast.LENGTH_LONG).show()
                                btnCancel.performClick()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { btnCancel.performClick() }
                    }
                }
            } else {
                // DIRECT SUBMISSION FOR MOBILE AND ADDRESS
                val emailToSend = originalEmail
                val mobileToSend = if (fieldName == "contact_no") newValue else originalMobile
                val addressToSend = if (fieldName == "address") newValue else originalAddress
                
                handleProfileChangeRequest(emailToSend, mobileToSend, addressToSend, fieldName)
            }
            
            editText.isEnabled = false
            editText.isFocusable = false
            editText.isFocusableInTouchMode = false
            onResume()
            btnEdit.visibility = View.VISIBLE
            layoutConfirm.visibility = View.GONE
        }
    }

    private fun setFieldPending(view: EditText) {
        // Use applyPendingState instead
    }


    private fun setupTopBar() {
        // Logout Button
        findViewById<ImageButton>(R.id.btnLogout)?.setOnClickListener {
            startActivity(Intent(this, LogoutActivity::class.java))
        }
        
        // Notifications Button
        findViewById<ImageButton>(R.id.btnNotifications)?.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
    }


    // Helper extension for smooth animation
    private fun View.setSmoothClickListener(action: () -> Unit) {
        this.setOnClickListener {
            this.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                this.animate().scaleX(1f).scaleY(1f).setDuration(100).withEndAction {
                    action()
                }.start()
            }.start()
        }
    }
}