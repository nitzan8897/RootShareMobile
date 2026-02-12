package com.example.rootsharemobile.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rootsharemobile.data.model.AuthProvider
import com.example.rootsharemobile.data.model.User
import com.example.rootsharemobile.data.model.UserRole
import com.example.rootsharemobile.ui.theme.Emerald500
import com.example.rootsharemobile.ui.theme.Gray500
import com.example.rootsharemobile.ui.theme.Gray900
import com.example.rootsharemobile.ui.theme.RootShareMobileTheme

@Composable
fun ProfileScreen(
    user: User?,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = "Profile",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Gray900
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Profile avatar
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Emerald500.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "Profile",
                modifier = Modifier.size(48.dp),
                tint = Emerald500
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // User info
        if (user != null) {
            Text(
                text = user.username,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Gray900
            )

            Text(
                text = user.email,
                fontSize = 14.sp,
                color = Gray500,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // User details card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileDetailRow(label = "Username", value = user.username)
                    ProfileDetailRow(label = "Email", value = user.email)
                    ProfileDetailRow(label = "Role", value = user.role.name)
                    ProfileDetailRow(
                        label = "Auth Provider",
                        value = user.authProvider.name
                    )
                }
            }
        } else {
            Text(
                text = "Not logged in",
                fontSize = 16.sp,
                color = Gray500
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Logout button
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Logout",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileDetailRow(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Gray500
        )
        Text(
            text = value,
            fontSize = 16.sp,
            color = Gray900,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    RootShareMobileTheme {
        ProfileScreen(
            user = User(
                id = "1",
                email = "john@example.com",
                username = "johndoe",
                profileImageUrl = null,
                role = UserRole.USER,
                authProvider = AuthProvider.LOCAL,
                createdAt = "2024-01-01",
                updatedAt = "2024-01-01"
            ),
            onLogout = {}
        )
    }
}
