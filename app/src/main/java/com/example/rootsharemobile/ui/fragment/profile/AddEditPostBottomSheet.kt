package com.example.rootsharemobile.ui.fragment.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.rootsharemobile.R
import com.example.rootsharemobile.data.local.db.entity.PlantEntity
import com.example.rootsharemobile.data.model.PostType
import com.example.rootsharemobile.databinding.BottomSheetAddEditPostBinding
import com.example.rootsharemobile.ui.viewmodel.AuthViewModel
import com.example.rootsharemobile.ui.viewmodel.MyPostsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class AddEditPostBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddEditPostBinding? = null
    private val binding get() = _binding!!

    private val postsViewModel: MyPostsViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    private val postId: String? get() = arguments?.getString(EXTRA_POST_ID)
    private val isEditMode: Boolean get() = postId != null

    private var submitted = false

    /** Maps dropdown display position → PlantEntity (index 0 is "None"). */
    private var plantOptions: List<PlantEntity?> = emptyList()

    companion object {
        private const val EXTRA_POST_ID   = "postId"
        private const val EXTRA_CONTENT   = "content"
        private const val EXTRA_POST_TYPE = "postType"
        private const val EXTRA_PLANT_ID  = "plantId"
        private const val EXTRA_PLANT_NAME = "plantName"
        private const val EXTRA_IMAGES    = "images"

        fun newAddInstance() = AddEditPostBottomSheet()

        fun newEditInstance(
            postId: String,
            content: String,
            postType: String,
            plantId: String?,
            plantName: String?,
            imagesJson: String
        ) = AddEditPostBottomSheet().apply {
            arguments = Bundle().apply {
                putString(EXTRA_POST_ID, postId)
                putString(EXTRA_CONTENT, content)
                putString(EXTRA_POST_TYPE, postType)
                putString(EXTRA_PLANT_ID, plantId)
                putString(EXTRA_PLANT_NAME, plantName)
                putString(EXTRA_IMAGES, imagesJson)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddEditPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPostTypeDropdown()
        setupPlantDropdown()
        setupUi()
        observeViewModel()
    }

    // ── UI setup ──────────────────────────────────────────────────────────────

    private fun setupUi() {
        if (isEditMode) {
            binding.textSheetTitle.setText(R.string.title_edit_post)
            binding.btnSubmit.setText(R.string.btn_save_changes)

            binding.editContent.setText(arguments?.getString(EXTRA_CONTENT))

            // Restore post type
            val savedType = arguments?.getString(EXTRA_POST_TYPE)?.uppercase() ?: "UPDATE"
            val typeLabel = postTypeLabel(savedType)
            binding.editPostType.setText(typeLabel, false)

            // Restore linked plant
            val savedPlantName = arguments?.getString(EXTRA_PLANT_NAME)
            if (!savedPlantName.isNullOrBlank()) {
                binding.editPlant.setText(savedPlantName, false)
            }

            // Restore images (comma-separated → newline-separated)
            val images = arguments?.getString(EXTRA_IMAGES) ?: ""
            if (images.isNotBlank()) {
                binding.editImages.setText(images.split(",").joinToString("\n") { it.trim() })
            }
        }

        binding.btnSubmit.setOnClickListener { submit() }
    }

    private fun setupPostTypeDropdown() {
        val types = listOf("Update", "Swap", "Giveaway")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types)
        binding.editPostType.setAdapter(adapter)
        if (!isEditMode) {
            binding.editPostType.setText("Update", false)
        }
    }

    private fun setupPlantDropdown() {
        postsViewModel.userPlants.observe(viewLifecycleOwner) { plants ->
            plantOptions = listOf(null) + plants
            val names = listOf(getString(R.string.hint_no_plant)) + plants.map { it.name }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
            binding.editPlant.setAdapter(adapter)

            // In edit mode, pre-select the linked plant
            if (isEditMode) {
                val savedPlantId = arguments?.getString(EXTRA_PLANT_ID)
                if (savedPlantId != null) {
                    val idx = plants.indexOfFirst { it.id == savedPlantId }
                    if (idx >= 0) {
                        binding.editPlant.setText(plants[idx].name, false)
                    }
                }
            }
        }
    }

    // ── ViewModel observer ────────────────────────────────────────────────────

    private fun observeViewModel() {
        postsViewModel.resetOperationState()
        postsViewModel.operationState.observe(viewLifecycleOwner) { state ->
            val loading = state is MyPostsViewModel.OperationState.Loading
            binding.progressSubmit.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnSubmit.isEnabled = !loading

            if (state is MyPostsViewModel.OperationState.Error) {
                binding.textError.text = state.message
                binding.textError.visibility = View.VISIBLE
            }

            if (state is MyPostsViewModel.OperationState.Success && submitted) {
                submitted = false
                dismiss()
            }
        }
    }

    // ── Submit logic ──────────────────────────────────────────────────────────

    private fun submit() {
        val content = binding.editContent.text?.toString()?.trim() ?: ""
        if (content.isBlank()) {
            binding.layoutContent.error = getString(R.string.error_field_required)
            return
        }
        binding.layoutContent.error = null
        binding.textError.visibility = View.GONE
        submitted = true

        val type = parsePostType(binding.editPostType.text?.toString() ?: "Update")

        // Resolve selected plant
        val plantText = binding.editPlant.text?.toString() ?: ""
        val selectedPlant = plantOptions.firstOrNull { it?.name == plantText }
        val plantId = selectedPlant?.id

        // Parse images (newline-separated URLs)
        val imagesRaw = binding.editImages.text?.toString() ?: ""
        val images = imagesRaw.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        lifecycleScope.launch {
            val token = authViewModel.getAccessToken()
            if (token == null) {
                binding.textError.text = getString(R.string.error_not_authenticated)
                binding.textError.visibility = View.VISIBLE
                submitted = false
                return@launch
            }
            if (isEditMode) {
                postsViewModel.updatePost(token, postId!!, content, type, plantId, images)
            } else {
                postsViewModel.createPost(token, content, type, plantId, images)
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parsePostType(label: String): PostType = when (label.uppercase()) {
        "SWAP" -> PostType.SWAP
        "GIVEAWAY" -> PostType.GIVEAWAY
        else -> PostType.UPDATE
    }

    private fun postTypeLabel(typeStr: String): String = when (typeStr.uppercase()) {
        "SWAP" -> "Swap"
        "GIVEAWAY" -> "Giveaway"
        else -> "Update"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
