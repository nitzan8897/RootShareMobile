package com.example.rootsharemobile.data.remote

import okhttp3.ResponseBody
import org.json.JSONObject
import java.io.IOException
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
 * Extracts the "message" field from a NestJS error response body.
 * Returns null if the body cannot be parsed.
 */
fun parseErrorBody(errorBody: ResponseBody?): String? {
    return try {
        val json = JSONObject(errorBody?.string() ?: return null)
        val msg = json.opt("message")
        when (msg) {
            is String -> msg
            is org.json.JSONArray -> (0 until msg.length()).joinToString("; ") { msg.getString(it) }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Maps network exceptions to user-friendly messages.
 */
fun mapNetworkError(e: Exception): String {
    return when (e) {
        is UnknownHostException -> "No internet connection. Please check your network."
        is ConnectException    -> "Could not connect to server. Please try again later."
        is SocketTimeoutException -> "Connection timed out. Please try again."
        is IOException         -> "Network error. Please try again later."
        else                   -> "Unexpected error: ${e.javaClass.simpleName}"
    }
}
