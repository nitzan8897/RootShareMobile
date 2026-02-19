package com.example.rootsharemobile

import com.example.rootsharemobile.data.local.db.entity.PostEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostEntityTest {

    private fun makePost(
        postType: String = "UPDATE",
        content: String = "Hello"
    ) = PostEntity(
        id = "1",
        userId = "u1",
        plantId = null,
        plantName = null,
        plantSpecies = null,
        postType = postType,
        content = content,
        imagesJson = "",
        likesCount = 0,
        commentsCount = 0,
        createdAt = "2026-01-01",
        updatedAt = "2026-01-01"
    )

    @Test
    fun typeBadge_update() {
        assertEquals("Update", makePost(postType = "UPDATE").typeBadge)
    }

    @Test
    fun typeBadge_swap() {
        assertEquals("Swap", makePost(postType = "SWAP").typeBadge)
    }

    @Test
    fun typeBadge_giveaway() {
        assertEquals("Giveaway", makePost(postType = "GIVEAWAY").typeBadge)
    }

    @Test
    fun typeBadge_caseInsensitive() {
        assertEquals("Update", makePost(postType = "update").typeBadge)
    }

    @Test
    fun tags_extractsHashtags() {
        val post = makePost(content = "My #plant is #growing well")
        assertEquals(listOf("#plant", "#growing"), post.tags)
    }

    @Test
    fun tags_emptyWhenNoHashtags() {
        val post = makePost(content = "No hashtags here")
        assertTrue(post.tags.isEmpty())
    }

    @Test
    fun tags_emptyForBlankContent() {
        val post = makePost(content = "")
        assertTrue(post.tags.isEmpty())
    }
}
