package com.example.rootsharemobile.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.rootsharemobile.data.model.Plant
import com.example.rootsharemobile.data.model.PlantStatus
import com.example.rootsharemobile.ui.theme.Emerald500
import com.example.rootsharemobile.ui.theme.Gray100
import com.example.rootsharemobile.ui.theme.Gray200
import com.example.rootsharemobile.ui.theme.Gray500
import com.example.rootsharemobile.ui.theme.Gray900
import com.example.rootsharemobile.ui.theme.RootShareMobileTheme

@Composable
fun PlantCard(
    plant: Plant,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.width(280.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Gray100
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = { onClick?.invoke() }
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
        ) {
            // Status Badge
            Text(
                text = plant.badge,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = when (plant.status) {
                    PlantStatus.ACTIVE -> Emerald500
                    PlantStatus.DEAD -> Gray500
                    PlantStatus.GIFTED -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Plant Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (plant.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = plant.imageUrl,
                        contentDescription = plant.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder when no image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = plant.name.take(1).uppercase(),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gray500
                        )
                    }
                }
            }

            // Bottom info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = plant.displayCategory,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Gray500,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = plant.displayTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Gray900
                    )
                }
            }
        }
    }
}

// Sample data for previews
val samplePlant = Plant(
    id = "1",
    name = "My Beautiful Monstera",
    species = "Monstera Deliciosa",
    status = PlantStatus.ACTIVE,
    imageUrl = ""
)

@Preview(showBackground = true)
@Composable
fun PlantCardPreview() {
    RootShareMobileTheme {
        PlantCard(plant = samplePlant)
    }
}
