package com.example.mobileapppawnshop.backend

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapppawnshop.R
import com.example.mobileapppawnshop.utils.SessionManager
import com.google.android.material.button.MaterialButton

class GatewaySplashActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gateway_splash_layout)

        sessionManager = SessionManager(this)

        // 1. Retrieve the Shop Info
        val shopName = intent.getStringExtra("shop_name") ?: sessionManager.getShopName() ?: "Pawnshop"
        val schemaName = intent.getStringExtra("schema_name") ?: sessionManager.getSchemaName() ?: ""

        val tvShopName = findViewById<TextView>(R.id.tvShopName)
        tvShopName.text = shopName

        val btnEnterTerminal = findViewById<MaterialButton>(R.id.btnEnterTerminal)
        val tvWrongShop = findViewById<TextView>(R.id.tvWrongShop)

        // 2. PATH A: Correct Shop -> Proceed to Login
        btnEnterTerminal.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java).apply {
                putExtra("shop_name", shopName)
                putExtra("schema_name", schemaName)
            }
            startActivity(intent)
            finish()
        }

        // 3. PATH B: Wrong Shop -> Back to Code Screen
        tvWrongShop.setOnClickListener {
            sessionManager.logoutUser() // Clears the incorrect shop code from memory
            val intent = Intent(this, ShopCodeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
