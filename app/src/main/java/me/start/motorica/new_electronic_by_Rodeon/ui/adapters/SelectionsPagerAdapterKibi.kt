@file:Suppress("DEPRECATION")
package me.start.motorica.new_electronic_by_Rodeon.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main.KibiFragment

class SelectionsPagerAdapterKibi (fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        var fragment: Fragment = KibiFragment()
        when (position) {
            0 -> fragment = KibiFragment()
        }
        return fragment
    }

    override fun getCount(): Int = COUNT_PAGERS

    companion object {
        const val COUNT_PAGERS = 1
    }
}