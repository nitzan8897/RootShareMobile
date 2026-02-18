package com.example.rootsharemobile.ui.fragment.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.rootsharemobile.R
import com.example.rootsharemobile.data.local.db.entity.PostEntity
import com.example.rootsharemobile.data.remote.ApiConfig
import com.example.rootsharemobile.databinding.FragmentProfileBinding
import com.example.rootsharemobile.ui.adapter.GridCardAdapter
import com.example.rootsharemobile.ui.adapter.toGridCardItem
import com.example.rootsharemobile.ui.viewmodel.AuthViewModel
import com.example.rootsharemobile.ui.viewmodel.MyPostsViewModel
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
    private val postsViewModel: MyPostsViewModel by activityViewModels()

    private var cameraImageUri: Uri? = null
    private var isEditMode = false

    private lateinit var postsGridAdapter: GridCardAdapter
    private var postsList: List<PostEntity> = emptyList()

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
        setupPostsGrid()
        observeViewModel()
        setupClickListeners()
        fetchPosts()
    }

    // ── Posts grid setup ─────────────────────────────────────────────────────

    private fun setupPostsGrid() {
        postsGridAdapter = GridCardAdapter { item ->
            val post = postsList.find { it.id == item.id } ?: return@GridCardAdapter
            showPostActionsDialog(post)
        }
        binding.recyclerMyPosts.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = postsGridAdapter
        }
    }

    private fun fetchPosts() {
        lifecycleScope.launch {
            val token = authViewModel.getAccessToken() ?: return@launch
            postsViewModel.loadPosts(token)
        }
    }

    private fun showPostActionsDialog(post: PostEntity) {
        val options = arrayOf(
            getString(R.string.dialog_edit_post_title),
            getString(R.string.dialog_delete_post_title)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_post_actions_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditPostDialog(post)
                    1 -> showDeletePostDialog(post)
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showEditPostDialog(post: PostEntity) {
        val editText = EditText(requireContext()).apply {
            setText(post.content)
            setPadding(64, 32, 64, 32)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_edit_post_title)
            .setView(editText)
            .setPositiveButton(R.string.btn_save_changes) { _, _ ->
                val newContent = editText.text.toString().trim()
                if (newContent.isNotBlank() && newContent != post.content) {
                    lifecycleScope.launch {
                        val token = authViewModel.getAccessToken() ?: return@launch
                        postsViewModel.updatePost(token, post.id, newContent)
                    }
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showDeletePostDialog(post: PostEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_post_title)
            .setMessage(R.string.dialog_delete_post_message)
            .setPositiveButton(R.string.btn_confirm_delete) { _, _ ->
                lifecycleScope.launch {
                    val token = authViewModel.getAccessToken() ?: return@launch
                    postsViewModel.deletePost(token, post.id)
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
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
                val memberSince = user.createdAt.take(7).replace("-", "/")
                binding.textStatMemberSince.text = memberSince

                val daysSinceCreation = try {
                    val created = java.time.LocalDate.parse(user.createdAt.take(10))
                    java.time.temporal.ChronoUnit.DAYS.between(created, java.time.LocalDate.now())
                } catch (_: Exception) { 0L }
                binding.textStatStreakEmoji.text = when {
                    daysSinceCreation >= 365 -> "\uD83C\uDFC6"
                    daysSinceCreation >= 90  -> "\u2B50"
                    daysSinceCreation >= 30  -> "\uD83D\uDD25"
                    else                     -> "\uD83C\uDF31"
                }
            }
        }

        authViewModel.plantCount.observe(viewLifecycleOwner) { count ->
            binding.textStatPlantsCount.text = count.toString()
            binding.textStatPlantsEmoji.text = when {
                count >= 20 -> "\uD83C\uDF33"
                count >= 10 -> "\uD83C\uDF3F"
                count >= 5  -> "\uD83C\uDF3E"
                count >= 1  -> "\uD83C\uDF3B"
                else        -> "\uD83C\uDF31"
            }
        }

        authViewModel.postCount.observe(viewLifecycleOwner) { count ->
            binding.textStatPostsCount.text = count.toString()
            binding.textStatPostsEmoji.text = when {
                count >= 20 -> "\uD83D\uDCE3"
                count >= 10 -> "\uD83D\uDCAC"
                count >= 5  -> "\u270D\uFE0F"
                count >= 1  -> "\uD83D\uDCDD"
                else        -> "\uD83D\uDE36"
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

        postsViewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            postsList = posts
            postsGridAdapter.submitList(posts.map { it.toGridCardItem() })
            val hasPosts = posts.isNotEmpty()
            binding.recyclerMyPosts.visibility   = if (hasPosts) View.VISIBLE else View.GONE
            binding.layoutPostsEmpty.visibility  = if (hasPosts) View.GONE    else View.VISIBLE
        }

        postsViewModel.snackMessage.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrBlank()) {
                showSnackbar(msg)
                postsViewModel.clearSnackMessage()
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
