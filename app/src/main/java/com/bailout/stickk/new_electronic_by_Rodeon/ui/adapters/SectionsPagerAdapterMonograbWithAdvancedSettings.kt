@file:Suppress("DEPRECATION")

package com.bailout.stickk.new_electronic_by_Rodeon.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.ubi3.main.ChartFragment
import com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.ubi3.main.AdvancedSettingsFragment

class SectionsPagerAdapterMonograbWithAdvancedSettings(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

  override fun getItem(position: Int): Fragment {
    var fragment: Fragment = ChartFragment()
    when (position) {
      0 -> fragment = ChartFragment()
      1 -> fragment = AdvancedSettingsFragment()
    }
    return fragment
  }

  override fun getCount(): Int = COUNT_PAGERS

  companion object {
    const val COUNT_PAGERS = 2

  }
}
