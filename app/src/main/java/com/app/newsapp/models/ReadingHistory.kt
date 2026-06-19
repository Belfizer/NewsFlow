package com.app.newsapp.models

/**
 * ReadingHistory — local data model representing a row in the reading_history SQLite table.
 *
 * Foreign key: articleId → saved_articles.article_id
 */
data class ReadingHistory(
    val id: Int,
    val articleId: String,          // matches saved_articles.article_id
    val readDate: String,           // ISO date-time string
    val readDurationSeconds: Int    // 0 if user just viewed without timing
)
