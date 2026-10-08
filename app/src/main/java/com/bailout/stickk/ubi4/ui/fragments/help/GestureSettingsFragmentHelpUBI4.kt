package com.bailout.stickk.ubi4.ui.fragments.help

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentGestureSettingsBinding
import com.bailout.stickk.ubi4.data.state.UiState

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
        if (UiState.isInterfaceV3Activated) {
            ubi4RelatedSpecialSettingsTitleTv.setText(R.string.special_settings)
        }
        if (resources.configuration.locales[0].language != "ru") {
            ubi4ImageView4.setImageResource(R.drawable.ubi4_help_image_gesture_settings_1_en)
            ubi4ImageView5.setImageResource(R.drawable.ubi4_help_image_gesture_settings_2_en)
            ubi4ImageView5Repeat.setImageResource(R.drawable.ubi4_help_image_gesture_settings_3_en)
            ubi4ImageView6.setImageResource(R.drawable.ubi4_help_image_gesture_settings_active_en)
            ubi4ImageView7.setImageResource(R.drawable.ubi4_help_image_gesture_settings_4_en)
            ubi4ImageView8.setImageResource(R.drawable.ubi4_help_image_gesture_settings_5_en)
            ubi4ImageView9.setImageResource(R.drawable.ubi4_help_image_gesture_settings_6_en)
            ubi4ImageView10.setImageResource(R.drawable.ubi4_help_image_gesture_settings_7_en)
            ubi4ImageView12.setImageResource(R.drawable.ubi4_help_image_gesture_settings_8_en)
            ubi4ImageView13.setImageResource(R.drawable.ubi4_help_image_gesture_settings_9_en)
        }

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
