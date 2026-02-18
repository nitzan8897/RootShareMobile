package com.example.rootsharemobile.ui.fragment.shared

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.rootsharemobile.R
import com.example.rootsharemobile.databinding.FragmentMyGardenBinding
import com.example.rootsharemobile.ui.adapter.GardenPlantAdapter
import com.example.rootsharemobile.ui.fragment.garden.AddEditPlantBottomSheet
import com.example.rootsharemobile.ui.viewmodel.AuthViewModel
import com.example.rootsharemobile.ui.viewmodel.MyGardenViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * My Garden screen — shows the user's full plant collection in a 2-column grid.
 *
 * Data flow (offline-first / Room as SSOT):
 *   Fragment → ViewModel action → Repository (API + Room) → Room LiveData → UI
 *
 * The Fragment never talks to the network or Room directly.
 */
class MyGardenFragment : Fragment() {

    private var _binding: FragmentMyGardenBinding? = null
    private val binding get() = _binding!!

    private val gardenViewModel: MyGardenViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    private lateinit var gardenAdapter: GardenPlantAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyGardenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        setupSwipeRefresh()
        setupFab()
        fetchInitialData()
    }

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    private fun setupRecyclerView() {
        gardenAdapter = GardenPlantAdapter { item ->
            val action = MyGardenFragmentDirections
                .actionGardenToPlantDetails(item.plant.id)
            findNavController().navigate(action)
        }

        binding.recyclerGarden.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = gardenAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.emerald_500)
        binding.swipeRefresh.setOnRefreshListener {
            lifecycleScope.launch {
                val token = authViewModel.getAccessToken() ?: run {
                    binding.swipeRefresh.isRefreshing = false
                    return@launch
                }
                gardenViewModel.refresh(token)
            }
        }

        binding.btnRetry.setOnClickListener { fetchInitialData() }
    }

    private fun setupFab() {
        binding.fabAddPlant.setOnClickListener {
            AddEditPlantBottomSheet.newAddInstance()
                .show(parentFragmentManager, "add_plant")
        }
    }

    // -------------------------------------------------------------------------
    // Observers
    // -------------------------------------------------------------------------

    private fun observeViewModel() {
        gardenViewModel.gardenPlants.observe(viewLifecycleOwner) { plants ->
            gardenAdapter.submitList(plants)
            val hasPlants = plants.isNotEmpty()
            binding.recyclerGarden.visibility = if (hasPlants) View.VISIBLE else View.GONE
            binding.layoutEmpty.visibility    = if (hasPlants) View.GONE    else View.VISIBLE
        }

        gardenViewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MyGardenViewModel.GardenUiState.Loading -> {
                    binding.progress.visibility    = View.VISIBLE
                    binding.layoutError.visibility = View.GONE
                }
                is MyGardenViewModel.GardenUiState.Success -> {
                    binding.progress.visibility       = View.GONE
                    binding.layoutError.visibility    = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                }
                is MyGardenViewModel.GardenUiState.Error -> {
                    binding.progress.visibility       = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    binding.textError.text            = state.message
                    binding.layoutError.visibility    = View.VISIBLE
                }
                else -> binding.progress.visibility = View.GONE
            }
        }

        gardenViewModel.snackMessage.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrBlank()) {
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                gardenViewModel.clearSnackMessage()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Data loading
    // -------------------------------------------------------------------------

    private fun fetchInitialData() {
        lifecycleScope.launch {
            val token = authViewModel.getAccessToken() ?: return@launch
            gardenViewModel.loadPlants(token)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
