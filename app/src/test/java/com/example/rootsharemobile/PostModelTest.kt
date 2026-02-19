package com.example.rootsharemobile

import com.example.rootsharemobile.data.model.CreatePostRequest
import com.example.rootsharemobile.data.model.PostType
import com.example.rootsharemobile.data.model.UpdatePostRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PostModelTest {

    @Test
    fun createPostRequest_withAllFields() {
        val request = CreatePostRequest(
            plantId = "plant1",
            type = PostType.SWAP,
            content = "Trading my Monstera",
            images = listOf("http://img1.jpg", "http://img2.jpg")
        )
        assertEquals("plant1", request.plantId)
        assertEquals(PostType.SWAP, request.type)
        assertEquals("Trading my Monstera", request.content)
        assertEquals(2, request.images?.size)
    }

    @Test
    fun createPostRequest_withoutOptionalFields() {
        val request = CreatePostRequest(
            type = PostType.UPDATE,
            content = "Simple update"
        )
        assertNull(request.plantId)
        assertNull(request.images)
    }

    @Test
    fun updatePostRequest_partialUpdate() {
        val request = UpdatePostRequest(content = "Updated content")
        assertEquals("Updated content", request.content)
        assertNull(request.type)
        assertNull(request.plantId)
        assertNull(request.images)
    }

    @Test
    fun updatePostRequest_fullUpdate() {
        val request = UpdatePostRequest(
            plantId = "p2",
            type = PostType.GIVEAWAY,
            content = "Now giving away",
            images = listOf("http://new.jpg")
        )
        assertEquals("p2", request.plantId)
        assertEquals(PostType.GIVEAWAY, request.type)
        assertEquals("Now giving away", request.content)
        assertEquals(listOf("http://new.jpg"), request.images)
    }

    @Test
    fun postType_enumValues() {
        assertEquals(3, PostType.entries.size)
        assertEquals(PostType.UPDATE, PostType.valueOf("UPDATE"))
        assertEquals(PostType.SWAP, PostType.valueOf("SWAP"))
        assertEquals(PostType.GIVEAWAY, PostType.valueOf("GIVEAWAY"))
    }
}
