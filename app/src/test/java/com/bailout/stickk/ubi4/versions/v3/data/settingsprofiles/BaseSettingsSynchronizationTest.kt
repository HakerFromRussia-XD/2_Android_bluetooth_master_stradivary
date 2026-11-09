package com.bailout.stickk.ubi4.versions.v3.data.settingsprofiles

import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileRepository
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.models.ble.SliderV3
import com.bailout.stickk.ubi4.models.ble.CurrentGestureV3
import com.bailout.stickk.ubi4.models.ble.GestureV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_SERIAL_NUMBER
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_FORCE_SETTINGS
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_CURRENT_GESTURE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_SETTING
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class BaseSettingsSynchronizationTest {
    private val dao = ProfileDao()
    private var repository = SettingsProfileRepository(dao)
    private val speed = ParameterInfoRegistry.require(P_KEY_SPEED_SETTINGS)
    private fun device(value: Int, serial: String = "serial") {
        ParameterStoreV3.put(ParameterInfoRegistry.require(P_KEY_SET_SERIAL_NUMBER), ParameterTypedValueV3.Text(serial))
        ParameterStoreV3.put(speed, ParameterTypedValueV3.Slider(SliderV3(value)))
    }
    private fun payload(value: Int, engineer: Boolean = false) = """{
      "BASE_SETTINGS":{"profile_id":0,"name":"Base","changed_by_service_engineer":$engineer,"engineer_revision":100,"updated_ts_ms":1,"settings":[
        {"target":"BLE","setting_key":"${speed.deviceAddress}:${speed.parameterID}:${speed.dataCode}:${speed.dataOffsets}","parameter_id":${speed.parameterID},"data_code":${speed.dataCode},"data_offset":${speed.dataOffsets},"device_address":${speed.deviceAddress},"codec_id":"SLIDER","value":{"sliderValue":$value}}
      ]},"PROFILE1":{"profile_id":{},"name":{},"settings":[]}
    }"""
    private suspend fun value(id: Int): Int {
        val values = repository.switchToProfile("serial", id).second
        return (values.single { it.parameterInfo == speed }.typedValue as ParameterTypedValueV3.Slider).value.sliderValue
    }
    @AfterEach fun tearDown() = ParameterStoreV3.clear()

    @Test fun `fresh offline device becomes baseline before any server access`() = runBlocking {
        device(80)
        repository.captureDeviceSettings("serial")
        assertEquals(listOf(0), repository.getProfiles("serial").map { it.profileId })
        assertEquals(0, repository.ensureState("serial").activeProfileId)
        assertEquals(80, value(0))
        assertEquals(0, repository.getPendingEngineerRevision("serial"))
    }

    @Test fun `different network baseline preserves and selects device snapshot in first free profile`() = runBlocking {
        device(80); repository.captureDeviceSettings("serial")
        val result = repository.synchronizeBaseSettings("serial", payload(20))
        assertEquals(1, result.first.activeProfileId)
        assertTrue(result.second.isEmpty())
        assertEquals(80, value(1))
        assertEquals(20, value(0))
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(80)), ParameterStoreV3.get(speed))
    }

    @Test fun `matching baseline does not create a personal profile`() = runBlocking {
        device(80); repository.captureDeviceSettings("serial")
        repository.synchronizeBaseSettings("serial", payload(80))
        assertEquals(listOf(0), repository.getProfiles("serial").map { it.profileId })
    }

    @Test fun `repeated synchronization reuses matching personal profile without duplicates`() = runBlocking {
        device(80); repository.captureDeviceSettings("serial")
        repeat(3) { repository.synchronizeBaseSettings("serial", payload(20)) }
        assertEquals(listOf(0, 1), repository.getProfiles("serial").map { it.profileId })
        assertEquals(1, repository.ensureState("serial").activeProfileId)
    }

    private suspend fun deleteSecondAndThirdProfiles() {
        device(80)
        repository.captureDeviceSettings("serial")
        repeat(3) { repository.createProfileFromActive("serial") }
        repository.switchToProfile("serial", 1)
        repository.deleteProfile("serial", 2)
        repository.deleteProfile("serial", 3)
        assertEquals(listOf(0, 1), repository.getProfiles("serial").map { it.profileId })
        ParameterStoreV3.clear()
        repository = SettingsProfileRepository(dao)
    }

    @Test fun `restart after deleting profiles does not recreate a slot when another setting is read`() = runBlocking {
        deleteSecondAndThirdProfiles()
        device(80)
        val force = ParameterInfoRegistry.require(P_KEY_FORCE_SETTINGS)
        ParameterStoreV3.put(force, ParameterTypedValueV3.Slider(SliderV3(35)))
        repeat(3) {
            repository.captureDeviceSettings("serial")
            repository.synchronizeBaseSettings("serial", payload(20))
        }
        assertEquals(listOf(0, 1), repository.getProfiles("serial").map { it.profileId })
        assertEquals(1, repository.ensureState("serial").activeProfileId)
        val values = repository.switchToProfile("serial", 1).second
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(35)), values.single { it.parameterInfo == force }.typedValue)
    }

    @Test fun `restart after deleting profiles does not recreate a slot for current gesture changes`() = runBlocking {
        val gesture = ParameterInfoRegistry.require(P_KEY_CURRENT_GESTURE)
        device(80)
        ParameterStoreV3.put(gesture, ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(1)))
        repository.captureDeviceSettings("serial")
        repeat(3) { repository.createProfileFromActive("serial") }
        repository.switchToProfile("serial", 1)
        repository.deleteProfile("serial", 2)
        repository.deleteProfile("serial", 3)
        ParameterStoreV3.clear()
        repository = SettingsProfileRepository(dao)
        device(80)
        ParameterStoreV3.put(gesture, ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(2)))
        repository.captureDeviceSettings("serial")
        repository.synchronizeBaseSettings("serial", payload(20))
        assertEquals(listOf(0, 1), repository.getProfiles("serial").map { it.profileId })
        assertEquals(1, repository.ensureState("serial").activeProfileId)
    }

    @Test fun `a genuinely changed known setting after restart still preserves the prosthesis settings`() = runBlocking {
        deleteSecondAndThirdProfiles()
        device(95)
        repository.captureDeviceSettings("serial")
        repository.synchronizeBaseSettings("serial", payload(20))
        assertEquals(listOf(0, 1, 2), repository.getProfiles("serial").map { it.profileId })
        assertEquals(2, repository.ensureState("serial").activeProfileId)
        assertEquals(80, value(1))
        assertEquals(95, value(2))
        assertEquals(20, value(0))
    }

    @Test fun `reading a different gesture after restart is not an edit of the saved gesture`() = runBlocking {
        val gesture = ParameterInfoRegistry.require(P_KEY_GESTURE_SETTING)
        device(80)
        ParameterStoreV3.put(gesture, ParameterTypedValueV3.GestureSettings(GestureV3(gestureId = 1, closePosition1 = 60)))
        repository.captureDeviceSettings("serial")
        repository.createProfileFromActive("serial")
        ParameterStoreV3.clear()
        repository = SettingsProfileRepository(dao)
        device(80)
        ParameterStoreV3.put(gesture, ParameterTypedValueV3.GestureSettings(GestureV3(gestureId = 2, closePosition1 = 90)))
        repository.captureDeviceSettings("serial")
        repository.synchronizeBaseSettings("serial", payload(20))
        assertEquals(listOf(0, 1), repository.getProfiles("serial").map { it.profileId })
        assertEquals(1, repository.ensureState("serial").activeProfileId)
    }

    @Test fun `changed settings of the same gesture are still preserved in a separate profile`() = runBlocking {
        val gesture = ParameterInfoRegistry.require(P_KEY_GESTURE_SETTING)
        device(80)
        ParameterStoreV3.put(gesture, ParameterTypedValueV3.GestureSettings(GestureV3(gestureId = 1, closePosition1 = 60)))
        repository.captureDeviceSettings("serial")
        repository.createProfileFromActive("serial")
        ParameterStoreV3.put(gesture, ParameterTypedValueV3.GestureSettings(GestureV3(gestureId = 1, closePosition1 = 90)))
        repository.captureDeviceSettings("serial")
        repository.synchronizeBaseSettings("serial", payload(20))
        assertEquals(listOf(0, 1, 2), repository.getProfiles("serial").map { it.profileId })
        assertEquals(2, repository.ensureState("serial").activeProfileId)
        val values = repository.switchToProfile("serial", 2).second
        assertEquals(90, (values.single { it.parameterInfo == gesture }.typedValue as ParameterTypedValueV3.GestureSettings).value.closePosition1)
    }

    @Test fun `partially known matching profiles keep the currently selected personal profile`() = runBlocking {
        device(80)
        repository.captureDeviceSettings("serial")
        repeat(3) { repository.createProfileFromActive("serial") }
        val force = ParameterInfoRegistry.require(P_KEY_FORCE_SETTINGS)
        ParameterStoreV3.put(force, ParameterTypedValueV3.Slider(SliderV3(35)))
        repository.captureDeviceSettings("serial")
        repository.synchronizeBaseSettings("serial", payload(20))
        assertEquals(3, repository.ensureState("serial").activeProfileId)
        assertEquals(listOf(0, 1, 2, 3), repository.getProfiles("serial").map { it.profileId })
    }

    @Test fun `a gesture notification alone cannot create a snapshot or a personal profile`() = runBlocking {
        ParameterStoreV3.put(ParameterInfoRegistry.require(P_KEY_SET_SERIAL_NUMBER), ParameterTypedValueV3.Text("serial"))
        ParameterStoreV3.put(ParameterInfoRegistry.require(P_KEY_CURRENT_GESTURE), ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(2)))
        repository.captureDeviceSettings("serial")
        assertTrue(dao.getProfiles("serial").isEmpty())
        repository.synchronizeBaseSettings("serial", payload(20))
        assertEquals(listOf(0), repository.getProfiles("serial").map { it.profileId })
    }

    @Test fun `full personal list overwrites first profile preserving its name`() = runBlocking {
        device(80); repository.captureDeviceSettings("serial")
        repeat(3) { repository.createProfileFromActive("serial") }
        for (id in 1..3) {
            repository.switchToProfile("serial", id)
            repository.saveBleValue("serial", speed, ParameterTypedValueV3.Slider(SliderV3(id * 10)), false)
        }
        repository.renameProfile("serial", 1, "My first")
        val result = repository.synchronizeBaseSettings("serial", payload(5))
        assertEquals(1, result.first.activeProfileId)
        assertEquals(80, value(1))
        assertEquals(20, value(2))
        assertEquals(30, value(3))
        assertEquals("My first", repository.getProfiles("serial").single { it.profileId == 1 }.customName)
    }

    @Test fun `existing matching profile wins even when all profiles are occupied`() = runBlocking {
        device(80); repository.captureDeviceSettings("serial")
        repeat(3) { repository.createProfileFromActive("serial") }
        repository.switchToProfile("serial", 1)
        repository.saveBleValue("serial", speed, ParameterTypedValueV3.Slider(SliderV3(10)), false)
        val result = repository.synchronizeBaseSettings("serial", payload(20))
        assertEquals(2, result.first.activeProfileId)
        assertEquals(10, value(1))
    }

    @Test fun `background sync preserves offline personal settings and replaces cached baseline despite timestamps`() = runBlocking {
        device(80); repository.captureDeviceSettings("serial")
        repository.createProfileFromActive("serial")
        repository.saveBleValue("serial", speed, ParameterTypedValueV3.Slider(SliderV3(55)), false)
        repository.renameProfile("serial", 1, "Offline")
        ParameterStoreV3.clear()
        val result = repository.synchronizeBaseSettings("serial", payload(20))
        assertEquals(1, result.first.activeProfileId)
        assertEquals("Offline", repository.getProfiles("serial").single { it.profileId == 1 }.customName)
        assertEquals(55, value(1))
        assertEquals(20, value(0))
    }

    @Test fun `pending engineer draft is preserved over server baseline`() = runBlocking {
        device(80); repository.captureDeviceSettings("serial")
        repository.saveBleValue("serial", speed, ParameterTypedValueV3.Slider(SliderV3(99)), true)
        val revision = repository.getPendingEngineerRevision("serial")
        repository.synchronizeBaseSettings("serial", payload(20))
        assertEquals(99, value(0))
        assertEquals(revision, repository.getPendingEngineerRevision("serial"))
    }

    @Test fun `another connected device cannot contaminate an offline snapshot`() = runBlocking {
        device(80); repository.captureDeviceSettings("serial")
        device(10, "other")
        repository.synchronizeBaseSettings("serial", payload(20))
        assertEquals(80, value(1))
    }
}
