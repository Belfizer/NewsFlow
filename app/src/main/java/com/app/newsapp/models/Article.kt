package com.app.newsapp.models

import java.io.Serializable

/**
 * Core data model for a news article.
 * Implements Serializable so it can be passed via Bundle between Fragments.
 */
data class Article(
    val id: Int,
    val title: String,
    val source: String,
    val category: String,
    val description: String,
    val timeAgo: String,
    val readTime: String,
    val tags: List<String>
) : Serializable
