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

package com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference

import android.annotation.SuppressLint
import android.content.Context

class PreferenceManager(private val mContext: Context) {
  private val preferenceKey = "motoricaStart"

  fun getBoolean(key: String, default_value: Boolean): Boolean {
    val pref = mContext.getSharedPreferences(preferenceKey, 0)
    return pref.getBoolean(key, default_value)
  }

  fun getInt(key: String, default_value: Int): Int {
    val pref = mContext.getSharedPreferences(preferenceKey, 0)
    return pref.getInt(key, default_value)
  }

  fun getString(key: String, default_value: String): String {
    val pref = mContext.getSharedPreferences(preferenceKey, 0)
    return pref.getString(key, default_value)!!
  }

  @SuppressLint("CommitPrefEdits")
  fun putBoolean(key: String, default_value: Boolean) {
    val pref = mContext.getSharedPreferences(preferenceKey, 0)
    val editor = pref.edit()
    editor.putBoolean(key, default_value).apply()
  }

  @SuppressLint("CommitPrefEdits")
  fun putInt(key: String, default_value: Int) {
    val pref = mContext.getSharedPreferences(preferenceKey, 0)
    val editor = pref.edit()
    editor.putInt(key, default_value).apply()
  }

  @SuppressLint("CommitPrefEdits")
  fun putString(key: String, default_value: String) {
    val pref = mContext.getSharedPreferences(preferenceKey, 0)
    val editor = pref.edit()
    editor.putString(key, default_value).apply()
  }
}
