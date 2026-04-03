package com.bailout.stickk.ubi4.ui.fragments.help

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentCareOfProstheticHandBinding

class CareFragmentUBI4 : Fragment(R.layout.ubi4_fragment_care_of_prosthetic_hand) {

    private var _binding: Ubi4FragmentCareOfProstheticHandBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = Ubi4FragmentCareOfProstheticHandBinding.bind(view)

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

        // Текущий экран — в себя не навигируем
        ubi4ProsthesesCareBtn.setOnClickListener { /* already here */ }

        ubi4HowProsthesesWorksBtn.setOnClickListener {
            openHelpScreen(HowProsthesesWorksFragmentUBI4())
        }
        ubi4HowToPutOnAProsthesesSocketBtn.setOnClickListener {
            openHelpScreen(HowToPutOnProsthesesSocketFragmentUBI4())
        }
        ubi4ComplectationBtn.setOnClickListener {
            openHelpScreen(CompleteSetFragmentUBI4())
        }
        ubi4ProsthesesChargeBtn.setOnClickListener {
            openHelpScreen(ChargingFragmentUBI4())
        }
        ubi4ServiceAndWarrantyBtn.setOnClickListener {
            openHelpScreen(ServiceWarrantyFragmentUBI4())
        }

        // чтобы back работал изнутри ScrollView/фокуса
        root.isFocusableInTouchMode = true
        root.requestFocus()
    }

    private fun openHelpScreen(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.slide_out_next,
                R.anim.slide_in_next,
                R.anim.slide_out
            )
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
