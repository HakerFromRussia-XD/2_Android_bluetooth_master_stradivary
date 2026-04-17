package com.bailout.stickk.ubi4.ui.fragments.help

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentGestureSettingsBinding

class GestureSettingsFragmentHelpUBI4 :
    Fragment(R.layout.ubi4_fragment_gesture_settings) {

    private var _binding: Ubi4FragmentGestureSettingsBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = Ubi4FragmentGestureSettingsBinding.bind(view)

        setupSystemBack()
        initUi()
    }

    private fun setupSystemBack() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackPress()
                }
            }
        )
    }

    private fun initUi() = with(binding) {
        ubi4TitleClickBlockBtn.setOnClickListener { /* no-op */ }
        ubi4BackBtn.setOnClickListener { handleBackPress() }

        ubi4ShowInteractiveInstructionBtn.setOnClickListener {
            // пока пусто
            // сюда потом добавишь открытие интерактивной инструкции
        }

        root.isFocusableInTouchMode = true
        root.requestFocus()
    }

    private fun handleBackPress() {
        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}