package com.app.newsapp.models

import java.io.Serializable

/**
 * SavedArticle — local data model representing a row in the saved_articles SQLite table.
 * Implements Serializable for Bundle-based fragment navigation.
 */
data class SavedArticle(
    val id: Int,
    val articleId: String,      // url used as unique ID
    val title: String,
    val sourceName: String,
    val description: String,
    val url: String,
    val publishedAt: String,
    val category: String,
    val savedDate: String       // ISO date string e.g. "2024-05-10"
) : Serializable
