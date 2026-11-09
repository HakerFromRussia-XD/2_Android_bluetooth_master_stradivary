package com.bailout.stickk.ubi4.data.network

import SettingsModelV3
import Token
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileRepository
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileRepositoryProvider
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.models.ble.SliderV3
import com.bailout.stickk.ubi4.models.deviceList.DeviceInList_DEV
import com.bailout.stickk.ubi4.models.user.ClientDataV2
import com.bailout.stickk.ubi4.models.user.UserV2
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_SERIAL_NUMBER
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS
import com.bailout.stickk.ubi4.versions.v3.data.settingsprofiles.ProfileDao
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.V3BaseSettingsEnvironment
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class EngineerBaseSettingsRoundTripTest {
    private val api = mockk<Ubi4RequestsApi>()
    private val sender = Ubi4SettingsProfileSender(api) { "encrypted" }
    private val speed = ParameterInfoRegistry.require(P_KEY_SPEED_SETTINGS)
    private var server = ""
    private class Phone {
        val dao = ProfileDao()
        val storage = SettingsProfileRepository(dao)
    }

    @BeforeEach fun setUp() {
        ParameterStoreV3.clear()
        mockkObject(SettingsProfileRepositoryProvider)
        coEvery { api.getToken(any()) } returns NetworkResult.Success(Token("token"))
        coEvery { api.getUserInfoV2("token", "en") } returns NetworkResult.Success(UserV2(ClientDataV2(clientId = 7)))
        coEvery { api.getDevicesList(7, "token", "en") } returns NetworkResult.Success(listOf(DeviceInList_DEV(9, serialNumber = "serial")))
        coEvery { api.getProthesisSettingsRaw("9", "token") } answers {
            NetworkResult.Success("{\"settings\":${JsonPrimitive(server)}}")
        }
        coEvery { api.postProthesisSettings("9", "token", any()) } answers {
            server = requireNotNull(thirdArg<SettingsModelV3>().allOptions)
            NetworkResult.Success("saved")
        }
    }

    @AfterEach fun tearDown() { ParameterStoreV3.clear(); unmockkObject(SettingsProfileRepositoryProvider) }

    private fun connect(phone: Phone, deviceSpeed: Int) {
        every { SettingsProfileRepositoryProvider.getOrNull() } returns phone.storage
        ParameterStoreV3.clear()
        ParameterStoreV3.put(ParameterInfoRegistry.require(P_KEY_SET_SERIAL_NUMBER), ParameterTypedValueV3.Text("serial"))
        ParameterStoreV3.put(speed, ParameterTypedValueV3.Slider(SliderV3(deviceSpeed)))
    }

    private suspend fun seed(phone: Phone, value: Int) {
        connect(phone, value)
        phone.storage.captureDeviceSettings("serial")
    }

    private suspend fun saved(phone: Phone, profile: Int): Int = Json.parseToJsonElement(
        requireNotNull(phone.dao.getValues("serial", profile).single { it.parameter_id == speed.parameterID && it.data_code == speed.dataCode }.value_text)
    ).jsonObject.getValue("sliderValue").jsonPrimitive.int

    private suspend fun engineerPublishes(phone: Phone): Long {
        seed(phone, 10)
        phone.storage.createProfileFromActive("serial")
        phone.storage.saveBleValue("serial", speed, ParameterTypedValueV3.Slider(SliderV3(33)))
        phone.storage.switchToProfile("serial", 0)
        phone.storage.saveBleValue("serial", speed, ParameterTypedValueV3.Slider(SliderV3(20)), true)
        val revision = phone.storage.getPendingEngineerRevision("serial")
        sender.sendEngineerSettingsForSerial("serial", "en")
        assertTrue(revision > 0)
        assertEquals(0, phone.storage.getPendingEngineerRevision("serial"))
        return revision
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `engineer values replace user base while the previous user base is backed up and survives the return trip`(personalSelected: Boolean) = runTest {
        val user = Phone()
        seed(user, 80)
        if (personalSelected) {
            user.storage.createProfileFromActive("serial")
            user.storage.renameProfile("serial", 1, "Work")
            user.storage.saveBleValue("serial", speed, ParameterTypedValueV3.Slider(SliderV3(55)), false)
        }
        val engineer = Phone()
        val revision = engineerPublishes(engineer)
        connect(user, 20) // The engineer has also changed the physical prosthesis.
        sender.downloadProfileSettingsForSerial("serial", "en", preserveCurrentSettings = true)
        val backup = if (personalSelected) 2 else 1
        assertEquals(20, saved(user, 0))
        assertEquals(80, saved(user, backup))
        assertEquals(if (personalSelected) 1 else backup, user.storage.ensureState("serial").activeProfileId)
        if (personalSelected) { assertEquals(55, saved(user, 1)); assertEquals("Work", user.storage.getProfiles("serial").single { it.profileId == 1 }.customName) }
        assertEquals(revision, user.storage.getBaseSettingsRevision("serial"))
        assertNotNull(V3BaseSettingsEnvironment("serial", true, true, revision, 0).offer)
        val profiles = user.storage.getProfiles("serial")
        repeat(2) { sender.downloadProfileSettingsForSerial("serial", "en", true) }
        assertEquals(profiles, user.storage.getProfiles("serial"))
        sender.sendProfile1SettingsForSerial("serial", "en")
        // The user then chooses personal settings on the prosthesis. Returning to the
        // engineer phone must neither import those personal profiles nor change its baseline.
        connect(engineer, 55)
        sender.downloadProfileSettingsForSerial("serial", "en", preserveCurrentSettings = false)
        assertEquals(20, saved(engineer, 0))
        assertEquals(33, saved(engineer, 1))
        assertEquals(listOf(0, 1), engineer.storage.getProfiles("serial").map { it.profileId })
        assertEquals(0, engineer.storage.ensureState("serial").activeProfileId)
        assertEquals(revision, engineer.storage.getBaseSettingsRevision("serial"))
    }

    @Test fun `reconnecting the engineer never moves the prosthesis user values into engineer profiles`() = runTest {
        val engineer = Phone()
        engineerPublishes(engineer)
        connect(engineer, 55)
        sender.downloadProfileSettingsForSerial("serial", "en", preserveCurrentSettings = false)
        assertEquals(listOf(0, 1), engineer.storage.getProfiles("serial").map { it.profileId })
        assertEquals(0, engineer.storage.ensureState("serial").activeProfileId)
        assertEquals(20, saved(engineer, 0))
        assertEquals(33, saved(engineer, 1))
    }

    @Test fun `all personal slots occupied backs up the old user base into profile one keeping its name and current selection`() = runTest {
        val user = Phone()
        seed(user, 80)
        repeat(3) { user.storage.createProfileFromActive("serial") }
        for (id in 1..3) {
            user.storage.switchToProfile("serial", id)
            user.storage.saveBleValue("serial", speed, ParameterTypedValueV3.Slider(SliderV3(id * 10)), false)
        }
        user.storage.renameProfile("serial", 1, "Keep name")
        user.storage.switchToProfile("serial", 2)
        engineerPublishes(Phone())
        connect(user, 20)
        sender.downloadProfileSettingsForSerial("serial", "en", true)
        assertEquals(listOf(0, 1, 2, 3), user.storage.getProfiles("serial").map { it.profileId })
        assertEquals(20, saved(user, 0))
        assertEquals(80, saved(user, 1))
        assertEquals("Keep name", user.storage.getProfiles("serial").single { it.profileId == 1 }.customName)
        assertEquals(20, saved(user, 2))
        assertEquals(30, saved(user, 3))
        assertEquals(2, user.storage.ensureState("serial").activeProfileId)
    }

    @Test fun `matching old user base reuses its personal backup without selecting it over another personal profile`() = runTest {
        val user = Phone()
        seed(user, 80)
        repeat(3) { user.storage.createProfileFromActive("serial") }
        user.storage.saveBleValue("serial", speed, ParameterTypedValueV3.Slider(SliderV3(55)), false)
        val before = user.storage.getProfiles("serial")
        engineerPublishes(Phone())
        connect(user, 20)
        sender.downloadProfileSettingsForSerial("serial", "en", true)
        assertEquals(before, user.storage.getProfiles("serial"))
        assertEquals(3, user.storage.ensureState("serial").activeProfileId)
        assertEquals(20, saved(user, 0))
        assertEquals(80, saved(user, 1))
        assertEquals(55, saved(user, 3))
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `an engineer receiving a newer base on download or background upload does not create a user backup`(upload: Boolean) = runTest {
        val engineer = Phone()
        seed(engineer, 10)
        engineer.storage.createProfileFromActive("serial")
        engineer.storage.saveBleValue("serial", speed, ParameterTypedValueV3.Slider(SliderV3(33)))
        engineer.storage.switchToProfile("serial", 0)
        val before = engineer.storage.getProfiles("serial")
        val revision = engineerPublishes(Phone())
        connect(engineer, 55)
        if (upload) sender.sendEngineerSettingsForSerial("serial", "en")
        else sender.downloadProfileSettingsForSerial("serial", "en", false)
        assertEquals(before, engineer.storage.getProfiles("serial"))
        assertEquals(20, saved(engineer, 0))
        assertEquals(33, saved(engineer, 1))
        assertEquals(revision, engineer.storage.getBaseSettingsRevision("serial"))
    }

    @Test fun `offline reconnect leaves the old user base untouched until server data arrives`() = runTest {
        val user = Phone()
        seed(user, 80)
        connect(user, 20)
        coEvery { api.getToken(any()) } returns NetworkResult.Error(null, "offline")
        assertThrows(SettingsProfileRequestException::class.java) {
            kotlinx.coroutines.runBlocking { sender.downloadProfileSettingsForSerial("serial", "en", true) }
        }
        assertEquals(listOf(0), user.storage.getProfiles("serial").map { it.profileId })
        assertEquals(0, user.storage.ensureState("serial").activeProfileId)
        assertEquals(80, saved(user, 0))
        coVerify(exactly = 0) { api.getProthesisSettingsRaw(any(), any()) }
    }

    @Test fun `older server packet cannot roll back an engineer version or be republished over it`() = runTest {
        val user = Phone()
        seed(user, 80)
        val revision = engineerPublishes(Phone())
        connect(user, 20)
        sender.downloadProfileSettingsForSerial("serial", "en", true)
        val before = user.storage.getProfiles("serial")
        val root = Json.parseToJsonElement(server).jsonObject.toMutableMap()
        val base = Json.parseToJsonElement(root.getValue("BASE_SETTINGS").jsonPrimitive.content).jsonObject.toMutableMap()
        base["engineer_revision"] = JsonPrimitive(revision - 1)
        base["settings"] = buildJsonArray { add(buildJsonObject {
            put("target", "BLE"); put("setting_key", "${speed.deviceAddress}:${speed.parameterID}:${speed.dataCode}:${speed.dataOffsets}")
            put("parameter_id", speed.parameterID); put("data_code", speed.dataCode); put("data_offset", speed.dataOffsets)
            put("device_address", speed.deviceAddress); put("codec_id", "SLIDER")
            put("value", buildJsonObject { put("sliderValue", 99) })
        }) }
        root["BASE_SETTINGS"] = JsonPrimitive(JsonObject(base).toString())
        server = JsonObject(root).toString()
        sender.downloadProfileSettingsForSerial("serial", "en", true)
        assertEquals(before, user.storage.getProfiles("serial"))
        assertEquals(20, saved(user, 0))
        assertEquals(revision, user.storage.getBaseSettingsRevision("serial"))
        sender.sendProfile1SettingsForSerial("serial", "en")
        val uploaded = Json.parseToJsonElement(Json.parseToJsonElement(server).jsonObject.getValue("BASE_SETTINGS").jsonPrimitive.content).jsonObject
        assertEquals(revision, uploaded.getValue("engineer_revision").jsonPrimitive.long)
    }
}
