package com.bailout.stickk.ubi4

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatViewInflater
import androidx.appcompat.widget.AppCompatImageView
import androidx.test.platform.app.InstrumentationRegistry
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetSpinnerBinding
import com.bailout.stickk.databinding.Ubi4WidgetToggleSliderBinding
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.widgets.ToggleSliderItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SCREEN_TIMEOUT
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderValue
import com.bailout.stickk.ubi4.versions.v3.presentation.togglesliders.ToggleSliderDelegateAdapterV3
import com.bailout.stickk.ubi4.versions.v3.presentation.togglesliders.ToggleSliderUiStateV3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Ubi4ThemeWidgetControlsTest {
    @Test fun togglePowerKeepsItsHighlightAfterBindingAndDrawableStateChanges() = inBothThemes { context ->
        val key = P_KEY_SCREEN_TIMEOUT
        val binding = Ubi4WidgetToggleSliderBinding.inflate(appCompatInflater(context))
        assertTrue(binding.toggleTurnOffBtnIv1 is AppCompatImageView)
        val delegate = ToggleSliderDelegateAdapterV3({}, setOf(key), {}, { false })
        val item = ToggleSliderItemV3("Screen timeout", ToggleSliderParameterWidgetSStruct(
            baseParameterWidgetSStruct = BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
                display = 2, parameterInfoSet = mutableSetOf(ParameterInfoRegistry.require(key)),
            )), minProgress = 1, maxProgress = 127, increment = 0.1f, unitLabel = "s",
        ))
        val state = ToggleSliderUiStateV3(V3ToggleSliderValue(30, true), 1..127, true)
        val bind = delegate.javaClass.getDeclaredMethod(
            "onBind", Ubi4WidgetToggleSliderBinding::class.java, ToggleSliderItemV3::class.java,
        ).apply { isAccessible = true }
        try {
            for (enabled in listOf(true, false, true)) {
                delegate.renderToggleSliders(mapOf(key to state.copy(value = state.value.copy(isEnabled = enabled))))
                bind.invoke(delegate, binding, item)
                val icon = binding.toggleTurnOffBtnIv1
                icon.isPressed = true
                icon.isPressed = false
                icon.refreshDrawableState()
                assertImageContains(icon, context.getColor(
                    if (enabled) R.color.ubi4_on_active else R.color.ubi4_control_disabled_text,
                ))
                assertImageContains(icon, context.getColor(
                    if (enabled) R.color.ubi4_active else R.color.ubi4_gray,
                ))
            }
        } finally { delegate.onDestroy() }
    }

    @Test fun spinnerArrowUsesTheThemeColorAfterSelectionChanges() = inBothThemes { context ->
        val spinner = Ubi4WidgetSpinnerBinding.inflate(appCompatInflater(context)).spinnerPsv
        val expected = context.getColor(R.color.ubi4_text_primary)
        spinner.setItems(listOf("First", "Second"))
        for (index in listOf(0, 1, 0)) {
            spinner.selectItemByIndex(index)
            assertEquals(expected, spinner.arrowTint)
            val arrow = requireNotNull(spinner.compoundDrawablesRelative.firstOrNull { it != null })
            assertDrawableContains(arrow, expected)
        }
    }

    private fun inBothThemes(check: (Context) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (mode in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                val config = Configuration(instrumentation.targetContext.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or mode
                }
                check(ContextThemeWrapper(instrumentation.targetContext.createConfigurationContext(config), R.style.Theme_UBI4))
            }
        }
    }

    private fun appCompatInflater(context: Context): LayoutInflater {
        val viewInflater = AppCompatViewInflater()
        return LayoutInflater.from(context).cloneInContext(context).apply {
            factory2 = object : LayoutInflater.Factory2 {
                override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? =
                    viewInflater.createView(parent, name, context, attrs, false, true, true, false)
                override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? =
                    onCreateView(null, name, context, attrs)
            }
        }
    }

    private fun assertImageContains(image: ImageView, expected: Int) {
        val size = (24 * image.resources.displayMetrics.density).toInt()
        val spec = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
        image.measure(spec, spec)
        image.layout(0, 0, size, size)
        assertBitmapContains(size, size, expected) { image.draw(it) }
    }

    private fun assertDrawableContains(drawable: Drawable, expected: Int) {
        val bounds = drawable.bounds
        assertBitmapContains(bounds.width(), bounds.height(), expected) {
            it.translate(-bounds.left.toFloat(), -bounds.top.toFloat())
            drawable.draw(it)
        }
    }

    private fun assertBitmapContains(width: Int, height: Int, expected: Int, draw: (Canvas) -> Unit) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            draw(Canvas(bitmap))
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            assertTrue("Control icon lost its theme/state color", pixels.any { it == expected })
        } finally { bitmap.recycle() }
    }
}
