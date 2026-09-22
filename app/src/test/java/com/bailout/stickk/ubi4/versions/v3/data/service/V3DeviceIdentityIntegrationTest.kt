package com.bailout.stickk.ubi4.versions.v3.data.service

import android.content.Intent
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileManager
import com.bailout.stickk.ubi4.data.state.ConnectionState
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.di.BleDependencies
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.versions.v3.data.main.V3MainRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.domain.main.*
import com.bailout.stickk.ubi4.versions.v3.domain.service.*
import com.bailout.stickk.ubi4.versions.v3.presentation.main.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class V3DeviceIdentityIntegrationTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val scope = ViewModelStore()
    private val owner = object : ViewModelStoreOwner { override val viewModelStore = scope }
    private val identity = BleDependencies.deviceIdentity(owner)
    private val originalName = runCatching { ConnectionState.connectedDeviceName }.getOrDefault("")
    private val originalAddress = runCatching { ConnectionState.connectedDeviceAddress }.getOrDefault("")
    private val originalProfile = UiState.activeV3DeviceProfile
    private val originalEnabled = UiState.isInterfaceV3Activated
    private val originalInteraction = UiState.v3WidgetsInteractionEnabled.value
    private val widgets = UiState.listWidgets
    private val mainRepository = V3MainRepositoryImpl(identity, mockk<Context> {
        every { applicationContext } returns mockk()
    }, mockk<SharedPreferences> {
        every { edit() } returns mockk(relaxed = true) {
            every { putString(any(), any()) } answers { self as SharedPreferences.Editor }
        }
    })
    private val vm = V3MainViewModel(GetMainVisibleDisplaysUseCaseV3(mainRepository),
        ObserveMainUpdatesUseCaseV3(mainRepository), UpdateMainDeviceIdentityUseCaseV3(mainRepository),
        ScheduleProfileUploadUseCaseV3(mainRepository), ManageMainConnectionUseCaseV3(mainRepository))

    @BeforeEach fun setup() {
        Dispatchers.setMain(dispatcher)
        scope.put("main", vm)
        UiState.isInterfaceV3Activated = true
        UiState.activeV3DeviceProfile = V3DeviceProfile.STANDARD_V3
        UiState.v3WidgetsInteractionEnabled.value = true
        UiState.listWidgets = mutableSetOf()
        ConnectionState.connectedDeviceName = "FTHS3-00001"
        ConnectionState.connectedDeviceAddress = "AA:BB:CC:DD:EE:01"
        vm.onAction(V3MainAction.DeviceConnected)
        vm.onAction(V3MainAction.ViewCreated)
    }

    @AfterEach fun cleanup() {
        scope.clear()
        Dispatchers.resetMain()
        ConnectionState.connectedDeviceName = originalName
        ConnectionState.connectedDeviceAddress = originalAddress
        UiState.activeV3DeviceProfile = originalProfile
        UiState.isInterfaceV3Activated = originalEnabled
        UiState.v3WidgetsInteractionEnabled.value = originalInteraction
        UiState.listWidgets = widgets
    }

    @Test fun `service rename updates observed header before acknowledgment without another command or profile write`() {
        val beforeSerial = SettingsProfileManager.serial()
        val beforeRevision = vm.uiState.value.navigationRevision
        val packets = mutableListOf<ByteArray>()
        lateinit var acknowledge: () -> Unit
        val service = V3DeviceInfoRepositoryImpl(identity, enqueuePacket = { packet, sent ->
            // Keep the old order: enqueue first, then publish the local name.
            assertEquals("00001", vm.uiState.value.deviceIdentity?.displayName)
            packets += packet; acknowledge = sent
        }, recordNameCustomization = {}, currentDeviceAddress = { "device" })
        assertEquals(V3DeviceInfoWriteResult.SENT, SetDeviceInfoTextUseCaseV3(service)(V3DeviceInfoField.DEVICE_NAME, "New"))
        assertEquals("New", vm.uiState.value.deviceIdentity?.displayName)
        assertEquals("FTHS3-New", vm.uiState.value.deviceIdentity?.serial)
        assertEquals("FTHS3-New", vm.uiState.value.deviceIdentity?.deviceName)
        assertEquals("New", service.getTextForInput(V3DeviceInfoField.DEVICE_NAME))
        assertEquals("FTHS3-New", ConnectionState.connectedDeviceName)
        assertEquals(beforeRevision, vm.uiState.value.navigationRevision)
        assertEquals(beforeSerial, SettingsProfileManager.serial())
        acknowledge(); acknowledge()
        vm.onAction(V3MainAction.NavigationRefreshRequested)
        assertEquals(1, packets.size)
    }

    @Test fun `blank names are ignored and detached observer resumes latest name without resetting it`() {
        val before = vm.uiState.value.deviceIdentity
        for (name in listOf("", " ", "\n")) identity.applyDeviceName(name)
        assertEquals(before, identity.identity.value)
        assertEquals("FTHS3-00001", ConnectionState.connectedDeviceName)
        vm.onAction(V3MainAction.ViewDestroyed)
        identity.applyDeviceName(" FTHS3-other ")
        assertEquals(before, vm.uiState.value.deviceIdentity)
        vm.onAction(V3MainAction.ViewCreated)
        assertEquals("other", vm.uiState.value.deviceIdentity?.displayName)
        assertEquals(" FTHS3-other ", vm.uiState.value.deviceIdentity?.serial)
        scope.clear()
        val last = vm.uiState.value
        identity.applyDeviceName("FTHS3-late")
        assertEquals(last, vm.uiState.value)
    }

    @Test fun `composition shares identity through recreation but isolates other Activity scopes and tracks new intent fallback`() {
        val recreated = object : ViewModelStoreOwner { override val viewModelStore = scope }
        assertSame(identity, BleDependencies.deviceIdentity(recreated))
        val otherScope = ViewModelStore()
        val otherOwner = object : ViewModelStoreOwner { override val viewModelStore = otherScope }
        assertNotSame(identity, BleDependencies.deviceIdentity(otherOwner))
        val intent = mockk<Intent>()
        every { intent.getStringExtra(any()) } returns "new-intent"
        val before = identity.identity.value
        BleDependencies.updateLaunchIntent(recreated, intent)
        assertEquals("new-intent", identity.intentDeviceName)
        assertEquals(before, identity.identity.value)
        assertNull(BleDependencies.deviceIdentity(otherOwner).identity.value)
        assertNull(BleDependencies.deviceIdentity(otherOwner).intentDeviceName)
        UiState.isInterfaceV3Activated = false
        every { intent.getStringExtra(any()) } returns "UBI4"
        BleDependencies.updateLaunchIntent(recreated, intent)
        assertEquals("new-intent", identity.intentDeviceName)
        otherScope.clear()
    }
}
