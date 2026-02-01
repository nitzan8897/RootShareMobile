package com.example.rootsharemobile.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.rootsharemobile.ui.theme.Emerald500
import com.example.rootsharemobile.ui.theme.Gray500
import com.example.rootsharemobile.ui.theme.RootShareMobileTheme

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: @Composable () -> ImageVector,
    val unselectedIcon: @Composable () -> ImageVector
) {
    data object Home : BottomNavItem(
        route = "home",
        label = "Home",
        selectedIcon = { Icons.Filled.Home },
        unselectedIcon = { Icons.Outlined.Home }
    )

    data object MyGarden : BottomNavItem(
        route = "my_garden",
        label = "My Garden",
        selectedIcon = { Icons.Filled.Person }, // Placeholder - will use custom plant icon
        unselectedIcon = { Icons.Outlined.Person }
    )

    data object Community : BottomNavItem(
        route = "community",
        label = "Community",
        selectedIcon = { Icons.Filled.Person }, // Placeholder - will use custom users icon
        unselectedIcon = { Icons.Outlined.Person }
    )

    data object Gallery : BottomNavItem(
        route = "gallery",
        label = "Gallery",
        selectedIcon = { Icons.Filled.Person }, // Placeholder - will use custom photo icon
        unselectedIcon = { Icons.Outlined.Person }
    )
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.MyGarden,
    BottomNavItem.Community,
    BottomNavItem.Gallery
)

@Composable
fun RootShareBottomNav(
    selectedRoute: String,
    onItemSelected: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = item.route == selectedRoute

            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemSelected(item) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon() else item.unselectedIcon(),
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(text = item.label)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Emerald500,
                    selectedTextColor = Emerald500,
                    unselectedIconColor = Gray500,
                    unselectedTextColor = Gray500,
                    indicatorColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RootShareBottomNavPreview() {
    RootShareMobileTheme {
        RootShareBottomNav(
            selectedRoute = "home",
            onItemSelected = {}
        )
    }
}
