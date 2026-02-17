package com.example.rootsharemobile.ui.fragment.garden

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.rootsharemobile.R
import com.example.rootsharemobile.data.local.db.entity.PlantEntity
import com.example.rootsharemobile.data.model.PlantStatus
import com.example.rootsharemobile.databinding.BottomSheetAddEditPlantBinding
import com.example.rootsharemobile.ui.viewmodel.AuthViewModel
import com.example.rootsharemobile.ui.viewmodel.MyGardenViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

/**
 * BottomSheetDialogFragment for creating a new plant or editing an existing one.
 *
 * Mode is determined by the presence of [EXTRA_PLANT_ID] in arguments:
 *  - No ID  → Add mode (single-shot creation)
 *  - With ID → Edit mode (pre-filled fields + status selector)
 *
 * Communicates with [MyGardenViewModel] scoped to the Activity so that
 * [MyGardenFragment] observes Room LiveData updates automatically.
 */
class AddEditPlantBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddEditPlantBinding? = null
    private val binding get() = _binding!!

    private val gardenViewModel: MyGardenViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    private val plantId: String? get() = arguments?.getString(EXTRA_PLANT_ID)
    private val isEditMode: Boolean get() = plantId != null

    companion object {
        private const val EXTRA_PLANT_ID  = "plantId"
        private const val EXTRA_NAME      = "name"
        private const val EXTRA_SPECIES   = "species"
        private const val EXTRA_IMAGE_URL = "imageUrl"
        private const val EXTRA_STATUS    = "status"

        fun newAddInstance() = AddEditPlantBottomSheet()

        fun newEditInstance(plant: PlantEntity) = AddEditPlantBottomSheet().apply {
            arguments = Bundle().apply {
                putString(EXTRA_PLANT_ID,  plant.id)
                putString(EXTRA_NAME,      plant.name)
                putString(EXTRA_SPECIES,   plant.species)
                putString(EXTRA_IMAGE_URL, plant.imageUrl)
                putString(EXTRA_STATUS,    plant.status)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddEditPlantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
        observeViewModel()
    }

    private fun setupUi() {
        if (isEditMode) {
            binding.textSheetTitle.setText(R.string.title_edit_plant)
            binding.btnSubmit.setText(R.string.btn_save_changes)

            // Pre-fill fields
            binding.editName.setText(arguments?.getString(EXTRA_NAME))
            binding.editSpecies.setText(arguments?.getString(EXTRA_SPECIES))
            binding.editImageUrl.setText(arguments?.getString(EXTRA_IMAGE_URL))

            // Show status selector
            binding.labelStatus.visibility = View.VISIBLE
            binding.radioStatus.visibility = View.VISIBLE
            when (arguments?.getString(EXTRA_STATUS)?.uppercase()) {
                "DEAD"   -> binding.radioDead.isChecked = true
                "GIFTED" -> binding.radioGifted.isChecked = true
                else     -> binding.radioActive.isChecked = true
            }
        }

        binding.btnSubmit.setOnClickListener { submit() }
    }

    private fun observeViewModel() {
        gardenViewModel.uiState.observe(viewLifecycleOwner) { state ->
            val loading = state is MyGardenViewModel.GardenUiState.Loading
            binding.progressSubmit.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnSubmit.isEnabled = !loading

            if (state is MyGardenViewModel.GardenUiState.Error) {
                binding.textError.text = state.message
                binding.textError.visibility = View.VISIBLE
            }

            if (state is MyGardenViewModel.GardenUiState.Success) {
                dismiss()
            }
        }
    }

    private fun submit() {
        val name      = binding.editName.text?.toString()?.trim() ?: ""
        val species   = binding.editSpecies.text?.toString()?.trim() ?: ""
        val imageUrl  = binding.editImageUrl.text?.toString()?.trim() ?: ""

        if (name.isBlank()) {
            binding.layoutName.error = getString(R.string.error_field_required)
            return
        }
        binding.layoutName.error = null

        if (species.isBlank()) {
            binding.layoutSpecies.error = getString(R.string.error_field_required)
            return
        }
        binding.layoutSpecies.error = null
        binding.textError.visibility = View.GONE

        lifecycleScope.launch {
            val token = authViewModel.getAccessToken() ?: return@launch
            if (isEditMode) {
                val status = when {
                    binding.radioDead.isChecked   -> PlantStatus.DEAD
                    binding.radioGifted.isChecked -> PlantStatus.GIFTED
                    else                          -> PlantStatus.ACTIVE
                }
                gardenViewModel.updatePlant(token, plantId!!, name, species, imageUrl, status)
            } else {
                gardenViewModel.createPlant(token, name, species, imageUrl)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
