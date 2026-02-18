package com.example.rootsharemobile.ui.fragment.garden

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.rootsharemobile.R
import com.example.rootsharemobile.data.local.db.entity.PlantWithPostCount
import com.example.rootsharemobile.data.remote.ApiConfig
import com.example.rootsharemobile.databinding.FragmentPlantDetailsBinding
import com.example.rootsharemobile.ui.viewmodel.AuthViewModel
import com.example.rootsharemobile.ui.viewmodel.MyGardenViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Shows the full details of a single plant from the user's garden.
 *
 * Receives the plant ID via SafeArgs, observes the plant LiveData from Room,
 * and delegates edit/delete actions to [MyGardenViewModel].
 */
class PlantDetailsFragment : Fragment() {

    private var _binding: FragmentPlantDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: PlantDetailsFragmentArgs by navArgs()
    private val gardenViewModel: MyGardenViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    private var currentItem: PlantWithPostCount? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlantDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        observePlant()
        observeActions()
    }

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    private fun setupToolbar() {
        binding.toolbar.setNavigationIcon(R.drawable.ic_nav_home)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.toolbar.title = getString(R.string.title_plant_details)

        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_edit) {
                currentItem?.let { openEditSheet(it) }
                true
            } else false
        }
    }

    // -------------------------------------------------------------------------
    // Observers
    // -------------------------------------------------------------------------

    private fun observePlant() {
        gardenViewModel.observePlant(args.plantId).observe(viewLifecycleOwner) { item ->
            if (item == null) {
                // Plant was deleted — navigate back
                findNavController().navigateUp()
                return@observe
            }
            currentItem = item
            bindPlant(item)
        }
    }

    private fun observeActions() {
        gardenViewModel.operationState.observe(viewLifecycleOwner) { state ->
            val loading = state is MyGardenViewModel.OperationState.Loading
            binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnDelete.isEnabled = !loading
        }

        gardenViewModel.snackMessage.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrBlank()) {
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                gardenViewModel.clearSnackMessage()
            }
        }
    }

    // -------------------------------------------------------------------------
    // UI
    // -------------------------------------------------------------------------

    private fun bindPlant(item: PlantWithPostCount) {
        val plant = item.plant

        binding.textPlantName.text = plant.name
        binding.textSpecies.text   = plant.displayCategory

        // Status badge
        binding.textStatusBadge.text = plant.badge
        val (bgColor, textColor) = when (plant.status.uppercase()) {
            "ACTIVE"  -> R.color.badge_active_bg  to R.color.badge_active_text
            "DEAD"    -> R.color.badge_dead_bg    to R.color.badge_dead_text
            else      -> R.color.badge_gifted_bg  to R.color.badge_gifted_text
        }
        binding.textStatusBadge.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), bgColor)
        binding.textStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), textColor))

        // Post count
        binding.textPostCount.text = if (item.postCount > 0) {
            getString(R.string.label_post_count, item.postCount)
        } else {
            getString(R.string.label_no_posts)
        }

        // Added date
        binding.textAddedOn.text = formatDate(plant.createdAt)

        // Hero image
        val imageUrl = ApiConfig.resolveImageUrl(plant.imageUrl)
        Glide.with(binding.imagePlant.context)
            .load(imageUrl)
            .placeholder(R.color.gray_100)
            .error(R.color.gray_200)
            .centerCrop()
            .into(binding.imagePlant)

        // Delete
        binding.btnDelete.setOnClickListener { confirmDelete(item) }
    }

    private fun openEditSheet(item: PlantWithPostCount) {
        AddEditPlantBottomSheet.newEditInstance(item.plant)
            .show(parentFragmentManager, "edit_plant")
    }

    private fun confirmDelete(item: PlantWithPostCount) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, item.plant.name))
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_confirm_delete) { _, _ ->
                lifecycleScope.launch {
                    val token = authViewModel.getAccessToken() ?: return@launch
                    gardenViewModel.deletePlant(token, item.plant.id)
                    // Navigation is handled by observePlant() — when the plant is
                    // deleted from Room, the observer receives null and navigates back.
                }
            }
            .show()
    }

    private fun formatDate(isoDate: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val date = sdf.parse(isoDate)
            val out  = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            getString(R.string.label_added_on, out.format(date!!))
        } catch (e: Exception) {
            ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
