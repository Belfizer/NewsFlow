package com.app.newsapp.repository

/**
 * Result — sealed class wrapping all API and DB call outcomes.
 *
 * Usage:
 *   when (result) {
 *       is Result.Success -> { result.data }
 *       is Result.Error   -> { result.message }
 *       is Result.Loading -> { show spinner }
 *   }
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
