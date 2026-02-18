package com.example.rootsharemobile

import com.example.rootsharemobile.data.local.db.entity.PostEntity
import com.example.rootsharemobile.ui.adapter.toGridCardItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class GridCardItemMappingTest {

    private fun makePost(
        id: String = "post1",
        postType: String = "UPDATE",
        content: String = "Hello world",
        imagesJson: String = "",
        plantName: String? = null,
        likesCount: Int = 5
    ) = PostEntity(
        id = id,
        userId = "user1",
        plantId = null,
        plantName = plantName,
        plantSpecies = null,
        postType = postType,
        content = content,
        imagesJson = imagesJson,
        likesCount = likesCount,
        commentsCount = 0,
        createdAt = "2026-01-01",
        updatedAt = "2026-01-01"
    )

    @Test
    fun postToGridCardItem_usesPostIdAsId() {
        val item = makePost(id = "abc123").toGridCardItem()
        assertEquals("abc123", item.id)
    }

    @Test
    fun postToGridCardItem_usesPlantNameAsTitleWhenPresent() {
        val item = makePost(plantName = "My Fern").toGridCardItem()
        assertEquals("My Fern", item.title)
    }

    @Test
    fun postToGridCardItem_usesTypeBadgeAsTitleWhenNoPlantName() {
        val item = makePost(plantName = null, postType = "SWAP").toGridCardItem()
        assertEquals("Swap", item.title)
    }

    @Test
    fun postToGridCardItem_truncatesLongContent() {
        val longContent = "A".repeat(50)
        val item = makePost(content = longContent).toGridCardItem()
        assertEquals("A".repeat(30) + "...", item.subtitle)
    }

    @Test
    fun postToGridCardItem_keepsShortContentAsIs() {
        val item = makePost(content = "Short").toGridCardItem()
        assertEquals("Short", item.subtitle)
    }

    @Test
    fun postToGridCardItem_extractsFirstImageUrl() {
        val item = makePost(imagesJson = "http://img1.jpg,http://img2.jpg").toGridCardItem()
        assertEquals("http://img1.jpg", item.imageUrl)
    }

    @Test
    fun postToGridCardItem_returnsNullImageForEmptyImagesJson() {
        val item = makePost(imagesJson = "").toGridCardItem()
        assertNull(item.imageUrl)
    }

    @Test
    fun postToGridCardItem_showsLikesCountAsCounter() {
        val item = makePost(likesCount = 42).toGridCardItem()
        assertEquals("42", item.counterText)
    }

    @Test
    fun postToGridCardItem_counterIconIsHidden() {
        val item = makePost().toGridCardItem()
        assertFalse(item.counterIconVisible)
    }

    @Test
    fun postToGridCardItem_updateTypeBadge() {
        val item = makePost(postType = "UPDATE").toGridCardItem()
        assertEquals("Update", item.badgeText)
        assertEquals(R.color.badge_active_bg, item.badgeBgColor)
        assertEquals(R.color.badge_active_text, item.badgeTextColor)
    }

    @Test
    fun postToGridCardItem_swapTypeBadge() {
        val item = makePost(postType = "SWAP").toGridCardItem()
        assertEquals("Swap", item.badgeText)
        assertEquals(R.color.badge_gifted_bg, item.badgeBgColor)
        assertEquals(R.color.badge_gifted_text, item.badgeTextColor)
    }

    @Test
    fun postToGridCardItem_giveawayTypeBadge() {
        val item = makePost(postType = "GIVEAWAY").toGridCardItem()
        assertEquals("Giveaway", item.badgeText)
        assertEquals(R.color.badge_dead_bg, item.badgeBgColor)
        assertEquals(R.color.badge_dead_text, item.badgeTextColor)
    }
}
