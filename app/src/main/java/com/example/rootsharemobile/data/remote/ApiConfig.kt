package com.example.rootsharemobile.data.remote

/**
 * API configuration constants.
 * Change BASE_URL to your backend server address.
 */
object ApiConfig {
    // For emulator: use 10.0.2.2 to access host machine's localhost
    // For physical device: use your computer's local IP address (e.g., 192.168.1.x)
    // For production: use your deployed server URL

    const val BASE_URL = "http://10.0.2.2:3000/api/"

    // Timeout configurations (in seconds)
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
    const val GOOGLE_WEB_CLIENT_ID = "754188443642-3n0b2jvnh1lu0mprfnqa2q5i5gj68ro7.apps.googleusercontent.com"
}
