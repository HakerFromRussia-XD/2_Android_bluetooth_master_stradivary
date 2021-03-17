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

package me.romans.motorica.new_electronic_by_Rodeon.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date


object DateUtils {

  val dateFormat: String
    get() = "yyyy-MM-dd"// HH:mm:ss

  val dateFormatWithTime: String
    get() = "yyyy-MM-dd HH:mm:ss"// HH:mm:ss

  fun getFarDay(far: Int): String {
    val date = Date()
    val cal = Calendar.getInstance()
    cal.time = date
    cal.add(Calendar.DAY_OF_MONTH, far)
    val sdf = SimpleDateFormat(dateFormat)
    val currentDateandTime = sdf.format(cal.time)
    System.err.println("getFarDay --> " +currentDateandTime)
    return currentDateandTime
  }

  fun getDateDay(date: String, dateType: String): Int {
    try {
      val dateFormat = SimpleDateFormat(dateType)
      val nDate = dateFormat.parse(date)
      val cal = Calendar.getInstance()
//      System.err.println("getDateDay --> nDate="+nDate)
      cal.time = nDate
//      System.err.println("getDateDay --> cal.time="+cal.time)
      System.err.println("getDateDay --> cal.get(Calendar.DAY_OF_WEEK) = "+(cal.get(Calendar.DAY_OF_WEEK)-1))
      return cal.get(Calendar.DAY_OF_WEEK) - 1
    } catch (e: ParseException) {
      e.printStackTrace()
    }

    return -1
  }

  fun getDayofWeek(data: String, dateType: String): Int {
    try {
      val dateFormat = SimpleDateFormat(dateType)
      val nDate = dateFormat.parse(data)
      val c = Calendar.getInstance()
      c.time = nDate
      val dayOfWeek = c.get(Calendar.DAY_OF_WEEK)
      return dayOfWeek
    } catch (e: ParseException) {
      e.printStackTrace()
    }

    return -1
  }

  fun getDayNameList(days: String): String {
    val builder = StringBuilder()
    if (days.contains("0"))
      builder.append("пн")
    if (days.contains("1"))
      builder.append("вт")
    if (days.contains("2"))
      builder.append("ср")
    if (days.contains("3"))
      builder.append("чт")
    if (days.contains("4"))
      builder.append("пт")
    if (days.contains("5"))
      builder.append("сб")
    if (days.contains("6"))
      builder.append("вс")
    return builder.toString()
  }

  fun getIndexOfDayName(index: Int): String {
    val dname: String
    when (index) {
      1 -> dname = "понедельник"
      2 -> dname = "вторник"
      3 -> dname = "среда"
      4 -> dname = "четверг"
      5 -> dname = "пятница"
      6 -> dname = "суббота"
      else -> dname = "воскресенье"
    }
    return dname
  }

  fun getIndexofDayNameHead(index: Int): String {
    var dayName = " (вс)"
    when (index) {
      1 -> dayName = " (вс)"
      2 -> dayName = " (пн)"
      3 -> dayName = " (вт)"
      4 -> dayName = " (ср)"
      5 -> dayName = " (чт)"
      6 -> dayName = " (пт)"
      7 -> dayName = " (сб)"
    }
    return dayName
  }
}
