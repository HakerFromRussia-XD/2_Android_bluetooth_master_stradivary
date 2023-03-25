@file:Suppress("DEPRECATION")

package me.start.motorica.new_electronic_by_Rodeon.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main.*
import me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main.ChartFragment
import me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main.AdvancedSettingsFragment

class SectionsPagerAdapterWithAdvancedSettings(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

  override fun getItem(position: Int): Fragment {
    var fragment: Fragment = ChartFragment()
    when (position) {
      0 -> fragment = GestureFragment()
      1 -> fragment = ChartFragment()
      2 -> fragment = AdvancedSettingsFragment()
    }
    return fragment
  }

  override fun getCount(): Int = COUNT_PAGERS

  companion object {
    const val COUNT_PAGERS = 3
  }
}
