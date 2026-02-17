package com.example.rootsharemobile.ui.fragment.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.rootsharemobile.R
import com.example.rootsharemobile.data.remote.ApiConfig
import com.example.rootsharemobile.databinding.FragmentProfileBinding
import com.example.rootsharemobile.ui.viewmodel.AuthViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * Profile screen Fragment.
 *
 * Displays the current user's profile data (sourced from Room via [AuthViewModel])
 * and allows changing the profile picture via camera or gallery.
 *
 * MVVM: the Fragment only reads from LiveData and calls ViewModel methods —
 * no direct database or network access.
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    // URI used when taking a photo with the camera
    private var cameraImageUri: Uri? = null

    // ── Activity Result launchers ────────────────────────────────────────────

    /** Opens the gallery for image selection. */
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { handleSelectedImage(it) }
    }

    /** Opens the camera to capture a new photo. */
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { handleSelectedImage(it) }
        }
    }

    /** Requests camera permission before opening the camera. */
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera() else showSnackbar("Camera permission denied.")
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()
        setupClickListeners()
    }

    // ── LiveData observers ────────────────────────────────────────────────────

    private fun observeViewModel() {
        // User data comes from the Room database via AuthViewModel
        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.textUsername.text = user.username
                binding.textEmail.text = user.email

                // Determine which image URL to display (local takes priority)
                val imageUrl = when {
                    !user.localProfileImageUrl.isNullOrBlank() -> user.localProfileImageUrl
                    !user.profileImageUrl.isNullOrBlank() ->
                        ApiConfig.resolveImageUrl(user.profileImageUrl)
                    else -> null
                }

                if (imageUrl != null) {
                    Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.color.gray_200)
                        .error(R.color.gray_200)
                        .circleCrop()
                        .into(binding.imageProfilePicture)
                } else {
                    binding.imageProfilePicture.setImageResource(R.color.gray_200)
                }
            }
        }

        // Upload state
        authViewModel.uploadState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.UploadState.Idle -> {
                    binding.progressUpload.visibility = View.GONE
                    binding.btnChangePhoto.isEnabled = true
                    binding.textUploadStatus.visibility = View.GONE
                }
                is AuthViewModel.UploadState.Uploading -> {
                    binding.progressUpload.visibility = View.VISIBLE
                    binding.btnChangePhoto.isEnabled = false
                    binding.textUploadStatus.text = getString(R.string.uploading_photo)
                    binding.textUploadStatus.visibility = View.VISIBLE
                }
                is AuthViewModel.UploadState.Success -> {
                    binding.progressUpload.visibility = View.GONE
                    binding.btnChangePhoto.isEnabled = true
                    binding.textUploadStatus.text = getString(R.string.msg_photo_uploaded)
                    binding.textUploadStatus.visibility = View.VISIBLE
                    authViewModel.resetUploadState()
                }
                is AuthViewModel.UploadState.Error -> {
                    binding.progressUpload.visibility = View.GONE
                    binding.btnChangePhoto.isEnabled = true
                    binding.textUploadStatus.visibility = View.GONE
                    showSnackbar(state.message)
                    authViewModel.resetUploadState()
                }
            }
        }
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private fun setupClickListeners() {
        binding.btnChangePhoto.setOnClickListener {
            showPhotoPickerDialog()
        }

        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            // Navigate to login and clear the back stack
            findNavController().navigate(R.id.action_profile_to_login)
        }
    }

    // ── Photo picker ─────────────────────────────────────────────────────────

    private fun showPhotoPickerDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_photo_title))
            .setItems(
                arrayOf(
                    getString(R.string.dialog_photo_camera),
                    getString(R.string.dialog_photo_gallery)
                )
            ) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen()
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .setNegativeButton(getString(R.string.dialog_photo_cancel), null)
            .show()
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> openCamera()

            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val imageFile = File(
            requireContext().cacheDir,
            "profile_capture_${System.currentTimeMillis()}.jpg"
        )
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            imageFile
        )
        cameraLauncher.launch(cameraImageUri)
    }

    // ── Image upload ─────────────────────────────────────────────────────────

    /**
     * Convert the selected [Uri] to a [MultipartBody.Part] and pass it to
     * the ViewModel for upload. The Fragment does not perform the upload itself.
     */
    private fun handleSelectedImage(uri: Uri) {
        val context = requireContext()
        val inputStream = context.contentResolver.openInputStream(uri) ?: return
        val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
        tempFile.outputStream().use { output -> inputStream.copyTo(output) }

        val requestBody = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", tempFile.name, requestBody)

        authViewModel.uploadProfileImage(imagePart)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
