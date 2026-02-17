package com.example.rootsharemobile.ui.fragment.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.rootsharemobile.R
import com.example.rootsharemobile.data.auth.GoogleAuthHelper
import com.example.rootsharemobile.databinding.FragmentLoginBinding
import com.example.rootsharemobile.ui.viewmodel.AuthViewModel

/**
 * Login screen Fragment.
 *
 * Responsibilities (Fragment's only job in MVVM):
 *  - Inflate the XML layout and bind UI events to ViewModel methods.
 *  - Observe ViewModel LiveData and update Views accordingly.
 *  - Navigate using the NavController (SafeArgs-generated directions).
 *
 * The Fragment holds NO business logic — all validation and API calls
 * live in [AuthViewModel].
 */
class LoginFragment : Fragment() {

    // ViewBinding reference — cleared in onDestroyView to avoid memory leaks.
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // ViewModel scoped to this Fragment
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeAuthState()
        observeLoginStatus()
        setupClickListeners()
    }

    // -------------------------------------------------------------------------
    // LiveData observers
    // -------------------------------------------------------------------------

    /**
     * Observe the login state from DataStore.
     * If already logged in (e.g., app restart), skip directly to Home.
     */
    private fun observeLoginStatus() {
        authViewModel.isLoggedIn.observe(viewLifecycleOwner) { loggedIn ->
            if (loggedIn) navigateToHome()
        }
    }

    /**
     * React to auth state changes emitted by the ViewModel after a login attempt.
     */
    private fun observeAuthState() {
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.AuthUiState.Idle -> showIdleState()
                is AuthViewModel.AuthUiState.Loading -> showLoadingState()
                is AuthViewModel.AuthUiState.Success -> navigateToHome()
                is AuthViewModel.AuthUiState.Error -> showError(state.message)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Click listeners — delegate immediately to the ViewModel
    // -------------------------------------------------------------------------

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.editEmail.text?.toString().orEmpty()
            val password = binding.editPassword.text?.toString().orEmpty()
            authViewModel.login(email, password)
        }

        binding.btnGoogleSignIn.setOnClickListener {
            val googleAuthHelper = GoogleAuthHelper(requireActivity())
            authViewModel.signInWithGoogle(googleAuthHelper)
        }

        binding.textRegisterLink.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    // -------------------------------------------------------------------------
    // UI state helpers
    // -------------------------------------------------------------------------

    private fun showIdleState() {
        binding.btnLogin.isEnabled = true
        binding.btnLogin.visibility = View.VISIBLE
        binding.progressLogin.visibility = View.GONE
        binding.textError.visibility = View.GONE
    }

    private fun showLoadingState() {
        binding.btnLogin.isEnabled = false
        binding.btnLogin.visibility = View.INVISIBLE
        binding.progressLogin.visibility = View.VISIBLE
        binding.textError.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.btnLogin.isEnabled = true
        binding.btnLogin.visibility = View.VISIBLE
        binding.progressLogin.visibility = View.GONE
        binding.textError.text = message
        binding.textError.visibility = View.VISIBLE
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    /**
     * Navigate to HomeFragment, passing the username via SafeArgs.
     * The login back-stack is cleared so the user cannot press Back to return.
     */
    private fun navigateToHome() {
        val username = authViewModel.currentUser.value?.username ?: "Gardener"
        val action = LoginFragmentDirections.actionLoginToHome(username = username)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
