package com.bailout.stickk.ubi4.ui.gestures

import android.content.Context
import android.content.SharedPreferences
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.utility.CollectionGesturesProvider
import com.bailout.stickk.ubi4.versions.v3.data.appsettings.V3AppSettingsRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.GetCustomGestureNamesUseCaseV3
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class GestureCollectionFactoryTest {
    private val context = mockk<Context>()
    private val preferences = mockk<SharedPreferences>()
    private val stored = mutableMapOf<String, String?>()

    @BeforeEach fun setUp() {
        every { context.getString(any()) } answers { "resource:${firstArg<Int>()}" }
        every { context.getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE) } returns preferences
        every { preferences.getString(any(), any()) } answers {
            if (stored.containsKey(firstArg<String>())) stored[firstArg()] else secondArg<String?>()
        }
        mockkObject(WDApplication.Companion)
        every { WDApplication.applicationContext() } returns context
    }

    @AfterEach fun tearDown() { unmockkObject(WDApplication.Companion) }

    @Test fun `visual catalog preserves protocol order hidden gesture images and all fourteen names without storage`() {
        val indexes = mutableListOf<Int>()
        val items = GestureCollectionFactory.create(context) { index, fallback ->
            indexes.add(index)
            if (index == 0) "" else if (index == 13) "Saved last" else fallback
        }
        assertEquals((1..11).toList() + (13..15).toList() + (64..77).toList(), items.map { it.gestureId })
        assertEquals((0..13).toList(), indexes)
        val first = items.first()
        assertEquals("resource:${SharedRes.strings.fist.resourceId}", first.gestureName)
        assertEquals(SharedRes.images.collection_fist_1.drawableResId, first.gestureImage)
        assertEquals("resource:${SharedRes.strings.gesture_double_point.resourceId}", items[11].gestureName)
        assertEquals(SharedRes.images.collection_double_point.drawableResId, items[11].gestureImage)
        assertEquals("", items[14].gestureName)
        assertEquals("resource:${SharedRes.strings.gesture_2_btn.resourceId}", items[15].gestureName)
        assertEquals("Saved last", items.last().gestureName)
        verify(exactly = 0) { context.getSharedPreferences(any(), any()) }
        verify { preferences wasNot Called }
    }

    @Test fun `existing provider and V3 state resolve saved missing null and empty names identically`() {
        stored[PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4] = "AA:BB"
        val prefix = PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM + "AA:BB"
        stored[prefix + 0] = "Saved first"
        stored[prefix + 1] = ""
        stored[prefix + 2] = null
        stored[prefix + 13] = "Saved last"
        val names = GetCustomGestureNamesUseCaseV3(V3AppSettingsRepositoryImpl(preferences))()
        val fromState = GestureCollectionFactory.create(context) { index, fallback ->
            names.collectionNames[index] ?: fallback
        }
        assertEquals(fromState, CollectionGesturesProvider.getCollectionGestures())
        assertEquals("Saved first", CollectionGesturesProvider.getGesture(64).gestureName)
        assertEquals("", CollectionGesturesProvider.getGesture(65).gestureName)
        assertEquals("resource:${SharedRes.strings.gesture_3_btn.resourceId}", CollectionGesturesProvider.getGesture(66).gestureName)
        assertEquals("resource:${SharedRes.strings.gesture_4_btn.resourceId}", CollectionGesturesProvider.getGesture(67).gestureName)
        assertEquals(0, CollectionGesturesProvider.getGesture(12).gestureId)
        assertEquals(0, CollectionGesturesProvider.getGesture(999).gestureId)
        verify(exactly = 0) { preferences.edit() }
    }

    @Test fun `existing provider keeps empty MAC fallback and returns fresh mutable items`() {
        stored[PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM + "0"] = "No MAC"
        assertEquals("No MAC", CollectionGesturesProvider.getGesture(64).gestureName)
        stored[PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4] = null
        assertEquals("No MAC", CollectionGesturesProvider.getGesture(64).gestureName)
        val old = CollectionGesturesProvider.getCollectionGestures()
        old.first().gestureName = "Changed locally"
        assertEquals("resource:${SharedRes.strings.fist.resourceId}", CollectionGesturesProvider.getCollectionGestures().first().gestureName)
        verify(exactly = 0) { preferences.edit() }
    }
}
