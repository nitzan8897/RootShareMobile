package com.example.rootsharemobile.ui.fragment.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rootsharemobile.R
import com.example.rootsharemobile.databinding.FragmentHomeBinding
import com.example.rootsharemobile.ui.adapter.FeaturedPlantsAdapter
import com.example.rootsharemobile.ui.adapter.FeedPostAdapter
import com.example.rootsharemobile.ui.fragment.profile.AddEditPostBottomSheet
import com.example.rootsharemobile.ui.viewmodel.AuthViewModel
import com.example.rootsharemobile.ui.viewmodel.HomeViewModel
import com.example.rootsharemobile.ui.viewmodel.MyPostsViewModel
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Home screen Fragment.
 *
 * Displays:
 *  - A personalised welcome banner (username received via SafeArgs).
 *  - A horizontal RecyclerView of featured plants (from Room via HomeViewModel).
 *  - A vertical RecyclerView of community feed posts (from Room via HomeViewModel).
 *  - A FAB to create a new post (launches AddEditPostBottomSheet).
 *
 * The Fragment ONLY observes LiveData — it never calls the API or Room directly.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // SafeArgs — username passed from LoginFragment
    private val args: HomeFragmentArgs by navArgs()

    private val homeViewModel: HomeViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val myPostsViewModel: MyPostsViewModel by activityViewModels()

    private lateinit var plantsAdapter: FeaturedPlantsAdapter
    private lateinit var feedAdapter: FeedPostAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWelcomeBanner()
        setupRecyclerViews()
        observeViewModel()
        setupSwipeToRefresh()
        setupFab()
        fetchInitialData()
    }

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    /** Show a personalised greeting using the username from SafeArgs. */
    private fun setupWelcomeBanner() {
        binding.textWelcome.text = getString(R.string.home_greeting, args.username)
    }

    private fun setupRecyclerViews() {
        // Horizontal featured plants list
        plantsAdapter = FeaturedPlantsAdapter()
        binding.recyclerFeaturedPlants.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = plantsAdapter
        }

        // Vertical community feed — click, like and comment buttons wired here
        feedAdapter = FeedPostAdapter(
            onPostClick = { post ->
                val action = HomeFragmentDirections.actionHomeToPostDetails(post.id)
                findNavController().navigate(action)
            },
            onLikeClick = { post ->
                Log.d("LIKE_DEBUG", "Fragment: onLikeClick postId=${post.id}")
                lifecycleScope.launch {
                    val token = authViewModel.getAccessToken()
                    Log.d("LIKE_DEBUG", "Fragment: token=${if (token != null) "OK" else "NULL"}")
                    if (token == null) return@launch
                    homeViewModel.toggleLike(token, post)
                }
            },
            onCommentClick = { post ->
                lifecycleScope.launch {
                    val token = authViewModel.getAccessToken() ?: return@launch
                    CommentsBottomSheet.newInstance(post.id, token)
                        .show(parentFragmentManager, "comments_${post.id}")
                }
            }
        )
        binding.recyclerFeedPosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = feedAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupFab() {
        binding.fabAddPost.setOnClickListener {
            // Ensure MyPostsViewModel knows the current user so the plant dropdown works
            lifecycleScope.launch {
                myPostsViewModel.initUserId()
                AddEditPostBottomSheet.newAddInstance()
                    .show(parentFragmentManager, "add_post_home")
            }
        }

        // Show a snackbar when a post is created from this screen
        myPostsViewModel.snackMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                myPostsViewModel.clearSnackMessage()
            }
        }
    }

    // -------------------------------------------------------------------------
    // LiveData observers — the Fragment's only job is to push data into Views
    // -------------------------------------------------------------------------

    private fun observeViewModel() {
        // Featured plants — sourced from Room via HomeViewModel
        homeViewModel.featuredPlants.observe(viewLifecycleOwner) { plants ->
            plantsAdapter.submitList(plants)
            binding.textEmptyPlants.visibility = if (plants.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerFeaturedPlants.visibility = if (plants.isNotEmpty()) View.VISIBLE else View.GONE
        }

        // Community feed posts — sourced from Room via HomeViewModel
        homeViewModel.feedPosts.observe(viewLifecycleOwner) { posts ->
            feedAdapter.submitList(posts)
            binding.textEmptyFeed.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerFeedPosts.visibility = if (posts.isNotEmpty()) View.VISIBLE else View.GONE
        }

        // Overall UI state (loading / success / error)
        homeViewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is HomeViewModel.HomeUiState.Loading -> {
                    binding.progressFeed.visibility = View.VISIBLE
                    binding.textError.visibility = View.GONE
                    binding.btnRetry.visibility = View.GONE
                }
                is HomeViewModel.HomeUiState.Success -> {
                    binding.progressFeed.visibility = View.GONE
                    binding.textError.visibility = View.GONE
                    binding.btnRetry.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                }
                is HomeViewModel.HomeUiState.Error -> {
                    binding.progressFeed.visibility = View.GONE
                    binding.textError.text = state.message
                    binding.textError.visibility = View.VISIBLE
                    binding.btnRetry.visibility = View.VISIBLE
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }

        // Per-section loading indicators
        homeViewModel.isLoadingPlants.observe(viewLifecycleOwner) { isLoading ->
            binding.progressPlants.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        homeViewModel.isLoadingPosts.observe(viewLifecycleOwner) { isLoading ->
            binding.progressFeed.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Transient error messages shown as Snackbars
        homeViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                homeViewModel.clearError()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Data loading
    // -------------------------------------------------------------------------

    private fun fetchInitialData() {
        lifecycleScope.launch {
            val token = authViewModel.getAccessToken() ?: return@launch
            homeViewModel.loadHomeData(token)
        }
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.emerald_500)
        binding.swipeRefresh.setOnRefreshListener {
            lifecycleScope.launch {
                val token = authViewModel.getAccessToken() ?: run {
                    binding.swipeRefresh.isRefreshing = false
                    return@launch
                }
                homeViewModel.refresh(token)
            }
        }

        binding.btnRetry.setOnClickListener {
            fetchInitialData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
