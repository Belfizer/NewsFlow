package com.app.newsapp.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.app.newsapp.models.NewsArticle

/**
 * FirestoreHelper — Firestore read/write helpers.
 *
 * Assignment 5 F2:
 * - Collection 1: users/{uid}  — profile data + FCM token
 * - Collection 2: users/{uid}/bookmarks/{articleId} — synced bookmarks
 * - Real-time listener on bookmarks subcollection
 */
class FirestoreHelper {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val uid get() = auth.currentUser?.uid

    /** Save or merge user profile data into Firestore. */
    fun saveUserProfile(name: String, email: String) {
        val userId = uid ?: return
        val user = hashMapOf(
            "userId" to userId,
            "fullName" to name,
            "email" to email,
            "joinedAt" to System.currentTimeMillis(),
            "topCategory" to "Technology"
        )
        db.collection("users").document(userId)
            .set(user, SetOptions.merge())
    }

    /** Write a bookmarked article to Firestore under users/{uid}/bookmarks. */
    fun syncBookmarkToFirestore(article: NewsArticle, category: String) {
        val userId = uid ?: return
        val realUrl = article.url ?: ""
        val titleStr = article.title ?: "Untitled"
        val articleId = (if (realUrl.isNotBlank()) realUrl else "local_${titleStr.hashCode()}").hashCode().toString()

        val bookmark = hashMapOf(
            "userId" to userId,
            "articleId" to articleId,
            "title" to (article.title ?: ""),
            "sourceName" to (article.source?.name ?: ""),
            "description" to (article.description ?: ""),
            "url" to (article.url ?: ""),
            "category" to category,
            "savedAt" to System.currentTimeMillis()
        )
        Log.d("FirestoreHelper", "Attempting to sync bookmark: $titleStr for user: $userId")
        db.collection("users").document(userId)
            .collection("bookmarks").document(articleId)
            .set(bookmark)
            .addOnSuccessListener {
                Log.d("FirestoreHelper", "Successfully synced bookmark: $titleStr")
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreHelper", "Failed to sync bookmark: $titleStr", e)
            }
    }

    /** Delete a bookmark document from Firestore. */
    fun removeBookmarkFromFirestore(articleUrl: String) {
        val userId = uid ?: return
        val articleId = articleUrl.hashCode().toString()
        db.collection("users").document(userId)
            .collection("bookmarks").document(articleId)
            .delete()
    }

    /**
     * Attaches a real-time Firestore snapshot listener on bookmarks.
     * Fires [onUpdate] with the latest list every time bookmarks change.
     * Returns the ListenerRegistration so caller can remove it if needed.
     */
    fun listenToBookmarks(onUpdate: (List<Map<String, Any>>) -> Unit) {
        val userId = uid ?: return
        db.collection("users").document(userId)
            .collection("bookmarks")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val bookmarks = snapshot.documents.mapNotNull { it.data }
                onUpdate(bookmarks)
            }
    }

    /** Fetch user profile once — result delivered via callback. */
    fun getUserProfile(onResult: (Map<String, Any>?) -> Unit) {
        val userId = uid ?: run { onResult(null); return }
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { onResult(it.data) }
            .addOnFailureListener { onResult(null) }
    }
}
