package com.example.rootsharemobile.ui.fragment.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.rootsharemobile.R
import com.example.rootsharemobile.data.remote.ApiConfig
import com.example.rootsharemobile.databinding.FragmentProfileBinding
import com.example.rootsharemobile.ui.viewmodel.AuthViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    private var cameraImageUri: Uri? = null
    private var isEditMode = false

    // ── Activity Result launchers ────────────────────────────────────────────

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { handleSelectedImage(it) } }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) cameraImageUri?.let { handleSelectedImage(it) }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera() else showSnackbar("Camera permission denied.")
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authViewModel.fetchCurrentUser()
        observeViewModel()
        setupClickListeners()
    }

    // ── LiveData observers ──────────────────────────────────────────────────

    private fun observeViewModel() {
        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.textUsername.text = user.username
                binding.textEmail.text = user.email

                val imageUrl = when {
                    !user.localProfileImageUrl.isNullOrBlank() ->
                        ApiConfig.resolveImageUrl(user.localProfileImageUrl)
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
                // Member-since from createdAt (YYYY-MM-DD...)
                val memberSince = user.createdAt.take(7).replace("-", "/") // e.g. "2025/01"
                binding.textStatMemberSince.text = memberSince

                // Streak emoji: days since account creation
                val daysSinceCreation = try {
                    val created = java.time.LocalDate.parse(user.createdAt.take(10))
                    java.time.temporal.ChronoUnit.DAYS.between(created, java.time.LocalDate.now())
                } catch (_: Exception) { 0L }
                binding.textStatStreakEmoji.text = when {
                    daysSinceCreation >= 365 -> "\uD83C\uDFC6" // trophy
                    daysSinceCreation >= 90  -> "\u2B50"        // star
                    daysSinceCreation >= 30  -> "\uD83D\uDD25"  // fire
                    else                     -> "\uD83C\uDF31"  // seedling
                }
            }
        }

        // Dashboard: plant count with progression emoji
        authViewModel.plantCount.observe(viewLifecycleOwner) { count ->
            binding.textStatPlantsCount.text = count.toString()
            binding.textStatPlantsEmoji.text = when {
                count >= 20 -> "\uD83C\uDF33" // deciduous tree (pro)
                count >= 10 -> "\uD83C\uDF3F" // herb
                count >= 5  -> "\uD83C\uDF3E" // rice
                count >= 1  -> "\uD83C\uDF3B" // sunflower
                else        -> "\uD83C\uDF31" // seedling (beginner)
            }
        }

        // Dashboard: post count with social emoji
        authViewModel.postCount.observe(viewLifecycleOwner) { count ->
            binding.textStatPostsCount.text = count.toString()
            binding.textStatPostsEmoji.text = when {
                count >= 20 -> "\uD83D\uDCE3" // megaphone (influencer)
                count >= 10 -> "\uD83D\uDCAC" // speech balloon
                count >= 5  -> "\u270D\uFE0F"  // writing hand
                count >= 1  -> "\uD83D\uDCDD" // memo
                else        -> "\uD83D\uDE36" // face without mouth (lurker)
            }
        }

        authViewModel.uploadState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.UploadState.Idle -> {
                    binding.progressUpload.visibility = View.GONE
                    binding.btnEditPhoto.isEnabled = true
                    binding.textUploadStatus.visibility = View.GONE
                }
                is AuthViewModel.UploadState.Uploading -> {
                    binding.progressUpload.visibility = View.VISIBLE
                    binding.btnEditPhoto.isEnabled = false
                    binding.textUploadStatus.text = getString(R.string.uploading_photo)
                    binding.textUploadStatus.visibility = View.VISIBLE
                }
                is AuthViewModel.UploadState.Success -> {
                    binding.progressUpload.visibility = View.GONE
                    binding.btnEditPhoto.isEnabled = true
                    binding.textUploadStatus.text = getString(R.string.msg_photo_uploaded)
                    binding.textUploadStatus.visibility = View.VISIBLE
                    authViewModel.resetUploadState()
                }
                is AuthViewModel.UploadState.Error -> {
                    binding.progressUpload.visibility = View.GONE
                    binding.btnEditPhoto.isEnabled = true
                    binding.textUploadStatus.visibility = View.GONE
                    showSnackbar(state.message)
                    authViewModel.resetUploadState()
                }
            }
        }

        authViewModel.profileUpdateState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.ProfileUpdateState.Idle -> { /* no-op */ }
                is AuthViewModel.ProfileUpdateState.Saving -> {
                    binding.btnEditProfile.isEnabled = false
                }
                is AuthViewModel.ProfileUpdateState.Success -> {
                    binding.btnEditProfile.isEnabled = true
                    exitEditMode()
                    showSnackbar(getString(R.string.msg_profile_updated))
                    authViewModel.resetProfileUpdateState()
                }
                is AuthViewModel.ProfileUpdateState.Error -> {
                    binding.btnEditProfile.isEnabled = true
                    showSnackbar(state.message)
                    authViewModel.resetProfileUpdateState()
                }
            }
        }
    }

    // ── Click listeners ─────────────────────────────────────────────────────

    private fun setupClickListeners() {
        binding.btnEditPhoto.setOnClickListener { showPhotoPickerDialog() }

        binding.btnLogout.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                authViewModel.logoutSuspend()
                findNavController().navigate(R.id.action_profile_to_login)
            }
        }

        binding.btnEditProfile.setOnClickListener {
            if (isEditMode) saveProfile() else enterEditMode()
        }
    }

    // ── Edit mode ───────────────────────────────────────────────────────────

    private fun enterEditMode() {
        isEditMode = true
        val currentUsername = authViewModel.currentUser.value?.username ?: ""

        binding.textUsername.visibility = View.GONE
        binding.inputLayoutUsername.visibility = View.VISIBLE
        binding.editUsername.setText(currentUsername)

        binding.btnEditProfile.text = getString(R.string.btn_save_profile)
        binding.btnEditProfile.setIconResource(R.drawable.ic_check)
    }

    private fun exitEditMode() {
        isEditMode = false
        binding.textUsername.visibility = View.VISIBLE
        binding.inputLayoutUsername.visibility = View.GONE

        binding.btnEditProfile.text = getString(R.string.btn_edit_profile)
        binding.btnEditProfile.setIconResource(R.drawable.ic_edit_pencil)
    }

    private fun saveProfile() {
        val newUsername = binding.editUsername.text.toString().trim()
        if (newUsername.isBlank() || newUsername.length < 3) {
            binding.inputLayoutUsername.error = "Username must be at least 3 characters"
            return
        }
        binding.inputLayoutUsername.error = null
        authViewModel.updateProfile(newUsername)
    }

    // ── Photo picker ────────────────────────────────────────────────────────

    private fun showPhotoPickerDialog() {
        val hasCustomPhoto = !authViewModel.currentUser.value?.localProfileImageUrl.isNullOrBlank()
        val options = if (hasCustomPhoto) {
            arrayOf(
                getString(R.string.dialog_photo_camera),
                getString(R.string.dialog_photo_gallery),
                getString(R.string.dialog_photo_remove)
            )
        } else {
            arrayOf(
                getString(R.string.dialog_photo_camera),
                getString(R.string.dialog_photo_gallery)
            )
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_photo_title))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen()
                    1 -> galleryLauncher.launch("image/*")
                    2 -> authViewModel.removeProfileImage()
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

    // ── Image upload ────────────────────────────────────────────────────────

    private fun handleSelectedImage(uri: Uri) {
        val context = requireContext()
        val inputStream = context.contentResolver.openInputStream(uri) ?: return
        val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
        tempFile.outputStream().use { output -> inputStream.copyTo(output) }

        val requestBody = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", tempFile.name, requestBody)
        authViewModel.uploadProfileImage(imagePart)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
