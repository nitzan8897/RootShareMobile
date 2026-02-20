package com.example.rootsharemobile.data.remote

import com.example.rootsharemobile.BuildConfig

object ApiConfig {
    val BASE_URL: String = BuildConfig.API_BASE_URL

    // Socket.io connects to the server root (no /api/ path)
    const val SOCKET_URL = "http://10.0.2.2:3000"

    /**
     * Server root URL (without /api/ suffix).
     * Used for resolving static file paths like /uploads/profile-images/...
     */
    val SERVER_URL: String
        get() = BASE_URL.removeSuffix("api/").removeSuffix("api")

    // Timeout configurations (in seconds)
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L

    /**
     * Resolves a profile image URL to a full URL.
     * - If it's already an absolute URL (http/https), returns as-is (e.g. Google CDN).
     * - If it's a relative path (e.g. /uploads/...), prepends the server root.
     */
    fun resolveImageUrl(url: String): String {
        return if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "${SERVER_URL.trimEnd('/')}${url}"
        }
    }
}
