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
import com.example.rootsharemobile.databinding.FragmentRegisterBinding
import com.example.rootsharemobile.ui.viewmodel.AuthViewModel

/**
 * Registration screen Fragment.
 *
 * Strictly MVVM: the Fragment only observes LiveData and delegates
 * all logic (validation, API calls) to [AuthViewModel].
 */
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeAuthState()
        setupClickListeners()
    }

    // -------------------------------------------------------------------------
    // LiveData observers
    // -------------------------------------------------------------------------

    private fun observeAuthState() {
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.AuthUiState.Idle -> showIdleState()
                is AuthViewModel.AuthUiState.Loading -> showLoadingState()
                is AuthViewModel.AuthUiState.Success -> navigateToLogin()
                is AuthViewModel.AuthUiState.Error -> showError(state.message)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Click listeners
    // -------------------------------------------------------------------------

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val username = binding.editUsername.text?.toString().orEmpty()
            val email = binding.editEmail.text?.toString().orEmpty()
            val password = binding.editPassword.text?.toString().orEmpty()
            val confirmPassword = binding.editConfirmPassword.text?.toString().orEmpty()
            authViewModel.register(username, email, password, confirmPassword)
        }

        binding.btnGoogleSignUp.setOnClickListener {
            val googleAuthHelper = GoogleAuthHelper(requireActivity())
            authViewModel.signInWithGoogle(googleAuthHelper)
        }

        binding.textLoginLink.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }

    // -------------------------------------------------------------------------
    // UI state helpers
    // -------------------------------------------------------------------------

    private fun showIdleState() {
        binding.btnRegister.isEnabled = true
        binding.btnRegister.visibility = View.VISIBLE
        binding.progressRegister.visibility = View.GONE
        binding.textError.visibility = View.GONE
    }

    private fun showLoadingState() {
        binding.btnRegister.isEnabled = false
        binding.btnRegister.visibility = View.INVISIBLE
        binding.progressRegister.visibility = View.VISIBLE
        binding.textError.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.btnRegister.isEnabled = true
        binding.btnRegister.visibility = View.VISIBLE
        binding.progressRegister.visibility = View.GONE
        binding.textError.text = message
        binding.textError.visibility = View.VISIBLE
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    /**
     * After successful registration, go to Login so the user can sign in.
     * The register back-stack entry is cleared to avoid double back-press.
     */
    private fun navigateToLogin() {
        authViewModel.resetAuthState()
        findNavController().navigate(R.id.action_register_to_login)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
