package com.app.newsapp.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.app.newsapp.R
import com.app.newsapp.auth.AuthRepository
import com.app.newsapp.repository.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * LoginActivity — Firebase-backed login screen.
 *
 * Assignment 5 F1:
 * - Email/Password via AuthRepository → Firebase Auth
 * - Google Sign-In via GoogleSignInClient + Firebase credential exchange
 * - Session check at start — skips login if already logged in
 * - FCM token saved to Firestore on successful login
 * - Existing dark-theme XML layout, error state, and shake animation preserved
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var googleSignInButton: LinearLayout
    private lateinit var skipButton: TextView
    private lateinit var errorText: TextView

    private lateinit var authRepository: AuthRepository
    private lateinit var googleSignInClient: GoogleSignInClient

    // Modern ActivityResult API (replaces deprecated onActivityResult)
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            setLoading(true)
            lifecycleScope.launch {
                when (val authResult = authRepository.signInWithGoogle(account.idToken!!)) {
                    is Result.Success -> {
                        val name = authResult.data.displayName ?: "User"
                        saveFcmTokenToFirestore(authResult.data.uid)
                        navigateToMain(name)
                    }
                    is Result.Error -> {
                        setLoading(false)
                        showError(authResult.message)
                    }
                    else -> setLoading(false)
                }
            }
        } catch (e: ApiException) {
            showError("Google Sign-In failed: ${e.statusCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        authRepository = AuthRepository()

        // F1: Skip login if session already exists
        if (authRepository.isUserLoggedIn()) {
            val name = authRepository.getCurrentUser()?.displayName ?: "User"
            navigateToMain(name)
            return
        }

        bindViews()
        setupWelcomeMessage()
        setupGoogleSignIn()
        setupLoginButton()
        setupGoogleButton()
        setupSkipButton()
    }

    private fun bindViews() {
        emailEditText    = findViewById(R.id.etEmail)
        passwordEditText = findViewById(R.id.etPassword)
        loginButton      = findViewById(R.id.btnLogin)
        googleSignInButton = findViewById(R.id.googleSignInButton)
        skipButton       = findViewById(R.id.tvSkip)
        errorText        = findViewById(R.id.tvError)
    }

    private fun setupWelcomeMessage() {
        val welcomeMessage = intent.getStringExtra("WELCOME_MESSAGE")
        val tvWelcome = findViewById<TextView>(R.id.tvWelcomeMessage)
        if (!welcomeMessage.isNullOrEmpty()) {
            tvWelcome.text = welcomeMessage
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupLoginButton() {
        loginButton.setOnClickListener {
            val email    = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            clearError()

            when {
                email.isEmpty() -> { showFieldError(emailEditText, "Please enter your email"); return@setOnClickListener }
                password.isEmpty() -> { showFieldError(passwordEditText, "Please enter your password"); return@setOnClickListener }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    showFieldError(emailEditText, "Please enter a valid email address")
                    return@setOnClickListener
                }
            }

            setLoading(true)

            lifecycleScope.launch {
                when (val result = authRepository.loginWithEmail(email, password)) {
                    is Result.Success -> {
                        val name = result.data.displayName
                            ?: email.substringBefore("@").replaceFirstChar { it.uppercaseChar() }
                        saveFcmTokenToFirestore(result.data.uid)
                        navigateToMain(name)
                    }
                    is Result.Error -> {
                        setLoading(false)
                        showError(result.message)
                    }
                    else -> setLoading(false)
                }
            }
        }
    }

    private fun setupGoogleButton() {
        googleSignInButton.setOnClickListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun setupSkipButton() {
        skipButton.setOnClickListener { navigateToMain("Guest") }
    }

    /** Save FCM token to Firestore so server can push to this device. */
    private fun saveFcmTokenToFirestore(userId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .set(
                        mapOf("fcmToken" to token, "lastLogin" to System.currentTimeMillis()),
                        SetOptions.merge()
                    )
            } catch (_: Exception) {
                // Silent fail — not critical for login flow
            }
        }
    }

    private fun navigateToMain(userName: String) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            putExtra("USER_NAME", userName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
        val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
        errorText.startAnimation(shake)
    }

    private fun showFieldError(field: EditText, message: String) {
        field.setBackgroundResource(R.drawable.bg_input_error)
        showError(message)
        field.requestFocus()
    }

    private fun clearError() {
        errorText.visibility = View.GONE
        emailEditText.setBackgroundResource(R.drawable.bg_search_bar)
        passwordEditText.setBackgroundResource(R.drawable.bg_search_bar)
    }

    private fun setLoading(loading: Boolean) {
        loginButton.isEnabled = !loading
        loginButton.text = if (loading) "Please wait..." else "Login"
        googleSignInButton.isEnabled = !loading
    }
}
