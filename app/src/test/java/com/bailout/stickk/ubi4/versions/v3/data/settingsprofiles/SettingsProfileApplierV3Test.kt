package com.bailout.stickk.ubi4.versions.v3.data.settingsprofiles

import android.content.Context
import android.content.SharedPreferences
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileApplyValue
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.models.ble.ParameterCodecIdV3
import com.bailout.stickk.ubi4.models.ble.SliderV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.MobileSettingsKey
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS
import com.bailout.stickk.ubi4.versions.v3.di.createSettingsProfileValueApplier
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SettingsProfileApplierV3Test {
    private val info = ParameterInfoRegistry.require(P_KEY_SPEED_SETTINGS)
    private val preferences = mockk<SharedPreferences>()
    private val editor = mockk<SharedPreferences.Editor>()
    private lateinit var originalSubDevices: MutableSet<BaseSubDeviceInfoStruct>
    private lateinit var cachedParameter: BaseParameterInfoStruct
    private val speed = SettingsProfileApplyValue(
        "BLE", info, ParameterCodecIdV3.SLIDER, ParameterTypedValueV3.Slider(SliderV3(42)), null, null,
    )
    private val autoLogin = SettingsProfileApplyValue(
        "MOBILE", null, null, null, MobileSettingsKey.AUTO_LOGIN.key, true,
    )

    @BeforeEach
    fun setUp() {
        originalSubDevices = GlobalParameters.baseSubDevicesInfoStructSetV3
        cachedParameter = BaseParameterInfoStruct(ID = info.parameterID, dataCode = info.dataCode, data = "before")
        GlobalParameters.baseSubDevicesInfoStructSetV3 = mutableSetOf(BaseSubDeviceInfoStruct(
            deviceAddress = info.deviceAddress, parametersList = arrayListOf(cachedParameter),
        ))
        ParameterStoreV3.clear()
        every { preferences.edit() } returns editor
        every { editor.putBoolean(PreferenceKeysUbi4.SET_MODE_SMART_CONNECTION, any()) } returns editor
        every { editor.apply() } just Runs
    }

    @AfterEach
    fun tearDown() {
        ParameterStoreV3.clear()
        GlobalParameters.baseSubDevicesInfoStructSetV3 = originalSubDevices
    }

    @Test
    fun `mixed profile preserves value order and updates both caches before enqueuing`() {
        val events = mutableListOf<String>()
        every { editor.apply() } answers { events += "preferences" }
        val packets = mutableListOf<ByteArray>()
        val apply = SettingsProfileApplierV3({ packet ->
            assertEquals(speed.typedValue, ParameterStoreV3.get(info))
            assertEquals("{\"sliderValue\":42}", cachedParameter.data)
            events += "command"
            packets += packet
        }, preferences)

        apply.apply(listOf(autoLogin, speed, autoLogin.copy(mobileBoolean = false)))

        assertEquals(listOf("preferences", "command", "preferences"), events)
        assertArrayEquals(byteArrayOf(0x00, 0x0F, 0x3D, 0x2A, 0xA6.toByte()), packets.single())
        verifyOrder {
            editor.putBoolean(PreferenceKeysUbi4.SET_MODE_SMART_CONNECTION, true)
            editor.apply()
            editor.putBoolean(PreferenceKeysUbi4.SET_MODE_SMART_CONNECTION, false)
            editor.apply()
        }
        verify(exactly = 0) { editor.commit() }
    }

    @Test
    fun `queue failure propagates after cache update and does not apply later values`() {
        val failure = IllegalStateException("Queue unavailable")
        val applier = SettingsProfileApplierV3({ throw failure }, preferences)

        assertSame(failure, assertThrows(IllegalStateException::class.java) {
            applier.apply(listOf(speed, autoLogin))
        })
        assertEquals(speed.typedValue, ParameterStoreV3.get(info))
        assertEquals("{\"sliderValue\":42}", cachedParameter.data)
        verify { preferences wasNot Called }
    }

    @Test
    fun `unknown targets mobile keys and incomplete BLE values remain ignored`() {
        val enqueue = mockk<(ByteArray) -> Unit>()
        SettingsProfileApplierV3(enqueue, preferences).apply(listOf(
            speed.copy(target = "UNKNOWN"), speed.copy(parameterInfo = null),
            speed.copy(codecId = null), speed.copy(typedValue = null),
            autoLogin.copy(mobileKey = "UNKNOWN"), autoLogin.copy(mobileKey = null),
        ))
        assertNull(ParameterStoreV3.get(info))
        assertEquals("before", cachedParameter.data)
        verify { listOf(enqueue, preferences) wasNot Called }
    }

    @Test
    fun `composition uses application preferences and the supplied queue without an activity`() {
        val context = mockk<Context>()
        val application = mockk<Context>()
        every { context.applicationContext } returns application
        every { application.getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE) } returns preferences
        val packets = mutableListOf<ByteArray>()

        createSettingsProfileValueApplier(context, packets::add)(listOf(speed, autoLogin.copy(mobileBoolean = null)))

        assertArrayEquals(byteArrayOf(0x00, 0x0F, 0x3D, 0x2A, 0xA6.toByte()), packets.single())
        verify(exactly = 1) { editor.putBoolean(PreferenceKeysUbi4.SET_MODE_SMART_CONNECTION, false) }
        verify(exactly = 1) { editor.apply() }
        verify(exactly = 0) { context.getSharedPreferences(any(), any()) }
    }
}
