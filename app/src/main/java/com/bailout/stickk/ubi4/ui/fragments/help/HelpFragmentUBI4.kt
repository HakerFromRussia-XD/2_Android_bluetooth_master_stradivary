package com.bailout.stickk.ubi4.ui.fragments.help

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentHelpBinding
import com.bailout.stickk.ubi4.ui.fragments.AdvancedFragment
import com.bailout.stickk.ubi4.ui.fragments.GesturesFragment
import com.bailout.stickk.ubi4.ui.fragments.SensorsFragment
import com.bailout.stickk.ubi4.ui.fragments.SpecialSettingsFragment
import com.bailout.stickk.ubi4.ui.fragments.SprGestureFragment
import com.bailout.stickk.ubi4.ui.fragments.SprTrainingFragment

class HelpFragmentUBI4 : Fragment(R.layout.ubi4_fragment_help) {

    private var _binding: Ubi4FragmentHelpBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = Ubi4FragmentHelpBinding.bind(view)

        setupSystemBack()
        initUi()
        renderVisibility()
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
        // заглушка на “клик по заголовку”
        ubi4TitleClickBlockBtn.setOnClickListener { }

        // Back в шапке
        ubi4BackBtn.setOnClickListener { handleBackPress() }

        // App control
        ubi4SensorsSettingsBtn.setOnClickListener { openScreen(SensorsFragment()) }
        ubi4SettingsGestureBtn.setOnClickListener { openScreen(GesturesFragment()) }
        ubi4AdvancedSettingsBtn.setOnClickListener { openScreen(AdvancedFragment()) }

        // Prostheses use (подставь свои реальные фрагменты)
        ubi4HowProsthesesWorksBtn.setOnClickListener {
            openScreen(HowProsthesesWorksFragmentUBI4())
        }
        ubi4HowToPutOnAProsthesesSocketBtn.setOnClickListener {
            openScreen(HowToPutOnProsthesesSocketFragmentUBI4())
        }

        ubi4ComplectationBtn.setOnClickListener {
            openScreen(CompleteSetFragmentUBI4())
        }
        ubi4ProsthesesChargeBtn.setOnClickListener {
            openScreen(ChargingFragmentUBI4())
        }
        ubi4ProsthesesCareBtn.setOnClickListener {
            openScreen(CareFragmentUBI4())
        }
        ubi4ServiceAndWarrantyBtn.setOnClickListener {
            openScreen(ServiceWarrantyFragmentUBI4())
        }

        // Contact us
        ubi4ContactSupportBtn.setOnClickListener { openDialer("88007077197") }
        ubi4VkBtn.setOnClickListener { openUrl("https://vk.com/motorica") }
        ubi4TelegrammBtn.setOnClickListener { openUrl("https://t.me/motoricans") }

        // чтобы Back отрабатывал при фокусе внутри ScrollView
        root.isFocusableInTouchMode = true
        root.requestFocus()
    }

    private fun renderVisibility() = with(binding) {
        // если нужно — сюда правила видимости
        // ubi4AdvancedSettingsRl.visibility = View.VISIBLE
        // ubi4GestureCustomizationRl.visibility = View.VISIBLE
    }

    private fun openScreen(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun handleBackPress() {
        val source = arguments?.getString(ARG_SOURCE_FRAGMENT)

        if (source.isNullOrBlank()) {
            parentFragmentManager.popBackStack()
            return
        }

        val target: Fragment? = when (source) {
            SensorsFragment::class.java.name -> SensorsFragment()
            SpecialSettingsFragment::class.java.name -> SpecialSettingsFragment()
            SprTrainingFragment::class.java.name -> SprTrainingFragment()
            SprGestureFragment::class.java.name -> SprGestureFragment()
            else -> null
        }

        if (target == null) {
            parentFragmentManager.popBackStack()
            return
        }

        // Важно: мы “возвращаемся” на экран-источник без Navigator.
        // Чтобы не плодить back stack — чистим до корня и ставим target как текущий.
        parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, target)
            .commit()
    }

    private fun openDialer(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        runCatching { startActivity(intent) }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        runCatching { startActivity(intent) }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_SOURCE_FRAGMENT = "sourceFragmentClass"
    }
}
