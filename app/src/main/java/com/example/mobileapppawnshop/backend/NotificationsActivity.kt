package com.example.mobileapppawnshop.backend

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileapppawnshop.R
import com.example.mobileapppawnshop.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationsActivity : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var layoutEmptyNotifications: LinearLayout
    private lateinit var adapter: NotificationAdapter
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        sessionManager = SessionManager(this)

        // Initialize Views
        rvNotifications = findViewById(R.id.rvNotifications)
        layoutEmptyNotifications = findViewById(R.id.layoutEmptyNotifications)
        
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Setup RecyclerView
        rvNotifications.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter(emptyList())
        rvNotifications.adapter = adapter

        fetchNotifications()
    }

    private fun fetchNotifications() {
        val tenantSchema = sessionManager.getTenantSchema() ?: ""
        val customerId = sessionManager.getCustomerId() ?: ""

        if (tenantSchema.isEmpty() || customerId.isEmpty()) {
            showEmptyState()
            return
        }

        val request = TenantCustomerRequest(tenantSchema, customerId)
        
        ApiClient.apiService.getNotifications(request).enqueue(object : Callback<NotificationResponse> {
            override fun onResponse(call: Call<NotificationResponse>, response: Response<NotificationResponse>) {
                if (response.isSuccessful) {
                    val notificationResponse = response.body()
                    val notifications = notificationResponse?.data ?: emptyList()

                    if (notifications.isEmpty()) {
                        showEmptyState()
                    } else {
                        showNotifications(notifications)
                    }
                } else {
                    Toast.makeText(this@NotificationsActivity, "Failed to fetch notifications", Toast.LENGTH_SHORT).show()
                    showEmptyState()
                }
            }

            override fun onFailure(call: Call<NotificationResponse>, t: Throwable) {
                Toast.makeText(this@NotificationsActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                showEmptyState()
            }
        })
    }

    private fun showEmptyState() {
        layoutEmptyNotifications.visibility = View.VISIBLE
        rvNotifications.visibility = View.GONE
    }

    private fun showNotifications(notifications: List<NotificationItem>) {
        layoutEmptyNotifications.visibility = View.GONE
        rvNotifications.visibility = View.VISIBLE
        adapter.updateData(notifications)
    }
}
