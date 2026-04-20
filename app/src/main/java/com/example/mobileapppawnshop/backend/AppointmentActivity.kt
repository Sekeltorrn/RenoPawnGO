package com.example.mobileapppawnshop.backend

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mobileapppawnshop.R
import com.example.mobileapppawnshop.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class AppointmentActivity : AppCompatActivity() {

    private var selectedService: String = "Pawn Appraisal"
    private lateinit var sessionManager: SessionManager
    
    // Calendar & Time State
    private var currentCalendar: Calendar = Calendar.getInstance()
    private var selectedDate: Calendar? = null
    private var selectedTime: String? = null
    
    // Store Settings & Live Data
    private var storeOpenTime: String = "08:00 AM"
    private var storeCloseTime: String = "05:00 PM"
    private var closedDays: List<String> = listOf("Sunday")
    private var liveAppointments: List<BookedSlot> = emptyList()
    
    // UI Elements
    private lateinit var tvCurrentMonth: TextView
    private lateinit var rvCalendarMonth: RecyclerView
    private lateinit var tvFeedbackRow: TextView
    private lateinit var rvTimeSlots: RecyclerView
    private lateinit var rvAppointmentLog: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.appointment)
        
        sessionManager = SessionManager(this)
        
        // Initialize UI
        tvCurrentMonth = findViewById(R.id.tvCurrentMonth)
        rvCalendarMonth = findViewById(R.id.rvCalendarMonth)
        tvFeedbackRow = findViewById(R.id.tvFeedbackRow)
        rvTimeSlots = findViewById(R.id.rvTimeSlots)
        rvAppointmentLog = findViewById(R.id.rvAppointmentLog)
        
        setupBottomNavigation()
        setupTopBar()
        setupServiceCards()
        setupCalendarNavigation()
        
        // Task 2: Fetch Live Data
        fetchStoreHours()
        fetchLiveSchedule()
        
        // Setup Confirm Button
        findViewById<MaterialButton>(R.id.btnConfirmAppointment).setOnClickListener {
            if (selectedDate == null) {
                Toast.makeText(this, "Please select a date from the calendar.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedTime == null) {
                Toast.makeText(this, "Please select an available time slot.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            processAppointment()
        }
    }

    private fun setupTopBar() {
        // Logout Button
        findViewById<ImageButton>(R.id.btnLogout)?.setOnClickListener {
            startActivity(Intent(this, LogoutActivity::class.java))
        }
        
        // Notifications Button
        findViewById<ImageButton>(R.id.btnNotifications)?.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.navigation_appointments
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> { startActivity(Intent(this, CustomerDashboardActivity::class.java)); finish(); true }
                R.id.navigation_loans -> { startActivity(Intent(this, PawnTicketActiveActivity::class.java)); finish(); true }
                R.id.navigation_payments -> { startActivity(Intent(this, PaymentsActivity::class.java)); finish(); true }
                R.id.navigation_appointments -> true
                R.id.navigation_account -> { startActivity(Intent(this, AccountUpdateActivity::class.java)); finish(); true }
                else -> false
            }
        }
    }

    private fun fetchStoreHours() {
        val tenantSchema = sessionManager.getSchemaName() ?: "public"
        ApiClient.apiService.getStoreHours(TenantRequest(tenantSchema)).enqueue(object : Callback<StoreHoursResponse> {
            override fun onResponse(call: Call<StoreHoursResponse>, response: Response<StoreHoursResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val dataObj = response.body()?.data
                    
                    storeOpenTime = dataObj?.store_open_time ?: "08:00 AM"
                    storeCloseTime = dataObj?.store_close_time ?: "05:00 PM"
                    
                    val rawClosedDays = dataObj?.closed_days
                    closedDays = when (rawClosedDays) {
                        is List<*> -> rawClosedDays.map { it.toString().replace(Regex("[^a-zA-Z]"), "") }
                        is String -> {
                            rawClosedDays.split(",")
                                         .map { it.replace(Regex("[^a-zA-Z]"), "") }
                                         .filter { it.isNotEmpty() }
                        }
                        else -> listOf("Sunday")
                    }

                    // --- NEW: Update the UI Banner ---
                    findViewById<TextView>(R.id.tvStoreHoursLabel)?.text = "Store Hours: $storeOpenTime - $storeCloseTime"
                    val closedText = if (closedDays.isEmpty()) "None" else closedDays.joinToString(", ")
                    findViewById<TextView>(R.id.tvClosedDaysLabel)?.text = "Closed Days: $closedText"
                }
                renderCalendar()
            }
            override fun onFailure(call: Call<StoreHoursResponse>, t: Throwable) {
                renderCalendar()
            }
        })
    }



    // Task 2: Fetch Live Schedule
    private fun fetchLiveSchedule() {
        val tenantSchema = sessionManager.getSchemaName() ?: "public"
        val customerId = sessionManager.getCustomerId() ?: ""
        
        val request = BookedSlotsRequest(tenant_schema = tenantSchema, customer_id = customerId)
        
        ApiClient.apiService.getBookedSlots(request).enqueue(object : Callback<BookedSlotsResponse> {
            override fun onResponse(call: Call<BookedSlotsResponse>, response: Response<BookedSlotsResponse>) {
                if (response.isSuccessful) {
                    liveAppointments = response.body()?.appointments ?: emptyList()
                    renderCalendar() // Re-render calendar to update status dots
                    
                    // Update Appointment Log
                    val myAppts = liveAppointments.filter { it.customer_id == customerId }
                    rvAppointmentLog.layoutManager = LinearLayoutManager(this@AppointmentActivity)
                    rvAppointmentLog.adapter = AppointmentLogAdapter(myAppts)
                }
            }
            override fun onFailure(call: Call<BookedSlotsResponse>, t: Throwable) {
                // Silently fail or log
            }
        })
    }


    private fun setupCalendarNavigation() {
        findViewById<ImageButton>(R.id.btnPrevMonth).setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            renderCalendar()
        }
        findViewById<ImageButton>(R.id.btnNextMonth).setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            renderCalendar()
        }
    }

    private fun renderCalendar() {
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        tvCurrentMonth.text = monthYearFormat.format(currentCalendar.time)
        
        val days = mutableListOf<Calendar?>()
        val cal = currentCalendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
        for (i in 0 until firstDayOfWeek) days.add(null)
        
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..daysInMonth) {
            val day = cal.clone() as Calendar
            day.set(Calendar.DAY_OF_MONTH, i)
            days.add(day)
        }
        
        rvCalendarMonth.layoutManager = GridLayoutManager(this, 7)
        rvCalendarMonth.adapter = CalendarAdapter(days)
    }

    // Task 3: Updated Filtering Logic (Personal Cooldown)
    private fun generateAvailableTimeSlots(date: Calendar) {
        rvTimeSlots.visibility = View.GONE
        tvFeedbackRow.setBackgroundColor(Color.parseColor("#E8F0FE"))

        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedDateStr = sdfDate.format(date.time)
        
        val tvExistingAppointments = findViewById<TextView>(R.id.tvExistingAppointments)
        val myAppointmentsToday = liveAppointments.filter { it.appointment_date == selectedDateStr }

        // --- NEW: Clean UI text instead of a popup ---
        if (myAppointmentsToday.isNotEmpty()) {
            val times = myAppointmentsToday.joinToString(" & ") { it.appointment_time }
            tvExistingAppointments.text = "Your schedule for today: $times"
            tvExistingAppointments.visibility = View.VISIBLE
        } else {
            tvExistingAppointments.visibility = View.GONE
        }

        // Limit Check (Clean Text)
        if (myAppointmentsToday.size >= 2) {
            tvFeedbackRow.text = "Daily limit reached for $selectedDateStr."
            tvFeedbackRow.setTextColor(Color.RED)
            return
        }

        // Base Slots Generation (1-hour intervals)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val openTime = try { timeFormat.parse(storeOpenTime)!! } catch(e: Exception) { timeFormat.parse("08:00 AM")!! }
        val closeTime = try { timeFormat.parse(storeCloseTime)!! } catch(e: Exception) { timeFormat.parse("05:00 PM")!! }
        
        val baseSlots = mutableListOf<String>()
        val slotCal = Calendar.getInstance().apply { time = openTime }
        val closeLimitCal = Calendar.getInstance().apply { time = closeTime }

        while (slotCal.time.before(closeLimitCal.time)) {
            baseSlots.add(timeFormat.format(slotCal.time))
            slotCal.add(Calendar.HOUR_OF_DAY, 1)
        }

        // Current Time Filter (If today)
        val now = Calendar.getInstance()
        val isToday = sdfDate.format(now.time) == selectedDateStr
        
        val currentFilteredSlots = if (isToday) {
            baseSlots.filter { 
                val slotTime = timeFormat.parse(it)!!
                val combinedCal = date.clone() as Calendar
                val slotCalTemp = Calendar.getInstance().apply { time = slotTime }
                combinedCal.set(Calendar.HOUR_OF_DAY, slotCalTemp.get(Calendar.HOUR_OF_DAY))
                combinedCal.set(Calendar.MINUTE, slotCalTemp.get(Calendar.MINUTE))
                combinedCal.timeInMillis > System.currentTimeMillis()
            }
        } else baseSlots

        if (currentFilteredSlots.isEmpty()) {
            tvFeedbackRow.text = "No slots available matching your schedule."
            tvFeedbackRow.setTextColor(Color.parseColor("#64748b"))
        } else {
            selectedTime = null
            tvFeedbackRow.text = "Select an available time for $selectedDateStr."
            tvFeedbackRow.setTextColor(Color.parseColor("#00327d"))
            rvTimeSlots.visibility = View.VISIBLE
            rvTimeSlots.adapter = TimeSlotAdapter(currentFilteredSlots)
        }
    }


    private fun processAppointment() {
        val customerId = sessionManager.getCustomerId()
        val tenantSchema = sessionManager.getSchemaName()

        if (customerId.isNullOrEmpty() || tenantSchema.isNullOrEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.format(selectedDate!!.time)
        
        val displayTimeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dbTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val parsedTime = displayTimeFormat.parse(selectedTime!!)
        val timeForDb = dbTimeFormat.format(parsedTime!!)

        val notes = findViewById<EditText>(R.id.etNotes).text.toString().trim()
        val btnConfirm = findViewById<MaterialButton>(R.id.btnConfirmAppointment)
        btnConfirm.isEnabled = false
        btnConfirm.text = "BOOKING..."

        val request = BookAppointmentRequest(
            tenant_schema = tenantSchema,
            customer_id = customerId,
            appointment_date = date,
            appointment_time = timeForDb,
            purpose = selectedService,
            item_description = notes
        )

        ApiClient.apiService.bookAppointment(request).enqueue(object : Callback<BasicAuthResponse> {
            override fun onResponse(call: Call<BasicAuthResponse>, response: Response<BasicAuthResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    
                    // --- NEW: In-Screen Success Popup ---
                    androidx.appcompat.app.AlertDialog.Builder(this@AppointmentActivity)
                        .setTitle("Booking Confirmed!")
                        .setMessage("Your appointment for $date at $selectedTime has been successfully booked.")
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                            // Reset the UI cleanly
                            selectedTime = null
                            findViewById<EditText>(R.id.etNotes).text.clear()
                            btnConfirm.isEnabled = true
                            btnConfirm.text = "CONFIRM APPOINTMENT"
                            rvTimeSlots.visibility = View.GONE
                            findViewById<TextView>(R.id.tvExistingAppointments).visibility = View.GONE
                            tvFeedbackRow.text = "Select a date to view availability."
                            tvFeedbackRow.setTextColor(Color.parseColor("#64748b"))
                            
                            // Re-fetch to update the calendar dots immediately!
                            fetchLiveSchedule() 
                        }
                        .setCancelable(false)
                        .show()

                } else {
                    Toast.makeText(this@AppointmentActivity, "Booking failed: ${response.body()?.message}", Toast.LENGTH_LONG).show()
                    btnConfirm.isEnabled = true
                    btnConfirm.text = "CONFIRM APPOINTMENT"
                }
            }
            override fun onFailure(call: Call<BasicAuthResponse>, t: Throwable) {
                Toast.makeText(this@AppointmentActivity, "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
                btnConfirm.isEnabled = true
                btnConfirm.text = "CONFIRM APPOINTMENT"
            }
        })
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
                cards.forEach { c -> c.strokeWidth = 0; c.cardElevation = 2f }
                card.strokeWidth = 6
                card.strokeColor = Color.parseColor("#00327d")
                card.cardElevation = 8f
            }
        }
        cardAppraisal.performClick()
    }

    // --- Task 4: Updated CalendarAdapter Dots ---
    inner class CalendarAdapter(private val days: List<Calendar?>) : RecyclerView.Adapter<CalendarAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDayNumber: TextView = view.findViewById(R.id.tvDayNumber)
            val dotRed: View = view.findViewById(R.id.dotRed)
            val dotBlue: View? = view.findViewById(R.id.dotBlue) // Nullable safety
            val dotGreen: View = view.findViewById(R.id.dotGreen)
            val dotYellow: View = view.findViewById(R.id.dotYellow)
            val root: View = view.findViewById(R.id.layoutDayRoot)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val day = days[position]
            if (day == null) {
                holder.tvDayNumber.text = ""
                holder.root.isEnabled = false
                holder.root.setOnClickListener(null) // STRICT BLOCK
                holder.dotRed.visibility = View.GONE
                holder.dotGreen.visibility = View.GONE
                holder.dotYellow.visibility = View.GONE
                holder.root.setBackgroundColor(Color.TRANSPARENT)
                return
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = sdf.format(day.time)
            val dayNum = day.get(Calendar.DAY_OF_MONTH)
            holder.tvDayNumber.text = dayNum.toString()
            
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            val isPast = day.before(today)
            // CRITICAL FIX: Force Locale.US so day names always match the English database
            val dayOfWeek = SimpleDateFormat("EEEE", Locale.US).format(day.time)
            
            // The strings are already perfectly clean from the Regex, just do a direct case-insensitive match
            val isClosed = closedDays.any { it.equals(dayOfWeek, ignoreCase = true) }

            
            // --- THE IRONCLAD STATE MACHINE ---
            if (isPast || isClosed) {
                // STATE: CLOSED OR PAST
                holder.tvDayNumber.setTextColor(Color.parseColor("#94A3B8")) // Slate Gray text
                holder.root.isEnabled = false
                holder.root.isClickable = false
                holder.root.alpha = 0.4f // Visually fade the entire cell
                holder.root.setBackgroundColor(Color.parseColor("#F8FAFC")) // Light gray background
                
                // CRITICAL: Destroy the click listener so it cannot trigger
                holder.root.setOnClickListener(null) 
            } else {
                // STATE: OPEN AND AVAILABLE
                holder.tvDayNumber.setTextColor(Color.BLACK)
                holder.root.isEnabled = true
                holder.root.isClickable = true
                holder.root.alpha = 1.0f // Full opacity
                
                if (selectedDate != null && sdf.format(selectedDate!!.time) == dateStr) {
                    holder.root.setBackgroundColor(Color.parseColor("#E8F0FE")) // Selected Blue
                } else {
                    holder.root.setBackgroundColor(Color.TRANSPARENT)
                }

                // CRITICAL: Only attach the listener if the day is open
                holder.root.setOnClickListener {
                    selectedDate = day
                    notifyDataSetChanged()
                    generateAvailableTimeSlots(day)
                }
            }

            // Dot Logic
            val appts = liveAppointments.filter { it.appointment_date == dateStr }
            val myAppts = appts.filter { it.customer_id == sessionManager.getCustomerId() }
                
            // Limit reached (2 appointments) = RED
            holder.dotRed.visibility = if (myAppts.size >= 2) View.VISIBLE else View.GONE
            
            // Partially booked (1 appointment) = BLUE
            holder.dotBlue?.visibility = if (myAppts.size == 1) View.VISIBLE else View.GONE
            
            // Keep existing status dots
            holder.dotGreen.visibility = if (appts.any { it.status == "completed" }) View.VISIBLE else View.GONE
            holder.dotYellow.visibility = if (appts.any { it.status == "cancelled" }) View.VISIBLE else View.GONE
        }


        override fun getItemCount() = days.size
    }

    inner class TimeSlotAdapter(private val slots: List<String>) : RecyclerView.Adapter<TimeSlotAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTimeValue: TextView = view.findViewById(R.id.tvTimeValue)
            val card: MaterialCardView = view.findViewById(R.id.cardTimeSlot)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_time_slot, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val slot = slots[position]
            holder.tvTimeValue.text = slot
            
            if (selectedTime == slot) {
                holder.card.setCardBackgroundColor(Color.parseColor("#00327d"))
                holder.tvTimeValue.setTextColor(Color.WHITE)
            } else {
                holder.card.setCardBackgroundColor(Color.WHITE)
                holder.tvTimeValue.setTextColor(Color.parseColor("#64748b"))
            }

            holder.card.setOnClickListener {
                selectedTime = slot
                notifyDataSetChanged()
                tvFeedbackRow.text = "Time slot $slot selected for ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate!!.time)}."
                tvFeedbackRow.setTextColor(Color.parseColor("#00327d"))
            }
        }

        override fun getItemCount() = slots.size
    }

    // --- PHASE 3: Appointment Log Adapter ---
    inner class AppointmentLogAdapter(private val myAppointments: List<BookedSlot>) : RecyclerView.Adapter<AppointmentLogAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvService: TextView = view.findViewById(R.id.tvLogService)
            val tvDateTime: TextView = view.findViewById(R.id.tvLogDateTime)
            val tvStatus: TextView = view.findViewById(R.id.tvLogStatus)
            val btnCancel: MaterialButton = view.findViewById(R.id.btnCancelAppointment)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_appointment_log, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val appt = myAppointments[position]
            holder.tvService.text = appt.purpose ?: "Branch Visit"
            holder.tvDateTime.text = "${appt.appointment_date} at ${appt.appointment_time}"
            holder.tvStatus.text = appt.status.uppercase()
            
            // Status Color Coding
            when (appt.status.lowercase()) {
                "completed" -> {
                    holder.tvStatus.setTextColor(Color.parseColor("#2e7d32")) // Green
                    holder.tvStatus.setBackgroundColor(Color.parseColor("#e8f5e9"))
                    holder.btnCancel.visibility = View.GONE
                }
                "cancelled" -> {
                    holder.tvStatus.setTextColor(Color.parseColor("#c62828")) // Red
                    holder.tvStatus.setBackgroundColor(Color.parseColor("#ffebee"))
                    holder.btnCancel.visibility = View.GONE
                }
                "pending" -> {
                    holder.tvStatus.setTextColor(Color.parseColor("#64748b")) // Gray
                    holder.tvStatus.setBackgroundColor(Color.parseColor("#f1f5f9"))
                    holder.btnCancel.visibility = View.VISIBLE
                }
                else -> {
                    holder.tvStatus.setTextColor(Color.parseColor("#64748b"))
                    holder.tvStatus.setBackgroundColor(Color.parseColor("#f1f5f9"))
                    holder.btnCancel.visibility = View.GONE
                }
            }

            holder.btnCancel.setOnClickListener {
                if (appt.appointment_id != null) {
                    performCancellation(appt.appointment_id)
                } else {
                    Toast.makeText(this@AppointmentActivity, "Cannot cancel: Missing ID", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun getItemCount() = myAppointments.size
    }

    private fun performCancellation(appointmentId: String) {
        val tenantSchema = sessionManager.getSchemaName() ?: "public"
        val request = CancelAppointmentRequest(appointment_id = appointmentId, tenant_schema = tenantSchema)
        
        ApiClient.apiService.cancelAppointment(request).enqueue(object : Callback<BasicAuthResponse> {
            override fun onResponse(call: Call<BasicAuthResponse>, response: Response<BasicAuthResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(this@AppointmentActivity, "Appointment Terminated", Toast.LENGTH_SHORT).show()
                    fetchLiveSchedule() // Refresh UI
                } else {
                    Toast.makeText(this@AppointmentActivity, "Fail: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<BasicAuthResponse>, t: Throwable) {
                Toast.makeText(this@AppointmentActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
