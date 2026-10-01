package com.bailout.stickk.ubi4

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bailout.stickk.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

/** Render the real instruction layout without connecting to a prosthesis. */
@RunWith(AndroidJUnit4::class)
class AdvancedInstructionLocalizationTest {
    @Test fun sensorLegendMatchesFigmaReference() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        for ((tag, openingTitle, closingTitle) in listOf(
            Triple("ru-RU", "Датчик открытия", "Датчик закрытия"),
            Triple("en-US", "Opening sensor", "Closing sensor")
        )) {
            instrumentation.runOnMainSync {
                val configuration = Configuration(instrumentation.targetContext.resources.configuration)
                configuration.setLocale(Locale.forLanguageTag(tag))
                val context = ContextThemeWrapper(instrumentation.targetContext.createConfigurationContext(configuration), androidx.appcompat.R.style.Theme_AppCompat)
                val root = LayoutInflater.from(context).inflate(R.layout.ubi4_fragment_sensor_settings, null)
                val density = context.resources.displayMetrics.density
                val width = (360 * density).toInt()
                root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(width * 2, View.MeasureSpec.EXACTLY))
                root.layout(0, 0, root.measuredWidth, root.measuredHeight)
                val opening = root.findViewById<TextView>(R.id.ubi4TextView27)
                val closing = root.findViewById<TextView>(R.id.ubi4TextView26)
                assertEquals(openingTitle, opening.text.toString())
                assertEquals(closingTitle, closing.text.toString())
                assertEquals(opening.bottom, closing.top)
                for (row in listOf(opening, closing)) {
                    assertEquals((30 * density).roundToInt(), row.height)
                    assertEquals(0xff838383.toInt(), row.currentTextColor)
                    assertEquals(context.resources.getFont(R.font.font_open_sans_regular), row.typeface)
                }
                assertEquals(4 * density, opening.shadowRadius, 0.01f)
                assertEquals(4 * density, opening.shadowDy, 0.01f)
                assertEquals(0f, closing.shadowRadius, 0.01f)
                val padding = (10 * density).toInt()
                val bitmap = Bitmap.createBitmap(opening.width, opening.height + closing.height + padding * 2, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(context.getColor(R.color.ubi4_gray))
                canvas.translate(0f, padding.toFloat())
                opening.draw(canvas)
                canvas.translate(0f, opening.height.toFloat())
                closing.draw(canvas)
                assertEquals(0xffffffff.toInt(), bitmap.getPixel((7 * density).toInt(), padding + opening.height / 2))
                assertEquals(0xffbfbfbf.toInt(), bitmap.getPixel((7 * density).toInt(), padding + opening.height + closing.height / 2))
                val locale = if (tag.startsWith("ru")) "ru" else "en"
                File(instrumentation.targetContext.getExternalFilesDir(null), "instruction-sensor-legend-$locale.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                bitmap.recycle()
            }
        }
    }

    @Test fun russianAndEnglishDescriptionsAndWidgetAssetsFollowConfiguration() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        for ((tag, expectedTitle) in listOf(
            "ru-RU" to "Переключение жестов сенсорами",
            "en-US" to "Switching gestures with sensors"
        )) {
            instrumentation.runOnMainSync {
                val configuration = Configuration(instrumentation.targetContext.resources.configuration)
                configuration.setLocale(Locale.forLanguageTag(tag))
                val localized = instrumentation.targetContext.createConfigurationContext(configuration)
                val context = ContextThemeWrapper(localized, androidx.appcompat.R.style.Theme_AppCompat)
                val root = LayoutInflater.from(context).inflate(R.layout.ubi4_fragment_advanced_settings_help, null)
                val content = root.findViewById<ViewGroup>(R.id.ubi4AdvancedSettingsContentLl)
                for (i in 0 until content.childCount) content.getChildAt(i).visibility = View.VISIBLE
                val titles = descendants(content).filterIsInstance<TextView>()
                assertTrue(titles.any { it.text.toString() == expectedTitle })
                assertEquals(32, titles.size)
                val headingFont = context.resources.getFont(R.font.font_inter_semi_bold)
                val bodyFont = context.resources.getFont(R.font.font_open_sans_regular)
                titles.forEach { text ->
                    val isHeading = context.resources.getResourceEntryName(text.id).endsWith("TitleTv")
                    assertEquals(if (isHeading) headingFont else bodyFont, text.typeface)
                    assertEquals(14f, text.textSize / context.resources.displayMetrics.scaledDensity, 0.01f)
                    assertFalse(text.text.contains("Если хочешь"))
                }
                val locale = if (tag.startsWith("ru")) "ru" else "en"
                val illustrations = descendants(content).filterIsInstance<ImageView>()
                assertEquals(8, illustrations.size)
                for (index in 1..8) {
                    val id = context.resources.getIdentifier("ubi4_help_widget_$index", "drawable", context.packageName)
                    val value = TypedValue()
                    context.resources.getValue(id, value, true)
                    assertTrue(value.string.toString(), value.string.toString().endsWith("ubi4_help_widget_${index}_$locale.png"))
                }
                val width = (360 * context.resources.displayMetrics.density).toInt()
                root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(width * 2, View.MeasureSpec.EXACTLY))
                root.layout(0, 0, root.measuredWidth, root.measuredHeight)
                assertTrue(content.height > 0)
                titles.forEach { text ->
                    assertTrue("Unmeasured text: ${text.text}", text.lineCount > 0)
                    assertEquals("Truncated text: ${text.text}", 0, text.layout.getEllipsisCount(text.lineCount - 1))
                    assertTrue(text.layout.getLineBottom(text.lineCount - 1) <= text.height)
                }
                val bitmap = Bitmap.createBitmap(content.width, content.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(context.getColor(R.color.ubi4_back))
                content.draw(canvas)
                val output = File(instrumentation.targetContext.getExternalFilesDir(null), "instruction-$locale.png")
                output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
        }
    }

    private fun descendants(view: View): List<View> = buildList {
        add(view)
        if (view is ViewGroup) for (i in 0 until view.childCount) addAll(descendants(view.getChildAt(i)))
    }
}
