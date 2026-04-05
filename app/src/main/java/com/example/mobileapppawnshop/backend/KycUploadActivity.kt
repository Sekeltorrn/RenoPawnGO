package com.example.mobileapppawnshop.backend

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.mobileapppawnshop.utils.FileUtils
import com.example.mobileapppawnshop.utils.SessionManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

import androidx.compose.foundation.clickable

class KycViewModel : ViewModel() {
    var uploadStatus by mutableStateOf<String?>(null)
    var isUploading by mutableStateOf(false)

    fun uploadId(
        context: android.content.Context,
        frontUri: Uri,
        backUri: Uri,
        customerId: String,
        tenantSchema: String
    ) {
        viewModelScope.launch {
            isUploading = true
            uploadStatus = "Preparing upload..."

            val fileFront = FileUtils.uriToFile(context, frontUri)
            val fileBack = FileUtils.uriToFile(context, backUri)
            
            if (fileFront == null || fileBack == null) {
                uploadStatus = "Error: File processing failed"
                isUploading = false
                return@launch
            }

            val requestFileFront = fileFront.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val bodyFront = MultipartBody.Part.createFormData("id_document", fileFront.name, requestFileFront)
            
            val requestFileBack = fileBack.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val bodyBack = MultipartBody.Part.createFormData("id_document_back", fileBack.name, requestFileBack)

            val customerIdPart = customerId.toRequestBody("text/plain".toMediaTypeOrNull())
            val tenantPart = tenantSchema.toRequestBody("text/plain".toMediaTypeOrNull())
            // Pass legacy strings dynamically
            val idTypePart = "Dual Image Upload".toRequestBody("text/plain".toMediaTypeOrNull())
            val idNumberPart = "N/A".toRequestBody("text/plain".toMediaTypeOrNull())

            try {
                val response = ApiClient.apiService.uploadKyc(
                    customerIdPart,
                    tenantPart,
                    idTypePart,
                    idNumberPart,
                    bodyFront,
                    bodyBack
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    uploadStatus = "Verification Uploaded Successfully!"
                } else {
                    uploadStatus = "Error: ${response.body()?.message ?: "Server Error"}"
                }
            } catch (e: Exception) {
                uploadStatus = "Network Error: ${e.message}"
            } finally {
                isUploading = false
            }
        }
    }
}

class KycUploadActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionManager = SessionManager(this)
        val customerId = sessionManager.getCustomerId() ?: ""
        val tenantSchema = sessionManager.getSchemaName() ?: ""

        setContent {
            KycUploadScreen(customerId, tenantSchema)
        }
    }
}

@Composable
fun KycUploadScreen(customerId: String, tenantSchema: String, viewModel: KycViewModel = viewModel()) {
    var frontImageUri by remember { mutableStateOf<Uri?>(null) }
    var backImageUri by remember { mutableStateOf<Uri?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()

    val frontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) frontImageUri = uri }
    )
    
    val backPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) backImageUri = uri }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text("KYC Verification", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Upload Front of ID", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedCard(
            modifier = Modifier.size(width = 320.dp, height = 200.dp).clickable {
                frontPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (frontImageUri != null) {
                    AsyncImage(
                        model = frontImageUri,
                        contentDescription = "Front of ID",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("Tap to Select Front Image", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Upload Back of ID", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedCard(
            modifier = Modifier.size(width = 320.dp, height = 200.dp).clickable {
                backPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (backImageUri != null) {
                    AsyncImage(
                        model = backImageUri,
                        contentDescription = "Back of ID",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("Tap to Select Back Image", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                if (frontImageUri != null && backImageUri != null) {
                    viewModel.uploadId(context, frontImageUri!!, backImageUri!!, customerId, tenantSchema)
                }
            },
            enabled = frontImageUri != null && backImageUri != null && !viewModel.isUploading,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            if (viewModel.isUploading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Submit Verified Documents")
            }
        }

        viewModel.uploadStatus?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = if (it.contains("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
        }
    }
}