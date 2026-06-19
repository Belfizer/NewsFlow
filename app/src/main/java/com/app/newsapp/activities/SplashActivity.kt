package com.app.newsapp.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.app.newsapp.R

/**
 * SplashActivity — Firebase session-aware entry point.
 *
 * Assignment 5 F1:
 * - If user is already signed in via Firebase Auth → skip login → go directly to MainActivity
 * - If not → navigate to LoginActivity as usual
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // Already logged in — skip login screen entirely
                val name = currentUser.displayName ?: "User"
                startActivity(Intent(this, MainActivity::class.java).apply {
                    putExtra("USER_NAME", name)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            } else {
                // Not logged in — go to login
                startActivity(Intent(this, LoginActivity::class.java).apply {
                    putExtra("WELCOME_MESSAGE", "Stay informed, stay ahead.")
                })
            }
            finish()
        }, 2000)
    }
}
