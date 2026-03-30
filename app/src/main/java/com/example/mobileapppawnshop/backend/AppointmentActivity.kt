package com.example.mobileapppawnshop.backend

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapppawnshop.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.util.Calendar

class AppointmentActivity : AppCompatActivity() {

    private var selectedService: String = "Pawn Appraisal"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.appointment)

        // Setup Bottom Navigation
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.navigation_appointments

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
                R.id.navigation_appointments -> true
                R.id.navigation_account -> {
                    startActivity(Intent(this, AccountUpdateActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        // Setup Top Bar
        findViewById<ImageButton>(R.id.btnLogout).setOnClickListener {
            val intent = Intent(this, LogoutActivity::class.java)
            startActivity(intent)
        }

        // Setup Service Cards logic
        setupServiceCards()

        // Setup Date Picker with Sunday restriction
        val etDate = findViewById<EditText>(R.id.etDate)
        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                
                // Check if selected day is Sunday (Calendar.SUNDAY = 1)
                if (selectedDate.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    Toast.makeText(this, "Sorry, we are closed on Sundays. Please pick another date.", Toast.LENGTH_LONG).show()
                    etDate.setText("")
                } else {
                    val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                    etDate.setText(formattedDate)
                }
            }, year, month, day)
            
            // Optional: Set min date to today
            datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
            datePickerDialog.show()
        }

        // Setup Time Picker with 8 AM to 5 PM restriction
        val etTime = findViewById<EditText>(R.id.etTime)
        etTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                // Check if time is between 8 AM and 5 PM (17:00)
                if (selectedHour < 8 || selectedHour >= 17) {
                    Toast.makeText(this, "Please select a time between 8:00 AM and 5:00 PM.", Toast.LENGTH_LONG).show()
                    etTime.setText("")
                } else {
                    val amPm = if (selectedHour < 12) "AM" else "PM"
                    val hourIn12Format = if (selectedHour % 12 == 0) 12 else selectedHour % 12
                    val formattedTime = String.format("%02d:%02d %s", hourIn12Format, selectedMinute, amPm)
                    etTime.setText(formattedTime)
                }
            }, hour, minute, false)
            
            timePickerDialog.show()
        }

        // Setup Confirm Button
        val btnConfirm = findViewById<MaterialButton>(R.id.btnConfirmAppointment)
        btnConfirm.setOnClickListener {
            it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                it.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).withEndAction {
                    processAppointment()
                }.start()
            }.start()
        }
    }

    private fun setupServiceCards() {
        val cardAppraisal = findViewById<MaterialCardView>(R.id.cardAppraisal)
        val cardLoanPayment = findViewById<MaterialCardView>(R.id.cardLoanPayment)
        val cardRedemption = findViewById<MaterialCardView>(R.id.cardRedemption)
        val cardConsultation = findViewById<MaterialCardView>(R.id.cardConsultation)

        val cards = listOf(cardAppraisal, cardLoanPayment, cardRedemption, cardConsultation)
        val serviceNames = listOf("Pawn Appraisal", "Loan Payment", "Item Redemption", "Consultation")

        cards.forEachIndexed { index, card ->
            card.setOnClickListener {
                selectedService = serviceNames[index]
                cards.forEach { c ->
                    c.strokeWidth = 0
                    c.cardElevation = 2f
                }
                card.strokeWidth = 6
                card.strokeColor = Color.parseColor("#00327d")
                card.cardElevation = 8f

                card.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                    card.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                }.start()
            }
        }
    }

    private fun processAppointment() {
        val date = findViewById<EditText>(R.id.etDate).text.toString()
        val time = findViewById<EditText>(R.id.etTime).text.toString()
        
        if (date.isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }
        if (time.isEmpty()) {
            Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Booking $selectedService at $time on $date...", Toast.LENGTH_SHORT).show()
        btnConfirmAppointmentSuccess()
    }

    private fun btnConfirmAppointmentSuccess() {
        val intent = Intent(this, SuccessConfirmationActivity::class.java)
        startActivity(intent)
        finish()
    }
}
