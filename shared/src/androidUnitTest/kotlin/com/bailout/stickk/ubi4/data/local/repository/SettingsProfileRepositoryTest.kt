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
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GLOBAL_THUMB_CLOSED_POSITION
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_DEVICE_NAME
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SETTINGS_PROFILE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
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
    fun `profile can be renamed and custom name is included in server payload`() = runBlocking {
        repository.ensureState("SERIAL-A")

        assertEquals(
            listOf(SettingsProfileInfo(profileId = 1, customName = null, isActive = true)),
            repository.getProfiles("SERIAL-A")
        )

        val renamed = requireNotNull(
            repository.renameProfile(
                serial = "SERIAL-A",
                profileId = 1,
                name = "  Everyday  "
            )
        )

        assertEquals(
            SettingsProfileInfo(profileId = 1, customName = "Everyday", isActive = true),
            renamed
        )
        assertEquals(listOf(renamed), repository.getProfiles("SERIAL-A"))

        val payload = repository.buildServerSettingsPayload("SERIAL-A")
        val profileJson = Json.parseToJsonElement(
            Json.parseToJsonElement(payload)
                .jsonObject
                .getValue("PROFILE1")
                .jsonPrimitive
                .content
        ).jsonObject
        assertEquals("Everyday", profileJson.getValue("name").jsonPrimitive.content)
    }

    @Test
    fun `profile rename rejects blank and too long names`() = runBlocking {
        repository.ensureState("SERIAL-A")

        assertNull(repository.renameProfile("SERIAL-A", 1, "   "))
        assertNull(repository.renameProfile("SERIAL-A", 4, "Invalid profile"))
        assertNull(
            repository.renameProfile(
                "SERIAL-A",
                1,
                "x".repeat(SETTINGS_PROFILE_NAME_MAX_LENGTH + 1)
            )
        )
        assertEquals(
            listOf(SettingsProfileInfo(profileId = 1, customName = null, isActive = true)),
            repository.getProfiles("SERIAL-A")
        )
    }

    @Test
    fun `migrateSerial moves profile values from temporary ble name to device serial`() = runBlocking {
        val speedInfo = ParameterInfoRegistry.require(P_KEY_SPEED_SETTINGS)

        repository.saveBleValue(
            serial = "FTHS3-00000",
            parameterInfo = speedInfo,
            typedValue = ParameterTypedValueV3.Slider(SliderV3(sliderValue = 42))
        )

        val state = repository.migrateSerial(
            oldSerial = "FTHS3-00000",
            newSerial = "FEST-H-12345"
        )
        val migratedValues = repository.switchToProfile("FEST-H-12345", 1).second
        val oldValues = repository.switchToProfile("FTHS3-00000", 1).second

        assertEquals(SettingsProfileState(profileCount = 1, activeProfileId = 1), state)
        assertEquals(42, migratedValues.sliderValue())
        assertTrue(oldValues.isEmpty())
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
    fun `server settings payload wraps all profiles as json strings`() = runBlocking {
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

        repository.createProfileFromActive("SERIAL-A")
        repository.saveBleValue(
            serial = "SERIAL-A",
            parameterInfo = speedInfo,
            typedValue = ParameterTypedValueV3.Slider(SliderV3(sliderValue = 20))
        )
        repository.saveMobileBoolean(
            serial = "SERIAL-A",
            mobileKey = MobileSettingsKey.AUTO_LOGIN.key,
            value = false
        )

        repository.createProfileFromActive("SERIAL-A")
        repository.saveBleValue(
            serial = "SERIAL-A",
            parameterInfo = speedInfo,
            typedValue = ParameterTypedValueV3.Slider(SliderV3(sliderValue = 30))
        )

        val payload = repository.buildServerSettingsPayload("SERIAL-A")
        val profiles = Json.parseToJsonElement(payload).jsonObject
        val profile1 = Json.parseToJsonElement(profiles.getValue("PROFILE1").jsonPrimitive.content).jsonObject
        val profile2 = Json.parseToJsonElement(profiles.getValue("PROFILE2").jsonPrimitive.content).jsonObject
        val profile3 = Json.parseToJsonElement(profiles.getValue("PROFILE3").jsonPrimitive.content).jsonObject
        val profile1Settings = profile1.getValue("settings").jsonArray
        val profile2Settings = profile2.getValue("settings").jsonArray
        val profile3Settings = profile3.getValue("settings").jsonArray
        val profile1Ble = profile1Settings.first { it.jsonObject.getValue("target").jsonPrimitive.content == "BLE" }.jsonObject
        val profile1Mobile = profile1Settings.first { it.jsonObject.getValue("target").jsonPrimitive.content == "MOBILE" }.jsonObject
        val profile2Ble = profile2Settings.first { it.jsonObject.getValue("target").jsonPrimitive.content == "BLE" }.jsonObject
        val profile2Mobile = profile2Settings.first { it.jsonObject.getValue("target").jsonPrimitive.content == "MOBILE" }.jsonObject
        val profile3Ble = profile3Settings.first { it.jsonObject.getValue("target").jsonPrimitive.content == "BLE" }.jsonObject
        val profile3Mobile = profile3Settings.first { it.jsonObject.getValue("target").jsonPrimitive.content == "MOBILE" }.jsonObject

        assertEquals(1, profile1.getValue("profile_id").jsonPrimitive.int)
        assertEquals(2, profile2.getValue("profile_id").jsonPrimitive.int)
        assertEquals(3, profile3.getValue("profile_id").jsonPrimitive.int)
        assertEquals("Профиль №1", profile1.getValue("name").jsonPrimitive.content)
        assertEquals("Профиль №2", profile2.getValue("name").jsonPrimitive.content)
        assertEquals("Профиль №3", profile3.getValue("name").jsonPrimitive.content)
        assertEquals("SLIDER", profile1Ble.getValue("codec_id").jsonPrimitive.content)
        assertEquals(10, profile1Ble.getValue("value").jsonObject.getValue("sliderValue").jsonPrimitive.int)
        assertEquals(20, profile2Ble.getValue("value").jsonObject.getValue("sliderValue").jsonPrimitive.int)
        assertEquals(30, profile3Ble.getValue("value").jsonObject.getValue("sliderValue").jsonPrimitive.int)
        assertEquals("mobile:${MobileSettingsKey.AUTO_LOGIN.key}", profile1Mobile.getValue("setting_key").jsonPrimitive.content)
        assertEquals(true, profile1Mobile.getValue("value").jsonPrimitive.boolean)
        assertEquals(false, profile2Mobile.getValue("value").jsonPrimitive.boolean)
        assertEquals(false, profile3Mobile.getValue("value").jsonPrimitive.boolean)
    }

    @Test
    fun `server settings payload imports profiles back to local database`() = runBlocking {
        val speedInfo = ParameterInfoRegistry.require(P_KEY_SPEED_SETTINGS)

        repository.saveBleValue(
            serial = "SERIAL-A",
            parameterInfo = speedInfo,
            typedValue = ParameterTypedValueV3.Slider(SliderV3(sliderValue = 10))
        )
        repository.createProfileFromActive("SERIAL-A")
        repository.saveBleValue(
            serial = "SERIAL-A",
            parameterInfo = speedInfo,
            typedValue = ParameterTypedValueV3.Slider(SliderV3(sliderValue = 20))
        )
        repository.createProfileFromActive("SERIAL-A")
        repository.saveBleValue(
            serial = "SERIAL-A",
            parameterInfo = speedInfo,
            typedValue = ParameterTypedValueV3.Slider(SliderV3(sliderValue = 30))
        )

        val payload = repository.buildServerSettingsPayload("SERIAL-A")
        val importedRepository = SettingsProfileRepository(InMemorySettingsProfileDao())
        val imported = importedRepository.importServerSettingsPayload("SERIAL-B", payload)

        assertEquals(SettingsProfileState(profileCount = 3, activeProfileId = 3), imported.first)
        assertEquals(30, imported.second.sliderValue())
        assertEquals(10, importedRepository.switchToProfile("SERIAL-B", 1).second.sliderValue())
        assertEquals(20, importedRepository.switchToProfile("SERIAL-B", 2).second.sliderValue())
        assertEquals(30, importedRepository.switchToProfile("SERIAL-B", 3).second.sliderValue())
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

    @Test
    fun `global finger positions round trip independently through settings profiles`() = runBlocking {
        val thumbInfo = ParameterInfoRegistry.require(P_KEY_GLOBAL_THUMB_CLOSED_POSITION)
        val indexMiddleInfo = ParameterInfoRegistry.require(P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION)

        repository.saveBleValue(
            serial = "SERIAL-A",
            parameterInfo = thumbInfo,
            typedValue = ParameterTypedValueV3.Slider(SliderV3(sliderValue = 15))
        )
        repository.saveBleValue(
            serial = "SERIAL-A",
            parameterInfo = indexMiddleInfo,
            typedValue = ParameterTypedValueV3.Slider(SliderV3(sliderValue = 35))
        )

        repository.createProfileFromActive("SERIAL-A")
        repository.saveBleValue(
            serial = "SERIAL-A",
            parameterInfo = thumbInfo,
            typedValue = ParameterTypedValueV3.Slider(SliderV3(sliderValue = 55))
        )
        repository.saveBleValue(
            serial = "SERIAL-A",
            parameterInfo = indexMiddleInfo,
            typedValue = ParameterTypedValueV3.Slider(SliderV3(sliderValue = 75))
        )

        val profile1 = repository.switchToProfile("SERIAL-A", 1).second
        val profile2 = repository.switchToProfile("SERIAL-A", 2).second

        assertEquals(15, profile1.sliderValue(thumbInfo))
        assertEquals(35, profile1.sliderValue(indexMiddleInfo))
        assertEquals(55, profile2.sliderValue(thumbInfo))
        assertEquals(75, profile2.sliderValue(indexMiddleInfo))
        assertEquals(thumbInfo, profile2.single { it.parameterInfo == thumbInfo }.parameterInfo)
        assertEquals(
            indexMiddleInfo,
            profile2.single { it.parameterInfo == indexMiddleInfo }.parameterInfo
        )
    }
}

private fun List<SettingsProfileApplyValue>.sliderValue(): Int? =
    filter { it.target == "BLE" }
        .mapNotNull { (it.typedValue as? ParameterTypedValueV3.Slider)?.value?.sliderValue }
        .firstOrNull()

private fun List<SettingsProfileApplyValue>.sliderValue(
    parameterInfo: com.bailout.stickk.ubi4.models.commonModels.ParameterInfo<Int, Int, Int, Int>
): Int? =
    firstOrNull { it.parameterInfo == parameterInfo }
        ?.typedValue
        ?.let { it as? ParameterTypedValueV3.Slider }
        ?.value
        ?.sliderValue

private fun List<SettingsProfileApplyValue>.mobileBoolean(key: String): Boolean? =
    firstOrNull { it.target == "MOBILE" && it.mobileKey == key }?.mobileBoolean

private fun List<SettingsProfileApplyValue>.currentGesture(): Int? =
    mapNotNull { (it.typedValue as? ParameterTypedValueV3.CurrentGesture)?.value?.currentGesture }
        .firstOrNull()

private fun List<SettingsProfileApplyValue>.rotationGroup(): RotationGroupV3 =
    mapNotNull { (it.typedValue as? ParameterTypedValueV3.RotationGroup)?.value }
        .first()
