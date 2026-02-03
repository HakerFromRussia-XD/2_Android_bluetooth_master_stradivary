package com.bailout.stickk.ubi4.ui.fragments.help

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentCompleteSetBinding

class CompleteSetFragmentUBI4 : Fragment(R.layout.ubi4_fragment_complete_set) {

    private var _binding: Ubi4FragmentCompleteSetBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = Ubi4FragmentCompleteSetBinding.bind(view)

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
        ubi4ComplectationBtn.setOnClickListener { /* already here */ }

        ubi4HowProsthesesWorksBtn.setOnClickListener {
            openHelpScreen(HowProsthesesWorksFragmentUBI4())
        }
        ubi4HowToPutOnAProsthesesSocketBtn.setOnClickListener {
            openHelpScreen(HowToPutOnProsthesesSocketFragmentUBI4())
        }
        ubi4ProsthesesChargeBtn.setOnClickListener {
            openHelpScreen(ChargingFragmentUBI4())
        }
        ubi4ProsthesesCareBtn.setOnClickListener {
            openHelpScreen(CareFragmentUBI4())
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