package com.bailout.stickk.ubi4.ui.fragments.help

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentSensorSettingsBinding
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys

class SensorsFragmentHelpUBI4 : Fragment(R.layout.ubi4_fragment_sensor_settings) {

    private var _binding: Ubi4FragmentSensorSettingsBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val settings by lazy(LazyThreadSafetyMode.NONE) {
        requireContext().getSharedPreferences(PreferenceKeys.APP_PREFERENCES, android.content.Context.MODE_PRIVATE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = Ubi4FragmentSensorSettingsBinding.bind(view)

        setupSystemBack()
        initUi()
    }

    private fun setupSystemBack() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = handleBackPress()
            }
        )
    }

    private fun initUi() = with(binding) {
        ubi4TitleClickBlockBtn.setOnClickListener { /* no-op */ }
        ubi4BackBtn.setOnClickListener { handleBackPress() }

        // RU картинки (упрощенно через Locale, без main.locate)
//        val isRu = java.util.Locale.getDefault().language.equals("ru", ignoreCase = true)
//        if (isRu) {
//            ubi4ImageView9.setImageResource(R.drawable.help_image_9_ru)
//            ubi4ImageView10.setImageResource(R.drawable.help_image_10_ru)
//        }

        // Тут без "интерактивной инструкции" — чтобы не тащить ChartFragment
        ubi4ShowInteractiveInstructionBtn.setOnClickListener { /* no-op */ }

        // Навигация на другие help-экраны (фрагменты UBI4)
//        ubi4SettingsGestureBtn.setOnClickListener { openHelpScreen(GesturesHelpFragmentUBI4()) }
//        ubi4AdvancedSettingsBtn.setOnClickListener {
//            openHelpScreen(HelpMonoAdvancedSettingsFragmentUBI4())
//        }

        // Видимость как в legacy
        val adv = settings.getInt(PreferenceKeys.ADVANCED_SETTINGS, 4)
        if (adv == 1) {
            ubi4AppInstructionTitle2Tv.visibility = View.VISIBLE
            ubi4MainControlsCv.visibility = View.VISIBLE
            ubi4AdvancedSettingsRl.visibility = View.VISIBLE
        }

        root.isFocusableInTouchMode = true
        root.requestFocus()
    }

    private fun openHelpScreen(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun handleBackPress() {
        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}