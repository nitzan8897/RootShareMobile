package com.example.rootsharemobile

import com.example.rootsharemobile.data.local.db.entity.PlantEntity
import com.example.rootsharemobile.data.local.db.entity.PlantWithPostCount
import com.example.rootsharemobile.ui.adapter.toGridCardItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantGridCardItemMappingTest {

    private fun makePlant(
        id: String = "p1",
        name: String = "Monstera",
        species: String = "Deliciosa",
        status: String = "ACTIVE",
        imageUrl: String = "http://img.jpg",
        postCount: Int = 3
    ) = PlantWithPostCount(
        plant = PlantEntity(
            id = id,
            userId = "u1",
            name = name,
            species = species,
            status = status,
            imageUrl = imageUrl,
            isFeatured = false,
            createdAt = "2026-01-01",
            updatedAt = "2026-01-01"
        ),
        postCount = postCount
    )

    @Test
    fun usesPlantIdAsId() {
        val item = makePlant(id = "plant42").toGridCardItem()
        assertEquals("plant42", item.id)
    }

    @Test
    fun usesPlantImageUrl() {
        val item = makePlant(imageUrl = "http://plant.jpg").toGridCardItem()
        assertEquals("http://plant.jpg", item.imageUrl)
    }

    @Test
    fun mapsDisplayTitle() {
        val item = makePlant(name = "Short Name").toGridCardItem()
        assertEquals("Short Name", item.title)
    }

    @Test
    fun mapsDisplayCategory() {
        val item = makePlant(species = "Rose").toGridCardItem()
        assertEquals("Rose Plant", item.subtitle)
    }

    @Test
    fun activeStatusBadge() {
        val item = makePlant(status = "ACTIVE").toGridCardItem()
        assertEquals("Active", item.badgeText)
        assertEquals(R.color.badge_active_bg, item.badgeBgColor)
        assertEquals(R.color.badge_active_text, item.badgeTextColor)
    }

    @Test
    fun deadStatusBadge() {
        val item = makePlant(status = "DEAD").toGridCardItem()
        assertEquals("Deceased", item.badgeText)
        assertEquals(R.color.badge_dead_bg, item.badgeBgColor)
        assertEquals(R.color.badge_dead_text, item.badgeTextColor)
    }

    @Test
    fun giftedStatusBadge() {
        val item = makePlant(status = "GIFTED").toGridCardItem()
        assertEquals("Gifted", item.badgeText)
        assertEquals(R.color.badge_gifted_bg, item.badgeBgColor)
        assertEquals(R.color.badge_gifted_text, item.badgeTextColor)
    }

    @Test
    fun showsPostCountAsCounter() {
        val item = makePlant(postCount = 7).toGridCardItem()
        assertEquals("7", item.counterText)
    }

    @Test
    fun counterIconIsVisible() {
        val item = makePlant().toGridCardItem()
        assertTrue(item.counterIconVisible)
    }
}
