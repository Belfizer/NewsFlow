package com.app.newsapp.repository

import com.app.newsapp.models.NewsArticle
import com.app.newsapp.network.RetrofitClient

/**
 * NewsRepository — the single source of truth for all live news data.
 *
 * F1 REQUIREMENT:
 * - All API calls are wrapped in try/catch to prevent crashes
 * - Returns sealed Result<T> for clean error handling in fragments
 * - Filters out articles with null or "[Removed]" titles
 *
 * All functions are `suspend` — must be called from Dispatchers.IO coroutine scope.
 */
class NewsRepository {

    /**
     * F1: Fetches US top headlines.
     * Filters null/[Removed] titles before returning.
     */
    suspend fun getTopHeadlines(): Result<List<NewsArticle>> {
        return try {
            val response = RetrofitClient.instance.getTopHeadlines()
            val filtered = response.articles.filter { article ->
                !article.title.isNullOrBlank() && article.title != "[Removed]"
            }
            Result.Success(filtered)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * F1: Fetches headlines by category (technology, sports, business, etc.).
     * Category is lowercased to match NewsAPI expected values.
     */
    suspend fun getByCategory(category: String): Result<List<NewsArticle>> {
        return try {
            val response = RetrofitClient.instance.getByCategory(
                category = category.lowercase()
            )
            val filtered = response.articles.filter { !it.title.isNullOrBlank() }
            Result.Success(filtered)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * F1 + F5: Searches all indexed articles by keyword query.
     * Called from HomeFragment when search text length >= 3.
     */
    suspend fun searchArticles(query: String): Result<List<NewsArticle>> {
        return try {
            val response = RetrofitClient.instance.searchArticles(query)
            val filtered = response.articles.filter { !it.title.isNullOrBlank() }
            Result.Success(filtered)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }
}
