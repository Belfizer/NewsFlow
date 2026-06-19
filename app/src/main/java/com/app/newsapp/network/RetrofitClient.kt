package com.app.newsapp.network

import com.app.newsapp.network.NewsApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * RetrofitClient — singleton that creates and caches the Retrofit instance.
 *
 * F1 REQUIREMENT:
 * - BASE_URL: https://newsapi.org/v2/
 * - OkHttp LoggingInterceptor set to BODY level for debug inspection
 * - GsonConverterFactory for automatic JSON → data class mapping
 * - Lazily initialized on first access
 */
object RetrofitClient {

    private const val BASE_URL = "https://newsapi.org/v2/"
    const val API_KEY = "" // add your own api key from newsapi.org

    val instance: NewsApiService by lazy {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NewsApiService::class.java)
    }
}
