package com.example.rootsharemobile.ui.adapter

import androidx.annotation.ColorRes
import com.example.rootsharemobile.R
import com.example.rootsharemobile.data.local.db.entity.PlantWithPostCount
import com.example.rootsharemobile.data.local.db.entity.PostEntity

data class GridCardItem(
    val id: String,
    val imageUrl: String?,
    val title: String,
    val subtitle: String,
    val badgeText: String,
    @ColorRes val badgeBgColor: Int,
    @ColorRes val badgeTextColor: Int,
    val counterText: String?,
    val counterIconVisible: Boolean = true
)

fun PlantWithPostCount.toGridCardItem(): GridCardItem {
    val (bgColor, textColor) = when (plant.status.uppercase()) {
        "ACTIVE" -> R.color.badge_active_bg to R.color.badge_active_text
        "DEAD"   -> R.color.badge_dead_bg to R.color.badge_dead_text
        else     -> R.color.badge_gifted_bg to R.color.badge_gifted_text
    }
    return GridCardItem(
        id = plant.id,
        imageUrl = plant.imageUrl,
        title = plant.displayTitle,
        subtitle = plant.displayCategory,
        badgeText = plant.badge,
        badgeBgColor = bgColor,
        badgeTextColor = textColor,
        counterText = postCount.toString(),
        counterIconVisible = true
    )
}

fun PostEntity.toGridCardItem(): GridCardItem {
    val (bgColor, textColor) = when (postType.uppercase()) {
        "SWAP"     -> R.color.badge_gifted_bg to R.color.badge_gifted_text
        "GIVEAWAY" -> R.color.badge_dead_bg to R.color.badge_dead_text
        else       -> R.color.badge_active_bg to R.color.badge_active_text
    }
    val firstImage = imagesJson.split(",").firstOrNull { it.isNotBlank() }
    val preview = if (content.length > 30) content.take(30) + "..." else content
    return GridCardItem(
        id = id,
        imageUrl = firstImage,
        title = plantName ?: typeBadge,
        subtitle = preview,
        badgeText = typeBadge,
        badgeBgColor = bgColor,
        badgeTextColor = textColor,
        counterText = "$likesCount",
        counterIconVisible = false
    )
}
