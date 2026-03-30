package com.example.mobileapppawnshop.backend

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapppawnshop.R
import com.example.mobileapppawnshop.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class ShopCodeActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.shop_code_layout)

        sessionManager = SessionManager(this)

        val etShopCode = findViewById<TextInputEditText>(R.id.etShopCode)
        val btnContinue = findViewById<MaterialButton>(R.id.btnContinue)

        btnContinue.setOnClickListener {
            val shopCode = etShopCode.text.toString().trim()

            if (shopCode.isEmpty()) {
                etShopCode.error = "Please enter a valid shop code"
                return@setOnClickListener
            }

            btnContinue.isEnabled = false
            btnContinue.text = "Connecting..."

            thread {
                try {
                    val url = URL("https://pawnereno.onrender.com/src/mobile_api.php")
                    val conn = url.openConnection() as HttpURLConnection

                    conn.connectTimeout = 60000
                    conn.readTimeout = 60000
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true

                    val jsonRequest = JSONObject().apply {
                        put("action", "connect_shop")
                        put("shop_code", shopCode)
                    }

                    conn.outputStream.use { it.write(jsonRequest.toString().toByteArray()) }

                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val responseJson = JSONObject(responseText)
                    val status = responseJson.getString("status")

                    runOnUiThread {
                        btnContinue.isEnabled = true
                        btnContinue.text = "Continue"

                        if (status == "success") {
                            val shopName = responseJson.getString("shop_name")
                            val schemaName = responseJson.getString("schema_name")

                            // SAVE TO SESSION
                            sessionManager.saveShopDetails(shopCode, schemaName, shopName)

                            Toast.makeText(this@ShopCodeActivity, "Connected to: $shopName", Toast.LENGTH_SHORT).show()

                            val intent = Intent(this@ShopCodeActivity, LoginActivity::class.java).apply {
                                putExtra("schema_name", schemaName)
                                putExtra("shop_name", shopName)
                            }
                            startActivity(intent)
                            finish()
                        } else {
                            val message = responseJson.getString("message")
                            etShopCode.error = message
                            Toast.makeText(this@ShopCodeActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        btnContinue.isEnabled = true
                        btnContinue.text = "Continue"
                        Toast.makeText(this@ShopCodeActivity, "Connection Error: Check Connection!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}