package com.app.newsapp.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.app.newsapp.compose.ReadingStatsScreen

/**
 * ReadingStatsActivity — Jetpack Compose host for the Reading Stats screen.
 *
 * Assignment 5 F4:
 * - Receives stats from ProfileFragment via Intent extras
 * - Renders the Compose ReadingStatsScreen
 */
class ReadingStatsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val articlesRead = intent.getIntExtra("ARTICLES_READ", 0)
        val savedCount   = intent.getIntExtra("SAVED_COUNT", 0)
        val topCategory  = intent.getStringExtra("TOP_CATEGORY") ?: "Technology"
        val userName     = intent.getStringExtra("USER_NAME") ?: "User"

        setContent {
            ReadingStatsScreen(
                userName     = userName,
                articlesRead = articlesRead,
                savedCount   = savedCount,
                topCategory  = topCategory,
                onBack       = { finish() }
            )
        }
    }
}
