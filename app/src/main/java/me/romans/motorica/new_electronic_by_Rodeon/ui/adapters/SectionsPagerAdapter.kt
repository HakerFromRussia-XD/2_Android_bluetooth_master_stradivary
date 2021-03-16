/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.romans.motorica.new_electronic_by_Rodeon.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import me.romans.motorica.new_electronic_by_Rodeon.ui.fragments.main.*
import me.romans.motorica.new_electronic_by_Rodeon.ui.fragments.main.ChartFragment
import me.romans.motorica.new_electronic_by_Rodeon.ui.fragments.main.MainWaterFragment
import me.romans.motorica.new_electronic_by_Rodeon.ui.fragments.main.SensSettingsFragment

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
