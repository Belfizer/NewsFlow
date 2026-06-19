package com.app.newsapp.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.app.newsapp.repository.Result
import kotlinx.coroutines.tasks.await

/**
 * AuthRepository — Firebase Authentication wrapper.
 *
 * Assignment 5 F1:
 * - Email/Password sign in with specific error messages
 * - Google Sign-In via credential exchange
 * - User registration with display name update
 * - Session persistence check
 */
class AuthRepository {

    private val firebaseAuth = FirebaseAuth.getInstance()

    /** Returns true if a user session is already active. */
    fun isUserLoggedIn(): Boolean = firebaseAuth.currentUser != null

    /** Returns the currently signed-in FirebaseUser, or null. */
    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    /** Sign in with email and password. Returns Success(user) or Error(message). */
    suspend fun loginWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Result.Success(result.user!!)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.Error("No account found with this email address.")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.Error("Incorrect password. Please try again.")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Login failed. Please try again.")
        }
    }

    /** Register a new account with email, password, and full name. */
    suspend fun registerWithEmail(email: String, password: String, fullName: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build()
            result.user!!.updateProfile(profileUpdates).await()
            Result.Success(result.user!!)
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.Error("Password must be at least 6 characters.")
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.Error("An account with this email already exists.")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Registration failed.")
        }
    }

    /** Exchange a Google ID token for a Firebase credential and sign in. */
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            Result.Success(result.user!!)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Google Sign-In failed.")
        }
    }

    /** Sign out the current user. */
    fun logout() {
        firebaseAuth.signOut()
    }
}
