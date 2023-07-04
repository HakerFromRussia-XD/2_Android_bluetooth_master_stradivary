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

package me.start.motorica.new_electronic_by_Rodeon

import me.start.motorica.new_electronic_by_Rodeon.presenters.GripperScreenPresenter
import me.start.motorica.new_electronic_by_Rodeon.presenters.MainPresenter
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.intro.StartActivity
import me.start.motorica.new_electronic_by_Rodeon.ui.fragments.help.*
import me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main.*
import me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main.ChartFragment
import me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main.AdvancedSettingsFragment


interface ApplicationGraph {
  fun inject(target_: StartActivity)

  fun inject(target_: MainPresenter)

  fun inject(target_: ChartFragment)
  fun inject(target_: AdvancedSettingsFragment)
  fun inject(target_: GestureFragment)
  fun inject(target_: GripperFragment)
  fun inject(target_: GripperScreenPresenter)
  fun inject(target_: KibiFragment)

  fun inject(settingsFragment: HelpFragment)
  fun inject(sensorsFragment: SensorsFragment) {}
  fun inject(gestureCustomizationFragment: GestureCustomizationFragment) {}

  fun inject(howProsthesesWorksFragment: HowProsthesesWorksFragment)
  fun inject(howProsthesesWorksMonoFragment: HowProsthesesWorksMonoFragment) {}
  fun inject(howToPutOnProsthesesSocketFragment: HowToPutOnProsthesesSocketFragment) {}
  fun inject(completeSetFragment: CompleteSetFragment) {}
  fun inject(chargingTheProsthesesFragment: ChargingTheProsthesesFragment) {}
  fun inject(prosthesesCareFragment: ProsthesesCareFragment) {}
  fun inject(serviceAndWarrantyFragment: ServiceAndWarrantyFragment) {}
  fun inject(advancedSettingsFragmentMulty: AdvancedSettingsFragmentMulty) {}
  fun inject(advancedSettingsFragmentMono: AdvancedSettingsFragmentMono) {}
}
