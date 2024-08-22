/*Licensed under the Apache License, Version 2.0 (the "License");
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

package com.bailout.stickk.new_electronic_by_Rodeon.utils

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager

//import com.gigamole.navigationtabbar.ntb.NavigationTabBar
import com.bailout.stickk.R

import java.util.ArrayList

object NavigationUtils {
  var showAdvancedSettings = false

//  private fun getNavigationModels(mContext: Context): ArrayList<NavigationTabBar.Model> {
//    val colors = mContext.resources.getStringArray(R.array.colors)
//    val models = ArrayList<NavigationTabBar.Model>()//здесь можно настроить боттом навигэйшен бар
//    if (showAdvancedSettings) {
//      models.add(
//              NavigationTabBar.Model.Builder(
//                      ContextCompat.getDrawable(mContext, R.drawable.ic_gestures),
//                      Color.parseColor(colors[1]))
//                      .title(mContext.resources.getString(R.string.setting_up_gestures))
//                      .badgeTitle("new")
//                      .build()
//      )
//      models.add(
//              NavigationTabBar.Model.Builder(
//                      ContextCompat.getDrawable(mContext, R.drawable.ic_sensors),
//                      Color.parseColor(colors[3]))
//                      .title(mContext.resources.getString(R.string.sensor_settings))//"настройка механики")
//                      .badgeTitle("new")
//                      .build()
//      )
//      models.add(
//              NavigationTabBar.Model.Builder(
//                      ContextCompat.getDrawable(mContext, R.drawable.ic_mechanics),
//                      Color.parseColor(colors[4]))
//                      .title(mContext.resources.getString(R.string.special_settings))
//                      .badgeTitle("new")
//                      .build()
//      )
//    } else {
//      models.add(
//              NavigationTabBar.Model.Builder(
//                      ContextCompat.getDrawable(mContext, R.drawable.ic_gestures),
//                      Color.parseColor(colors[1]))
//                      .title(mContext.resources.getString(R.string.setting_up_gestures))
//                      .badgeTitle("new")
//                      .build()
//      )
//      models.add(
//              NavigationTabBar.Model.Builder(
//                      ContextCompat.getDrawable(mContext, R.drawable.ic_sensors),
//                      Color.parseColor(colors[3]))
//                      .title(mContext.resources.getString(R.string.sensor_settings))//"настройка механики")
//                      .badgeTitle("new")
//                      .build()
//      )
//    }
//    return models
//  }

//  fun setComponents(context: Context, navigationTabBar: NavigationTabBar) {
//      navigationTabBar.models = getNavigationModels(context)
//      navigationTabBar.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
//          override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
//
//          override fun onPageSelected(position: Int) {
//              navigationTabBar.models[position].hideBadge()
//          }
//
//          override fun onPageScrollStateChanged(state: Int) {}
//      })
//  }
}
