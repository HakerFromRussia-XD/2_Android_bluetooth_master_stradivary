/*
 * Copyright (C) 2016 skydoves
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package me.romans.motorica.new_electronic_by_Rodeon.utils

import android.content.Context
import android.graphics.Color
import android.icu.util.TimeUnit.values
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager

import com.gigamole.navigationtabbar.ntb.NavigationTabBar
import me.romans.motorica.R
import java.time.chrono.JapaneseEra.values

import java.util.ArrayList

object NavigationUtils {
  private fun getNavigationModels(mContext: Context): ArrayList<NavigationTabBar.Model> {
    val colors = mContext.resources.getStringArray(R.array.colors)
    val models = ArrayList<NavigationTabBar.Model>()//здесь можно настроить боттом навигэйшен бар
    models.add(
        NavigationTabBar.Model.Builder(
            ContextCompat.getDrawable(mContext, R.drawable.ic_gestures),
            Color.parseColor(colors[1]))
            .title("настройка жестов")
            .badgeTitle("new")
            .build()
    )
    models.add(
        NavigationTabBar.Model.Builder(
            ContextCompat.getDrawable(mContext, R.drawable.ic_mechanics),
            Color.parseColor(colors[3]))
            .title("настройка механики")//"настройка механики")
            .badgeTitle("new")
            .build()
    )
    models.add(
        NavigationTabBar.Model.Builder(
            ContextCompat.getDrawable(mContext, R.drawable.ic_sensors),
            Color.parseColor(colors[4]))
            .title("настройка датчиков")
            .badgeTitle("new")
            .build()
    )
    return models
  }

  fun setComponents(context: Context, viewPager: ViewPager, navigationTabBar: NavigationTabBar) {
    navigationTabBar.models = getNavigationModels(context)
    navigationTabBar.setViewPager(viewPager, 1)//здесь можно настроить номер вью из боттом бара, открывающейся при страте приложения
    navigationTabBar.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
      override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

      override fun onPageSelected(position: Int) {
        navigationTabBar.models[position].hideBadge()
      }

      override fun onPageScrollStateChanged(state: Int) {}
    })
  }
}
