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
import com.example.mobileapppawnshop.R
import com.example.mobileapppawnshop.utils.SessionManager
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
    private var selectedImageUri: Uri? = null
    private var selectedImageUriBack: Uri? = null

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

        setupBottomNavigation()
        setupUserDetails()
        setupIdTypeDropdown()
        setupClickListeners()
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
        // Logout
        findViewById<ImageButton>(R.id.btnLogout).setSmoothClickListener {
            startActivity(Intent(this, LogoutActivity::class.java))
        }

        // Edit Profile Buttons
        findViewById<ImageButton>(R.id.btnEditEmail).setSmoothClickListener {
            Toast.makeText(this, "Edit Email", Toast.LENGTH_SHORT).show()
        }
        findViewById<ImageButton>(R.id.btnEditMobile).setSmoothClickListener {
            Toast.makeText(this, "Edit Mobile", Toast.LENGTH_SHORT).show()
        }
        findViewById<ImageButton>(R.id.btnEditAddress).setSmoothClickListener {
            Toast.makeText(this, "Edit Address", Toast.LENGTH_SHORT).show()
        }

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

        // Update Password
        findViewById<MaterialButton>(R.id.btnUpdatePassword).setSmoothClickListener {
            Toast.makeText(this, "Feature coming soon", Toast.LENGTH_SHORT).show()
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
                        
                        // Update UI to the Pending/Amber State
                        findViewById<LinearLayout>(R.id.layoutIdInput).visibility = View.GONE
                        findViewById<TextView>(R.id.tvIdVerificationStatus).visibility = View.GONE
                        findViewById<LinearLayout>(R.id.layoutIdVerified).visibility = View.VISIBLE
                        findViewById<ImageView>(R.id.ivVerifiedIdPhoto).setImageURI(selectedImageUri)
                        
                        findViewById<TextView>(R.id.tvIdStatus).apply {
                            text = "PENDING REVIEW"
                            setTextColor(Color.parseColor("#f59e0b"))
                        }
                        findViewById<ImageView>(R.id.ivIdCheck).apply {
                            setImageResource(android.R.drawable.ic_popup_sync)
                            setColorFilter(Color.parseColor("#f59e0b"))
                        }
                        
                        // Update Progress
                        findViewById<TextView>(R.id.tvStatusComplete).text = "85% COMPLETE"
                        findViewById<LinearProgressIndicator>(R.id.progressIndicator).progress = 85
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