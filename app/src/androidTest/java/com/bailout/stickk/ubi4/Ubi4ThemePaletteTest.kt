package com.bailout.stickk.ubi4

import android.content.res.Configuration
import android.graphics.Color
import android.os.LocaleList
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.test.platform.app.InstrumentationRegistry
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentSpecialSettingsBinding
import com.bailout.stickk.databinding.Ubi4WidgetSliderBinding
import com.bailout.stickk.databinding.Ubi4WidgetToggleSliderBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Shared XML widgets serve both UBI4 and V3; locale qualifiers must not mask light colors. */
class Ubi4ThemePaletteTest {
    @Test fun palettesAndSharedWidgetsWorkInBothThemesAndLocales() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (locale in listOf("en", "ru", "ru-RU")) {
                for (nightMode in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                    val configuration = Configuration(instrumentation.targetContext.resources.configuration).apply {
                        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
                        setLocales(LocaleList.forLanguageTags(locale))
                    }
                    val context = ContextThemeWrapper(
                        instrumentation.targetContext.createConfigurationContext(configuration), R.style.Theme_UBI4,
                    )
                    val light = nightMode == Configuration.UI_MODE_NIGHT_NO
                    val background = context.getColor(R.color.ubi4_back)
                    val surface = context.getColor(R.color.ubi4_gray)
                    assertEquals(Color.parseColor(if (light) "#F4F5F2" else "#2A2A2A"), background)
                    assertEquals(Color.parseColor(if (light) "#FFFFFF" else "#373737"), surface)
                    assertEquals(Color.parseColor(if (light) "#06B87B" else "#C6F158"), context.getColor(R.color.ubi4_active))
                    assertEquals(light, context.resources.getBoolean(R.bool.ubi4_light_system_bars))
                    assertTrue(ColorUtils.calculateContrast(context.getColor(R.color.ubi4_text_primary), surface) >= 4.5)
                    assertTrue(ColorUtils.calculateContrast(context.getColor(R.color.ubi4_active_foreground), surface) >= 4.5)
                    if (light) {
                        assertTrue(ColorUtils.calculateContrast(context.getColor(R.color.ubi4_control_disabled_text), surface) >= 3.0)
                    }

                    val inflater = LayoutInflater.from(context)
                    val settings = Ubi4FragmentSpecialSettingsBinding.inflate(inflater)
                    assertEquals(context.getColor(R.color.ubi4_text_primary), settings.mobileSettingsBtn.currentTextColor)
                    assertEquals(context.getColor(R.color.ubi4_text_primary), settings.appearanceSettingsRow.spinnerTv.currentTextColor)
                    // Exercise Android's selected layout variant, including layout-v31.
                    assertReadableSliderText(Ubi4WidgetSliderBinding.inflate(inflater).root, surface)
                    assertReadableSliderText(Ubi4WidgetToggleSliderBinding.inflate(inflater).root, surface)
                }
            }
        }
    }

    private fun assertReadableSliderText(view: View, surface: Int) {
        if (view is TextView) {
            assertTrue(
                "Slider text ${view.resources.getResourceEntryName(view.id)} blends into its surface",
                ColorUtils.calculateContrast(view.currentTextColor, surface) >= 4.5,
            )
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) assertReadableSliderText(view.getChildAt(index), surface)
        }
    }
}
