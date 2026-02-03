package com.bailout.stickk.ubi4.ui.fragments.help

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentHowProsthesisWorksBinding

class HowProsthesesWorksFragmentUBI4 : Fragment(R.layout.ubi4_fragment_how_prosthesis_works) {

    private var _binding: Ubi4FragmentHowProsthesisWorksBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = Ubi4FragmentHowProsthesisWorksBinding.bind(view)

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

        // Здесь кнопка "How prostheses works" — если это текущий экран,
        // то логично либо ничего не делать, либо закрывать сам Help.
        // Я сделал no-op, чтобы не было “навигации в себя”.
        ubi4HowProsthesesWorksBtn.setOnClickListener { /* already here */ }

        ubi4HowToPutOnAProsthesesSocketBtn.setOnClickListener {
            openHelpScreen(HowToPutOnProsthesesSocketFragmentUBI4())

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
        // Вариант B: просто назад по back stack
        parentFragmentManager.popBackStack()

        // Если хочешь “вернуться к источнику” (как у Account/Help), тогда тут можно сделать:
        // val source = arguments?.getString(ARG_SOURCE_FRAGMENT_CLASS)
        // ...и replace на нужный фрагмент. Но это уже гибрид B/A.
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_SOURCE_FRAGMENT_CLASS = "sourceFragmentClass"
    }
}