package com.example.nplayer.data.repository

import java.io.IOException
import retrofit2.HttpException

/** Minimal, exception-safe result type used across the data layer. */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: AppError) : ApiResult<Nothing>
}

enum class AppError {
    NETWORK,
    UNAUTHORIZED,
    SERVER,
    UNKNOWN,
}

/** Runs [block], mapping thrown exceptions onto [ApiResult]. */
suspend fun <T> apiCall(block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (e: HttpException) {
    ApiResult.Failure(
        when (e.code()) {
            401, 403 -> AppError.UNAUTHORIZED
            in 500..599 -> AppError.SERVER
            else -> AppError.UNKNOWN
        },
    )
} catch (e: IOException) {
    ApiResult.Failure(AppError.NETWORK)
} catch (e: Exception) {
    ApiResult.Failure(AppError.UNKNOWN)
}
