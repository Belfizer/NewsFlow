package com.app.newsapp.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * NewsResponse — top-level JSON response from NewsAPI.
 */
data class NewsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("totalResults") val totalResults: Int,
    @SerializedName("articles") val articles: List<NewsArticle>
)

/**
 * NewsArticle — a single article from the API.
 * Implements Serializable so it can be passed via Bundle to ArticleDetailFragment.
 */
data class NewsArticle(
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("urlToImage") val urlToImage: String?,
    @SerializedName("publishedAt") val publishedAt: String?,
    @SerializedName("source") val source: NewsSource?
) : Serializable

/**
 * NewsSource — the source nested inside NewsArticle.
 * Serializable for Bundle passing.
 */
data class NewsSource(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?
) : Serializable

