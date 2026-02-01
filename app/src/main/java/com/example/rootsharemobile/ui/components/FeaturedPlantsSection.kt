package com.example.rootsharemobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rootsharemobile.data.model.Plant
import com.example.rootsharemobile.data.model.PlantStatus
import com.example.rootsharemobile.ui.theme.Gray500
import com.example.rootsharemobile.ui.theme.Gray900
import com.example.rootsharemobile.ui.theme.RootShareMobileTheme

@Composable
fun FeaturedPlantsSection(
    plants: List<Plant>,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onPlantClick: ((Plant) -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Featured Plants",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Gray900,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            plants.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No plants yet. Add your first plant!",
                        fontSize = 16.sp,
                        color = Gray500
                    )
                }
            }
            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    items(plants, key = { it.id }) { plant ->
                        PlantCard(
                            plant = plant,
                            onClick = { onPlantClick?.invoke(plant) }
                        )
                    }
                }
            }
        }
    }
}

// Sample data for previews
val sampleFeaturedPlants = listOf(
    Plant(
        id = "1",
        name = "My Beautiful Monstera",
        species = "Monstera Deliciosa",
        status = PlantStatus.ACTIVE,
        imageUrl = ""
    ),
    Plant(
        id = "2",
        name = "Boston Fern",
        species = "Nephrolepis exaltata",
        status = PlantStatus.ACTIVE,
        imageUrl = ""
    ),
    Plant(
        id = "3",
        name = "Snake Plant",
        species = "Sansevieria",
        status = PlantStatus.GIFTED,
        imageUrl = ""
    )
)

@Preview(showBackground = true)
@Composable
fun FeaturedPlantsSectionPreview() {
    RootShareMobileTheme {
        FeaturedPlantsSection(plants = sampleFeaturedPlants)
    }
}

@Preview(showBackground = true)
@Composable
fun FeaturedPlantsSectionLoadingPreview() {
    RootShareMobileTheme {
        FeaturedPlantsSection(plants = emptyList(), isLoading = true)
    }
}

@Preview(showBackground = true)
@Composable
fun FeaturedPlantsSectionEmptyPreview() {
    RootShareMobileTheme {
        FeaturedPlantsSection(plants = emptyList(), isLoading = false)
    }
}
