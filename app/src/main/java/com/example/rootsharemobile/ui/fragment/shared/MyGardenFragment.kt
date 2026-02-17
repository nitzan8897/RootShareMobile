package com.example.rootsharemobile.ui.fragment.shared

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.rootsharemobile.databinding.FragmentMyGardenBinding

/**
 * My Garden placeholder Fragment.
 * Will display the user's full plant collection in a future iteration.
 */
class MyGardenFragment : Fragment() {

    private var _binding: FragmentMyGardenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyGardenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
