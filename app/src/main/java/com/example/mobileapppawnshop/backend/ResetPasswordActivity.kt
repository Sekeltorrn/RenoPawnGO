package com.example.mobileapppawnshop.backend

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.mobileapppawnshop.R
import com.example.mobileapppawnshop.utils.ValidationUtils
import com.example.mobileapppawnshop.viewmodel.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

/**
 * Activity for resetting the user's password
 */
class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Reusing forgot_password_layout logic for simplicity or a specific reset layout if it exists
        // Looking at the requirements, we need a New Password and Confirm Password screen.
        // If a separate layout doesn't exist, we might need to find which one is intended for this.
        // Assuming there might be a missing layout or forgot_password_layout is used.
        // Wait, the prompt says "Reset Password" is a screen already designed.
        // I don't see "reset_password_layout.xml" in the file list. 
        // Let me check if success_confirmation_layout is used for something else.
        
        // Re-checking file list: forgot_password_layout.xml, success_confirmation_layout.xml...
        // Maybe it's not in the list I saw. Let me list files again to be sure.
        setContentView(R.layout.forgot_password_layout) // Temporary placeholder
        
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // Actually, let's look for a layout that has 2 password fields.
        // If not found, I will use forgot_password_layout and adapt if possible, 
        // but the prompt says DO NOT MODIFY XML.
        
        // Let's assume there is a layout I missed or forgot_password_layout is actually the one.
        // But forgot_password_layout only has 1 email field.
    }
}
