package com.example.rootsharemobile.ui.screens.profile

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.rootsharemobile.data.model.AuthProvider
import com.example.rootsharemobile.data.model.User
import com.example.rootsharemobile.data.model.UserRole
import com.example.rootsharemobile.data.remote.ApiConfig
import com.example.rootsharemobile.ui.screens.auth.AuthViewModel
import com.example.rootsharemobile.ui.screens.auth.UploadState
import com.example.rootsharemobile.ui.theme.Emerald500
import com.example.rootsharemobile.ui.theme.Gray500
import com.example.rootsharemobile.ui.theme.Gray900
import com.example.rootsharemobile.ui.theme.RootShareMobileTheme
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User?,
    onLogout: () -> Unit,
    authViewModel: AuthViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showImagePickerSheet by remember { mutableStateOf(false) }
    val uploadState = authViewModel?.uploadState?.collectAsState()

    // Camera URI for storing captured photo
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleImageSelected(context, it, authViewModel) }
    }

    // Camera capture launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            cameraImageUri?.let { handleImageSelected(context, it, authViewModel) }
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createCameraImageUri(context)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle upload success/error feedback
    LaunchedEffect(uploadState?.value) {
        when (val state = uploadState?.value) {
            is UploadState.Success -> {
                Toast.makeText(context, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                authViewModel?.resetUploadState()
            }
            is UploadState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                authViewModel?.resetUploadState()
            }
            else -> {}
        }
    }

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

        // Profile avatar with edit overlay
        Box(
            contentAlignment = Alignment.Center
        ) {
            ProfileAvatar(user = user)

            // Edit badge
            if (authViewModel != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Emerald500)
                        .clickable { showImagePickerSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (uploadState?.value is UploadState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Change profile picture",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // User info
        if (user != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = user.username,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray900
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Copy username",
                    modifier = Modifier
                        .size(18.dp)
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("username", user.username))
                            Toast.makeText(context, "Username copied", Toast.LENGTH_SHORT).show()
                        },
                    tint = Gray500
                )
            }

            Text(
                text = user.email,
                fontSize = 14.sp,
                color = Gray500,
                modifier = Modifier.padding(top = 4.dp)
            )
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

    // Bottom sheet for choosing image source
    if (showImagePickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImagePickerSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Change Profile Picture",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray900
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Gallery option
                Button(
                    onClick = {
                        showImagePickerSheet = false
                        galleryLauncher.launch("image/*")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald500
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Choose from Gallery",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Camera option
                Button(
                    onClick = {
                        showImagePickerSheet = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald500
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Take a Photo",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ProfileAvatar(user: User?) {
    val imageUrl = user?.let { getProfileImageUrl(it) }

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(Emerald500.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "Profile",
                modifier = Modifier.size(48.dp),
                tint = Emerald500
            )
        }
    }
}

/**
 * Resolves the best available profile image URL for the user.
 * Prefers localProfileImageUrl (user-uploaded), falls back to profileImageUrl (Google CDN).
 */
private fun getProfileImageUrl(user: User): String? {
    val url = user.localProfileImageUrl ?: user.profileImageUrl ?: return null
    return ApiConfig.resolveImageUrl(url)
}

/**
 * Handles a selected image URI (from gallery or camera) and uploads it.
 */
private fun handleImageSelected(context: Context, uri: Uri, authViewModel: AuthViewModel?) {
    authViewModel ?: return

    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return
        val bytes = inputStream.readBytes()
        inputStream.close()

        val contentType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val requestBody = bytes.toRequestBody(contentType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData(
            "image",
            "profile.${contentType.substringAfter("/")}",
            requestBody
        )

        authViewModel.uploadProfileImage(part)
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to process image", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Creates a temporary file URI for camera capture via FileProvider.
 */
private fun createCameraImageUri(context: Context): Uri {
    val file = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
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
