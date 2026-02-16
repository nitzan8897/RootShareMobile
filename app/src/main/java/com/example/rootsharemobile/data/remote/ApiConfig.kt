package com.example.rootsharemobile.data.remote

import com.example.rootsharemobile.BuildConfig

object ApiConfig {
    val BASE_URL: String = BuildConfig.API_BASE_URL

    // Timeout configurations (in seconds)
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}
