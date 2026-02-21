package com.example.rootsharemobile.data.remote

import com.google.gson.annotations.SerializedName
import com.example.rootsharemobile.data.model.AuthResponse
import com.example.rootsharemobile.data.model.ChatMessageResponse
import com.example.rootsharemobile.data.model.ChatResponse
import com.example.rootsharemobile.data.model.CreateChatRequest
import com.example.rootsharemobile.data.model.CreateGroupChatRequest
import com.example.rootsharemobile.data.model.CreatePlantRequest
import com.example.rootsharemobile.data.model.MakeAdminRequest
import com.example.rootsharemobile.data.model.RemoveMemberRequest
import com.example.rootsharemobile.data.model.RenameGroupRequest
import com.example.rootsharemobile.data.model.Comment
import com.example.rootsharemobile.data.model.CreateCommentRequest
import com.example.rootsharemobile.data.model.CreatePostRequest
import com.example.rootsharemobile.data.model.GoogleTokenRequest
import com.example.rootsharemobile.data.model.LoginRequest
import com.example.rootsharemobile.data.model.Plant
import com.example.rootsharemobile.data.model.PlantStatus
import com.example.rootsharemobile.data.model.Post
import com.example.rootsharemobile.data.model.Species
import com.example.rootsharemobile.data.model.RefreshTokenResponse
import com.example.rootsharemobile.data.model.RegisterRequest
import com.example.rootsharemobile.data.model.UpdatePlantRequest
import com.example.rootsharemobile.data.model.UpdatePostRequest
import com.example.rootsharemobile.data.model.User
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API service interface for RootShare backend.
 * Auth, Plants, and Posts endpoints.
 */
interface ApiService {

    // ==================== AUTH ====================

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Header("Authorization") refreshToken: String
    ): Response<RefreshTokenResponse>

    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<Unit>

    @GET("auth/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): Response<User>

    @POST("auth/google/token")
    suspend fun googleAuth(
        @Body request: GoogleTokenRequest
    ): Response<AuthResponse>

    // ==================== USERS ====================

    @retrofit2.http.PUT("users/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Response<User>

    @Multipart
    @POST("users/profile/image")
    suspend fun uploadProfileImage(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part
    ): Response<User>

    // ==================== SPECIES ====================

    @GET("species")
    suspend fun getSpecies(
        @Header("Authorization") token: String
    ): Response<List<Species>>

    // ==================== PLANTS ====================

    @GET("plants")
    suspend fun getPlants(
        @Header("Authorization") token: String,
        @Query("status") status: PlantStatus? = null
    ): Response<List<Plant>>

    @GET("plants/featured")
    suspend fun getFeaturedPlants(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int? = null
    ): Response<List<Plant>>

    @GET("plants/{id}")
    suspend fun getPlantById(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Plant>

    @POST("plants")
    suspend fun createPlant(
        @Header("Authorization") token: String,
        @Body request: CreatePlantRequest
    ): Response<Plant>

    @PATCH("plants/{id}")
    suspend fun updatePlant(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body request: UpdatePlantRequest
    ): Response<Plant>

    @DELETE("plants/{id}")
    suspend fun deletePlant(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<DeleteResponse>

    // ==================== POSTS ====================

    @GET("posts")
    suspend fun getPosts(
        @Header("Authorization") token: String
    ): Response<List<Post>>

    @GET("posts/{id}")
    suspend fun getPostById(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Post>

    @POST("posts")
    suspend fun createPost(
        @Header("Authorization") token: String,
        @Body request: CreatePostRequest
    ): Response<Post>

    @PATCH("posts/{id}")
    suspend fun updatePost(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body request: UpdatePostRequest
    ): Response<Post>

    @DELETE("posts/{id}")
    suspend fun deletePost(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<DeleteResponse>

    // ==================== COMMENTS ====================

    @GET("comments/post/{postId}")
    suspend fun getComments(
        @Header("Authorization") token: String,
        @Path("postId") postId: String
    ): Response<List<Comment>>

    @POST("comments")
    suspend fun createComment(
        @Header("Authorization") token: String,
        @Body request: CreateCommentRequest
    ): Response<Comment>

    // ==================== LIKES ====================

    @POST("likes/posts/{postId}/toggle")
    suspend fun togglePostLike(
        @Header("Authorization") token: String,
        @Path("postId") postId: String
    ): Response<ToggleLikeResponse>

    @GET("likes/posts/{postId}/is-liked")
    suspend fun isPostLiked(
        @Header("Authorization") token: String,
        @Path("postId") postId: String
    ): Response<IsLikedResponse>

    // ==================== USERS ====================

    @GET("users/search")
    suspend fun searchUsers(
        @Header("Authorization") token: String,
        @Query("query") query: String
    ): Response<List<User>>

    // ==================== CHATS ====================

    @GET("chats")
    suspend fun getChats(
        @Header("Authorization") token: String
    ): Response<List<ChatResponse>>

    @POST("chats")
    suspend fun createOrGetChat(
        @Header("Authorization") token: String,
        @Body request: CreateChatRequest
    ): Response<ChatResponse>

    @POST("chats/group")
    suspend fun createGroupChat(
        @Header("Authorization") token: String,
        @Body request: CreateGroupChatRequest
    ): Response<ChatResponse>

    @GET("chats/{chatId}/messages")
    suspend fun getChatMessages(
        @Header("Authorization") token: String,
        @Path("chatId") chatId: String,
        @Query("limit") limit: Int = 30,
        @Query("page") page: Int = 1
    ): Response<List<ChatMessageResponse>>

    @POST("chats/{chatId}/read")
    suspend fun markChatAsRead(
        @Header("Authorization") token: String,
        @Path("chatId") chatId: String
    ): Response<Unit>

    @POST("chats/{chatId}/leave")
    suspend fun leaveGroupChat(
        @Header("Authorization") token: String,
        @Path("chatId") chatId: String
    ): Response<Unit>

    @PATCH("chats/{chatId}/name")
    suspend fun renameGroupChat(
        @Header("Authorization") token: String,
        @Path("chatId") chatId: String,
        @Body request: RenameGroupRequest
    ): Response<ChatResponse>

    @HTTP(method = "DELETE", path = "chats/{chatId}/members", hasBody = true)
    suspend fun removeGroupMember(
        @Header("Authorization") token: String,
        @Path("chatId") chatId: String,
        @Body request: RemoveMemberRequest
    ): Response<Unit>

    @DELETE("chats/{chatId}")
    suspend fun deleteGroupChat(
        @Header("Authorization") token: String,
        @Path("chatId") chatId: String
    ): Response<Unit>

    @PATCH("chats/{chatId}/admin")
    suspend fun makeGroupAdmin(
        @Header("Authorization") token: String,
        @Path("chatId") chatId: String,
        @Body request: MakeAdminRequest
    ): Response<ChatResponse>
}

data class DeleteResponse(
    val deleted: Boolean,
    val id: String
)

data class ToggleLikeResponse(
    @SerializedName("liked")
    val liked: Boolean = false,
    @SerializedName("likesCount")
    val count: Int = 0
)

data class IsLikedResponse(
    val liked: Boolean = false
)
