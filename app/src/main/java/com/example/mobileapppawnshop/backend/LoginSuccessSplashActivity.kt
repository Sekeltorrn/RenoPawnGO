package com.example.mobileapppawnshop.backend

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapppawnshop.R
import com.google.android.material.card.MaterialCardView

class LoginSuccessSplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_success_splash)

        val logoContainer = findViewById<MaterialCardView>(R.id.cvSplashLogo)
        val appName = findViewById<View>(R.id.tvSplashAppName)
        
        // Facebook-style pulse/zoom animation for the circle container
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        logoContainer.startAnimation(fadeIn)
        appName.startAnimation(fadeIn)
        
        logoContainer.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(1500)
            .withEndAction {
                logoContainer.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(500)
                    .start()
            }
            .start()

        // Wait for 2.5 seconds then go to dashboard
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, CustomerDashboardActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2500)
    }
}
