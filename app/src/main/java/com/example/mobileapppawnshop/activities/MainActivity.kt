package com.example.mobileapppawnshop.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapppawnshop.backend.LoginActivity
import com.example.mobileapppawnshop.utils.SessionManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // This activity now only serves as a routing relay.
        // It immediately hands off to LoginActivity, which handles the session-based routing.
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
