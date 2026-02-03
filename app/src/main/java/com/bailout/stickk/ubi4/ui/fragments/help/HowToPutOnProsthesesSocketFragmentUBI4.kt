package com.bailout.stickk.ubi4.ui.fragments.help

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentHowToPutOnAProsthesisSocketBinding

class HowToPutOnProsthesesSocketFragmentUBI4 :
    Fragment(R.layout.ubi4_fragment_how_to_put_on_a_prosthesis_socket) {

    private var _binding: Ubi4FragmentHowToPutOnAProsthesisSocketBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = Ubi4FragmentHowToPutOnAProsthesisSocketBinding.bind(view)

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

        // Текущий экран — навигации в себя не делаем
        ubi4HowToPutOnAProsthesesSocketBtn.setOnClickListener { /* already here */ }

        // Остальные help-экраны
        ubi4HowProsthesesWorksBtn.setOnClickListener {
            openHelpScreen(HowProsthesesWorksFragmentUBI4())
        }
        ubi4ComplectationBtn.setOnClickListener {
            openHelpScreen(CompleteSetFragmentUBI4())
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

        // фикс для back + focus внутри ScrollView
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