package com.example.rootsharemobile.ui.fragment.garden

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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

class AddEditPlantBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddEditPlantBinding? = null
    private val binding get() = _binding!!

    private val gardenViewModel: MyGardenViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    private val plantId: String? get() = arguments?.getString(EXTRA_PLANT_ID)
    private val isEditMode: Boolean get() = plantId != null

    private var submitted = false

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
        setupSpeciesDropdown()
        observeViewModel()
    }

    private fun setupUi() {
        if (isEditMode) {
            binding.textSheetTitle.setText(R.string.title_edit_plant)
            binding.btnSubmit.setText(R.string.btn_save_changes)

            binding.editName.setText(arguments?.getString(EXTRA_NAME))
            binding.editSpecies.setText(arguments?.getString(EXTRA_SPECIES), false)
            binding.editImageUrl.setText(arguments?.getString(EXTRA_IMAGE_URL))

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

    private fun setupSpeciesDropdown() {
        gardenViewModel.speciesList.observe(viewLifecycleOwner) { species ->
            val names = species.map { it.name }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
            binding.editSpecies.setAdapter(adapter)
        }

        // Trigger fetch if not loaded yet
        lifecycleScope.launch {
            val token = authViewModel.getAccessToken() ?: return@launch
            gardenViewModel.fetchSpecies(token)
        }
    }

    private fun observeViewModel() {
        gardenViewModel.resetOperationState()
        gardenViewModel.operationState.observe(viewLifecycleOwner) { state ->
            val loading = state is MyGardenViewModel.OperationState.Loading
            binding.progressSubmit.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnSubmit.isEnabled = !loading

            if (state is MyGardenViewModel.OperationState.Error) {
                binding.textError.text = state.message
                binding.textError.visibility = View.VISIBLE
            }

            if (state is MyGardenViewModel.OperationState.Success && submitted) {
                submitted = false
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
        submitted = true

        val finalImageUrl = imageUrl.ifBlank { MyGardenViewModel.randomDefaultImage() }

        lifecycleScope.launch {
            val token = authViewModel.getAccessToken()
            if (token == null) {
                binding.textError.text = getString(R.string.error_not_authenticated)
                binding.textError.visibility = View.VISIBLE
                submitted = false
                return@launch
            }
            if (isEditMode) {
                val status = when {
                    binding.radioDead.isChecked   -> PlantStatus.DEAD
                    binding.radioGifted.isChecked -> PlantStatus.GIFTED
                    else                          -> PlantStatus.ACTIVE
                }
                gardenViewModel.updatePlant(token, plantId!!, name, species, finalImageUrl, status)
            } else {
                gardenViewModel.createPlant(token, name, species, finalImageUrl)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
