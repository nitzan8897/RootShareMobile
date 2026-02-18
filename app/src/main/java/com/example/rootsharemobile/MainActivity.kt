package com.example.rootsharemobile

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.rootsharemobile.data.local.TokenManager
import com.example.rootsharemobile.data.remote.RetrofitClient
import com.example.rootsharemobile.databinding.ActivityMainBinding

/**
 * Single-Activity host for the entire application.
 *
 * Responsibilities:
 *  - Inflate [ActivityMainBinding] (activity_main.xml).
 *  - Wire the [NavController] to the [BottomNavigationView].
 *  - Hide the bottom nav on auth destinations (Login, Register).
 *
 * No business logic lives here — the Activity is purely an
 * infrastructure component that hosts Fragments.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    /** Fragment IDs that are part of the main app (bottom nav shown). */
    private val mainDestinations = setOf(
        R.id.homeFragment,
        R.id.myGardenFragment,
        R.id.myPostsFragment,
        R.id.communityFragment,
        R.id.profileFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the root layout with ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialise Retrofit with the TokenManager so the automatic
        // token-refresh interceptor has access to stored credentials.
        RetrofitClient.init(TokenManager(applicationContext))

        // Obtain the NavController from the NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Connect the BottomNavigationView to the NavController.
        // Menu item IDs must match Fragment IDs in nav_graph.xml.
        binding.bottomNavigation.setupWithNavController(navController)

        // Show/hide bottom nav based on the current destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.visibility =
                if (destination.id in mainDestinations) View.VISIBLE else View.GONE
        }
    }

    /**
     * Allow the NavController to handle the system Back button correctly
     * (e.g., pop the Fragment back-stack instead of finishing the Activity).
     */
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
