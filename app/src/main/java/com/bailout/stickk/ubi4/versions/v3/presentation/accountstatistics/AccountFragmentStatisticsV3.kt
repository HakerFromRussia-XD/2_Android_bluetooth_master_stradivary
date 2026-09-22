package com.bailout.stickk.ubi4.versions.v3.presentation.accountstatistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentAccountStatisticsV3Binding
import com.bailout.stickk.ubi4.versions.v3.di.V3AccountStatisticsViewModelFactory
import kotlinx.coroutines.launch

class AccountFragmentStatisticsV3 : Fragment() {
    private var _binding: Ubi4FragmentAccountStatisticsV3Binding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: V3AccountStatisticsViewModel by viewModels {
        V3AccountStatisticsViewModelFactory.from(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = Ubi4FragmentAccountStatisticsV3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.gestureUsageChart.setTitle(getString(R.string.gesture_usage_chart_title))
        // The previous screen subscribed and requested once per view, not once per START.
        viewModel.onAction(V3AccountStatisticsAction.ViewAttached)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.gestureUsageChart.setItems(V3AccountStatisticsChartMapper.map(requireContext(), state))
            }
        }
    }

    override fun onDestroyView() {
        viewModel.onAction(V3AccountStatisticsAction.ViewDetached)
        _binding = null
        super.onDestroyView()
    }
}

/** FragmentManager may restore a back stack saved before this screen changed package. */
internal fun FragmentFactory.withAccountStatisticsCompatibility(): FragmentFactory {
    val delegate = this
    return object : FragmentFactory() {
        override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
            val restoredName = if (className == "com.bailout.stickk.ubi4.ui.fragments.account.statisticsFragmentV3.AccountFragmentStatisticsV3") {
                AccountFragmentStatisticsV3::class.java.name
            } else className
            return delegate.instantiate(classLoader, restoredName)
        }
    }
}
