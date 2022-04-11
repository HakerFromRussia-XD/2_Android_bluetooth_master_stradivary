@file:Suppress("DEPRECATION")

package me.start.motorica.new_electronic_by_Rodeon.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main.*
import me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main.ChartFragment

class SectionsPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

  override fun getItem(position: Int): Fragment {
    var fragment: Fragment = ChartFragment()
    when (position) {
      0 -> fragment = GestureFragment()//AlarmFragment()
      1 -> fragment = ChartFragment()//MainWaterFragment()
    }
    return fragment
  }

  override fun getCount(): Int = COUNT_PAGERS

  companion object {
    const val COUNT_PAGERS = 2
  }
}
