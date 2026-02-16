package com.example.rootsharemobile.data.remote

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Maps HTTP error codes to user-friendly messages.
 */
fun mapHttpError(code: Int, resource: String): String {
    return when (code) {
        401 -> "Session expired. Please log in again."
        403 -> "You don't have permission to access this $resource."
        404 -> "The requested $resource was not found."
        500 -> "Server error. Please try again later."
        503 -> "Server is temporarily unavailable. Please try again later."
        else -> "Something went wrong (error $code). Please try again."
    }
}

/**
 * Maps network exceptions to user-friendly messages.
 */
fun mapNetworkError(e: Exception): String {
    return when (e) {
        is UnknownHostException -> "No internet connection. Please check your network."
        is ConnectException -> "Could not connect to server. Please try again later."
        is SocketTimeoutException -> "Connection timed out. Please try again."
        else -> "Network error. Please check your connection and try again."
    }
}
