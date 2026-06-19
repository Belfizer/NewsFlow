package com.app.newsapp.network

import com.app.newsapp.models.NewsResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * NewsApiService — Retrofit interface defining all NewsAPI endpoints.
 *
 * F1 REQUIREMENT:
 * - getTopHeadlines()  → /v2/top-headlines?country=us
 * - getByCategory()    → /v2/top-headlines?country=us&category=...
 * - searchArticles()   → /v2/everything?q=...
 *
 * All functions are `suspend` — must be called from a coroutine (Dispatchers.IO).
 */
interface NewsApiService {

    /**
     * Fetches top headlines for a given country.
     * Default country = "us".
     */
    @GET("top-headlines")
    suspend fun getTopHeadlines(
        @Query("country") country: String = "us",
        @Query("apiKey") apiKey: String = RetrofitClient.API_KEY
    ): NewsResponse

    /**
     * Fetches headlines filtered by category.
     * Valid categories: business, entertainment, general, health, science, sports, technology
     */
    @GET("top-headlines")
    suspend fun getByCategory(
        @Query("country") country: String = "us",
        @Query("category") category: String,
        @Query("apiKey") apiKey: String = RetrofitClient.API_KEY
    ): NewsResponse

    /**
     * Full-text search across all indexed articles.
     * Used for real-time search in HomeFragment when query >= 3 chars.
     */
    @GET("everything")
    suspend fun searchArticles(
        @Query("q") query: String,
        @Query("apiKey") apiKey: String = RetrofitClient.API_KEY
    ): NewsResponse
}
