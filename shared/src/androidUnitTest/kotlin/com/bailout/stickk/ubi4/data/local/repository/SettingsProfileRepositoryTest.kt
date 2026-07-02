package com.bailout.stickk.ubi4.data.local.repository

import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.models.ble.CurrentGestureV3
import com.bailout.stickk.ubi4.models.ble.GestureV3
import com.bailout.stickk.ubi4.models.ble.RotationGroupV3
import com.bailout.stickk.ubi4.models.ble.SliderV3
import com.bailout.stickk.ubi4.models.ble.SpinnerV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.MobileSettingsKey
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.testing.InMemorySettingsProfileDao
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_CURRENT_GESTURE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_GROUPE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_SETTING
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_DEVICE_NAME
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SETTINGS_PROFILE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SettingsProfileRepositoryTest {
    private lateinit var dao: InMemorySettingsProfileDao
    private lateinit var repository: SettingsProfileRepository

    @Before
    fun setUp() {
        ParameterStoreV3.clear()
        dao = InMemorySettingsProfileDao()
        repository = SettingsProfileRepository(dao)
    }

    @After
    fun tearDown() {
        ParameterStoreV3.clear()
    }

    @Test
    fun `ensureState creates first profile per serial and keeps devices isolated`() = runBlocking {
        val speedInfo = ParameterInfoRegistry.require(P_KEY_SPEED_SETTINGS)

        assertEquals(SettingsProfileState(profileCount = 1, activeProfileId = 1), repository.ensureState("SERIAL-A"))
        repository.saveBleValue(
            serial = "SERIAL-A",
            parameterInfo = speedInfo,
            typedValue = ParameterTypedValueV3.Slider(SliderV3(sliderValue = 42))
        )

        assertEquals(SettingsProfileState(profileCount = 1, activeProfileId = 1), repository.ensureState("SERIAL-B"))

        val valuesA = repository.switchToProfile("SERIAL-A", 1).second
        val valuesB = repository.switchToProfile("SERIAL-B", 1).second

        assertEquals(1, valuesA.size)
        assertEquals(42, valuesA.sliderValue())
        assertTrue(valuesB.isEmpty())
    }

    @Test
    fun `new profile copies active profile and switching returns selected profile values`() = runBlocking {
        val speedInfo = ParameterInfoRegistry.require(P_KEY_SPEED_SETTINGS)

        repository.saveBleValue(
            serial = "SERIAL-A",
            parameterInfo = speedInfo,
            typedValue = ParameterTypedValueV3.Slider(SliderV3(sliderValue = 10))
        )
        repository.saveMobileBoolean(
            serial = "SERIAL-A",
            mobileKey = MobileSettingsKey.AUTO_LOGIN.key,
            value = true
        )

        val createdProfile2 = repository.createProfileFromActive("SERIAL-A")
        assertEquals(SettingsProfileState(profileCount = 2, activeProfileId = 2), createdProfile2.first)
        assertEquals(10, createdProfile2.second.sliderValue())
        assertEquals(true, createdProfile2.second.mobileBoolean(MobileSettingsKey.AUTO_LOGIN.key))

        repository.saveBleValue(
            serial = "SERIAL-A",
            parameterInfo = speedInfo,
            typedValue = ParameterTypedValueV3.Slider(SliderV3(sliderValue = 20))
        )

        val profile1 = repository.switchToProfile("SERIAL-A", 1)
        assertEquals(SettingsProfileState(profileCount = 2, activeProfileId = 1), profile1.first)
        assertEquals(10, profile1.second.sliderValue())

        val profile2 = repository.switchToProfile("SERIAL-A", 2)
        assertEquals(SettingsProfileState(profileCount = 2, activeProfileId = 2), profile2.first)
        assertEquals(20, profile2.second.sliderValue())

        val profile3 = repository.createProfileFromActive("SERIAL-A")
        assertEquals(SettingsProfileState(profileCount = 3, activeProfileId = 3), profile3.first)
        assertEquals(20, profile3.second.sliderValue())

        val overLimit = repository.createProfileFromActive("SERIAL-A")
        assertEquals(SettingsProfileState(profileCount = 3, activeProfileId = 3), overLimit.first)
    }

    @Test
    fun `system profile spinner and text inputs are not saved as profile BLE settings`() = runBlocking {
        repository.saveBleValue(
            serial = "SERIAL-A",
            parameterInfo = ParameterInfoRegistry.require(P_KEY_SETTINGS_PROFILE),
            typedValue = ParameterTypedValueV3.Spinner(SpinnerV3(spinnerValue = 1))
        )
        repository.saveBleValue(
            serial = "SERIAL-A",
            parameterInfo = ParameterInfoRegistry.require(P_KEY_SET_DEVICE_NAME),
            typedValue = ParameterTypedValueV3.Text("New name")
        )

        val values = repository.switchToProfile("SERIAL-A", 1).second

        assertTrue(values.isEmpty())
    }

    @Test
    fun `active gesture rotation group and finger positions are saved and restored`() = runBlocking {
        val currentGestureInfo = ParameterInfoRegistry.require(P_KEY_CURRENT_GESTURE)
        val rotationGroupInfo = ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE)
        val gestureSettingsBaseInfo = ParameterInfoRegistry.require(P_KEY_GESTURE_SETTING)
        val gesture72Info = gestureSettingsBaseInfo.copy(dataOffsets = 72)
        val gesture73Info = gestureSettingsBaseInfo.copy(dataOffsets = 73)

        repository.saveBleValue(
            serial = "SERIAL-A",
            parameterInfo = currentGestureInfo,
            typedValue = ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(currentGesture = 72))
        )
        repository.saveBleValue(
            serial = "SERIAL-A",
            parameterInfo = rotationGroupInfo,
            typedValue = ParameterTypedValueV3.RotationGroup(
                RotationGroupV3(
                    gesture1Id = 72,
                    gesture1ImageId = 72,
                    gesture2Id = 73,
                    gesture2ImageId = 73
                )
            )
        )
        repository.saveBleValue(
            serial = "SERIAL-A",
            parameterInfo = gesture72Info,
            typedValue = ParameterTypedValueV3.GestureSettings(
                GestureV3(gestureId = 72, openPosition1 = 11, closePosition1 = 21)
            )
        )
        repository.saveBleValue(
            serial = "SERIAL-A",
            parameterInfo = gesture73Info,
            typedValue = ParameterTypedValueV3.GestureSettings(
                GestureV3(gestureId = 73, openPosition1 = 12, closePosition1 = 22)
            )
        )

        val values = repository.switchToProfile("SERIAL-A", 1).second

        assertEquals(72, values.currentGesture())
        val rotationGroup = values.rotationGroup()
        assertEquals(72, rotationGroup.gesture1Id)
        assertEquals(73, rotationGroup.gesture2Id)

        val gestureSettings = values
            .filter { it.typedValue is ParameterTypedValueV3.GestureSettings }
            .associateBy { it.parameterInfo?.dataOffsets }

        val gesture72 = assertIs<ParameterTypedValueV3.GestureSettings>(gestureSettings[72]?.typedValue).value
        val gesture73 = assertIs<ParameterTypedValueV3.GestureSettings>(gestureSettings[73]?.typedValue).value
        assertEquals(11, gesture72.openPosition1)
        assertEquals(21, gesture72.closePosition1)
        assertEquals(12, gesture73.openPosition1)
        assertEquals(22, gesture73.closePosition1)
    }
}

private fun List<SettingsProfileApplyValue>.sliderValue(): Int? =
    filter { it.target == "BLE" }
        .mapNotNull { (it.typedValue as? ParameterTypedValueV3.Slider)?.value?.sliderValue }
        .firstOrNull()

private fun List<SettingsProfileApplyValue>.mobileBoolean(key: String): Boolean? =
    firstOrNull { it.target == "MOBILE" && it.mobileKey == key }?.mobileBoolean

private fun List<SettingsProfileApplyValue>.currentGesture(): Int? =
    mapNotNull { (it.typedValue as? ParameterTypedValueV3.CurrentGesture)?.value?.currentGesture }
        .firstOrNull()

private fun List<SettingsProfileApplyValue>.rotationGroup(): RotationGroupV3 =
    mapNotNull { (it.typedValue as? ParameterTypedValueV3.RotationGroup)?.value }
        .first()
