package com.example.rootsharemobile.data.remote

import com.example.rootsharemobile.data.local.TokenManager
import com.example.rootsharemobile.data.model.Plant
import com.example.rootsharemobile.data.model.Post
import com.example.rootsharemobile.data.model.PostAuthor
import com.example.rootsharemobile.data.model.PostType
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit client for API calls.
 * Must call [init] with a [TokenManager] before using [apiService].
 */
object RetrofitClient {

    private var tokenManager: TokenManager? = null

    fun init(tokenManager: TokenManager) {
        this.tokenManager = tokenManager
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .apply {
                tokenManager?.let { authenticator(TokenRefreshAuthenticator(it)) }
            }
            .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    /**
     * The backend's POST /posts and PATCH /posts/:id endpoints return the raw Mongoose document
     * without calling .populate(), so `userId` and `plantId` arrive as bare ObjectId strings
     * instead of nested objects. This deserializer handles both shapes so Gson never throws
     * JsonSyntaxException on those endpoints.
     */
    private val postDeserializer = object : JsonDeserializer<Post> {
        override fun deserialize(json: JsonElement, typeOfT: Type, ctx: JsonDeserializationContext): Post {
            val obj = json.asJsonObject

            val author: PostAuthor? = obj["userId"]?.takeIf { !it.isJsonNull }?.let {
                if (it.isJsonObject) ctx.deserialize(it, PostAuthor::class.java)
                else PostAuthor(id = it.asString)
            }

            val plant: Plant? = obj["plantId"]?.takeIf { !it.isJsonNull }?.let {
                if (it.isJsonObject) ctx.deserialize(it, Plant::class.java)
                else Plant(id = it.asString)
            }

            val type = obj["type"]?.takeIf { !it.isJsonNull }?.let {
                runCatching { ctx.deserialize<PostType>(it, PostType::class.java) }.getOrNull()
            } ?: PostType.UPDATE

            val images = obj["images"]?.takeIf { it.isJsonArray }
                ?.asJsonArray?.mapNotNull { it?.asString } ?: emptyList()

            return Post(
                id           = obj["_id"]?.asString ?: "",
                author       = author,
                plant        = plant,
                type         = type,
                content      = obj["content"]?.asString ?: "",
                images       = images,
                likesCount   = obj["likesCount"]?.asInt ?: 0,
                commentsCount = obj["commentsCount"]?.asInt ?: 0,
                createdAt    = obj["createdAt"]?.asString ?: "",
                updatedAt    = obj["updatedAt"]?.asString ?: ""
            )
        }
    }

    private val gson = GsonBuilder()
        .registerTypeAdapter(Post::class.java, postDeserializer)
        .create()

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val apiService: ApiService by lazy { retrofit.create(ApiService::class.java) }
}