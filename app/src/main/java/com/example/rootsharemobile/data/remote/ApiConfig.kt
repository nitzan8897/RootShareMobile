package com.example.rootsharemobile.data.remote

import com.example.rootsharemobile.BuildConfig

object ApiConfig {
    val BASE_URL: String = BuildConfig.API_BASE_URL

    // Socket.io connects to the server root (no /api/ path)
    const val SOCKET_URL = "http://10.0.2.2:3000"

    // Timeout configurations (in seconds)
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}
