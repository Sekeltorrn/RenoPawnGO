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

class KycViewModel : ViewModel() {
    var uploadStatus by mutableStateOf<String?>(null)
    var isUploading by mutableStateOf(false)

    fun uploadId(
        context: android.content.Context,
        uri: Uri,
        customerId: String,
        tenantSchema: String,
        idType: String,
        idNumber: String
    ) {
        viewModelScope.launch {
            isUploading = true
            uploadStatus = "Preparing upload..."

            val file = FileUtils.uriToFile(context, uri)
            if (file == null) {
                uploadStatus = "Error: File processing failed"
                isUploading = false
                return@launch
            }

            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("id_document", file.name, requestFile)
            val customerIdPart = customerId.toRequestBody("text/plain".toMediaTypeOrNull())
            val tenantPart = tenantSchema.toRequestBody("text/plain".toMediaTypeOrNull())
            val idTypePart = idType.toRequestBody("text/plain".toMediaTypeOrNull())
            val idNumberPart = idNumber.toRequestBody("text/plain".toMediaTypeOrNull())

            try {
                // FIXED: Passing all 5 parameters and handling Response wrapper
                val response = ApiClient.apiService.uploadKyc(
                    customerIdPart,
                    tenantPart,
                    idTypePart,
                    idNumberPart,
                    body
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    uploadStatus = "Upload Successful!"
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
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var idType by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
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

        // ID Document Preview
        if (selectedImageUri != null) {
            AsyncImage(
                model = selectedImageUri,
                contentDescription = "Selected ID Card",
                modifier = Modifier.size(200.dp)
            )
        } else {
            OutlinedCard(modifier = Modifier.size(200.dp)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No image selected")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }) {
            Text("Select ID Card Image")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Added Input Fields
        OutlinedTextField(
            value = idType,
            onValueChange = { idType = it },
            label = { Text("ID Type (e.g. Passport)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = idNumber,
            onValueChange = { idNumber = it },
            label = { Text("ID Number") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                selectedImageUri?.let {
                    viewModel.uploadId(context, it, customerId, tenantSchema, idType, idNumber)
                }
            },
            enabled = selectedImageUri != null && idType.isNotBlank() && idNumber.isNotBlank() && !viewModel.isUploading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (viewModel.isUploading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Submit KYC")
            }
        }

        viewModel.uploadStatus?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = if (it.contains("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
    }
}