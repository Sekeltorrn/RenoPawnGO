package com.example.mobileapppawnshop.backend

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapppawnshop.R
import com.example.mobileapppawnshop.utils.SessionManager

class LogoutActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.logout)

        sessionManager = SessionManager(this)

        val btnLogout = findViewById<Button>(R.id.btn_logout)
        val btnCancel = findViewById<Button>(R.id.btn_cancel)

        btnLogout.setOnClickListener {
            // Perform logout logic
            sessionManager.logoutUser()

            // Redirect to LoginActivity and clear activity stack
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        btnCancel.setOnClickListener {
            // Simply return to the previous screen
            finish()
        }
    }
}
