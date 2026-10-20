package com.bailout.stickk.ubi4

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.ProgressBar
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HorizontalProgressStyleInstrumentedTest {
    @Test fun dialogsNeverLoadTheFrameworkIndeterminateDrawable() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.AppTheme)
            val inflater = LayoutInflater.from(context)
            val layouts = listOf(
                R.layout.ubi4_dialog_progressbar_firmware to R.id.loadingFirmwareProgressBar,
                R.layout.ubi4_dialog_progressbar to R.id.loadingProgressBar,
                R.layout.ubi4_dialog_sync_progress to R.id.sync_dialog_progress_pb,
                R.layout.dialog_progress_communication_test to R.id.pb_test_communication,
                R.layout.layout_updating_le to R.id.pb_update
            )
            layouts.forEach { (layout, id) ->
                val bar = inflater.inflate(layout, null).findViewById<ProgressBar>(id)
                assertFalse(bar.isIndeterminate)
                assertNotNull(bar.progressDrawable)
                assertNull(bar.indeterminateDrawable)
                bar.isIndeterminate = true
                assertNull(bar.indeterminateDrawable)
            }
        }
    }
}
