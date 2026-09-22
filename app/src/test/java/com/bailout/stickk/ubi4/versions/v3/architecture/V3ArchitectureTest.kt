package com.bailout.stickk.ubi4.versions.v3.architecture

import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3AppSettingKeys
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ParameterKeys
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.MobileSettingsKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** Dependency guards for the agreed V3 architecture, run with the ordinary app unit tests. */
class V3ArchitectureTest {
    private val prefix = "com.bailout.stickk.ubi4.versions.v3."
    private val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
        .map { File(it, "app/src/main/java/com/bailout/stickk/ubi4/versions/v3") }
        .first { it.isDirectory }
    private fun sources(layer: String) = File(root, layer).walkTopDown().filter { it.extension == "kt" }.toList()
        .also { assertTrue(it.isNotEmpty(), "Missing source files for $layer") }
    private fun imports(file: File) = file.readLines().filter { it.startsWith("import ") }
        .map { it.removePrefix("import ").substringBefore(" as ") }
    private fun reject(file: File, bad: List<String>) = assertTrue(bad.isEmpty(),
        "${file.relativeTo(root)} has forbidden dependencies: $bad")

    @Test fun `domain is independent of platform storage presentation and composition`() {
        sources("domain").forEach { file ->
            reject(file, imports(file).filterNot {
                it.startsWith(prefix + "domain.") || it.startsWith("kotlin.") ||
                    it.startsWith("kotlinx.coroutines.") ||
                    // The shared enum is a pure KMM device model, not an Android or storage type.
                    it == "com.bailout.stickk.ubi4.models.device.V3DeviceProfile"
            })
        }
    }

    @Test fun `data never depends on presentation or dependency factories`() {
        sources("data").forEach { file ->
            reject(file, imports(file).filter {
                it.startsWith(prefix + "presentation.") || it.startsWith(prefix + "di.") ||
                    it.startsWith("com.bailout.stickk.ubi4.di.") || it.startsWith("com.bailout.stickk.ubi4.ui.") ||
                    it.startsWith("androidx.lifecycle.") || it.startsWith("android.view.")
            })
        }
    }

    @Test fun `presentation does not hold repositories or build use cases`() {
        val repositories = sources("domain").filter { it.name.endsWith("Repository.kt") }.map { it.nameWithoutExtension }
        sources("presentation").forEach { file ->
            val code = file.readText()
            reject(file, repositories.filter { Regex("\\b$it\\b").containsMatchIn(code) })
            // This screen obtains its ViewModel from DI; it does not assemble domain/data dependencies.
            val screenFactory = when (file.relativeTo(root).invariantSeparatorsPath) {
                "presentation/accountstatistics/AccountFragmentStatisticsV3.kt" -> prefix + "di.V3AccountStatisticsViewModelFactory"
                else -> null
            }
            reject(file, imports(file).filter { it.startsWith(prefix + "data.") ||
                (it.startsWith(prefix + "di.") && it != screenFactory) ||
                it.startsWith("com.bailout.stickk.ubi4.di.") })
            assertTrue(!Regex("\\b[A-Z]\\w*UseCaseV3\\s*\\(").containsMatchIn(code),
                "${file.name} constructs a UseCase; assemble it in di")
        }
    }

    @Test fun `only viewmodels call use cases in presentation`() {
        sources("presentation").filterNot { it.name.endsWith("ViewModel.kt") }.forEach { file ->
            assertTrue(!Regex("\\b\\w*UseCaseV3\\b").containsMatchIn(file.readText()),
                "${file.name} depends on a UseCase; delegate the domain operation to its screen ViewModel")
        }
    }

    @Test fun `viewmodels expose one action entry point and readonly screen state`() {
        sources("presentation").filter { it.name.endsWith("ViewModel.kt") }.forEach { file ->
            val code = file.readText()
            reject(file, imports(file).filter {
                it.startsWith("android.") || it.contains(".ubi4.data.") ||
                    it.contains(".ubi4.persistence.") || it.contains(".ubi4.ble.") || it.contains(".ubi4.ui.")
            })
            val publicFunctions = Regex("(?m)^    fun (\\w+)\\(").findAll(code).map { it.groupValues[1] }.toList()
            assertEquals(listOf("onAction"), publicFunctions, file.name)
            assertTrue(code.contains("val uiState =") && !Regex("(?m)^    (?:val|var) \\w+.*Mutable(?:State|Shared)Flow").containsMatchIn(code),
                "${file.name} must expose read-only UiState")
        }
    }

    @Test fun `UI state and actions contain values without infrastructure or mutable streams`() {
        sources("presentation").filter { it.name.endsWith("UiState.kt") || it.name.endsWith("Action.kt") }.forEach { file ->
            reject(file, imports(file).filter {
                it.startsWith("android.") || it.startsWith("androidx.") || it.contains(".data.") ||
                    it.contains(".persistence.") || it.contains(".ble.") || it.contains(".di.")
            })
            assertTrue(!Regex("\\b(?:var|MutableStateFlow|MutableSharedFlow|MutableList|MutableMap|MutableSet)\\b")
                .containsMatchIn(file.readText()), "${file.name} exposes mutable UI state")
        }
    }

    @Test fun `shared UI compatibility is restricted to existing widget composition adapters`() {
        val compatibilityFiles = setOf(
            "DataFactoryV3SensorsWidgetsSource.kt", "DataFactoryV3ServiceWidgetsSource.kt",
            "DataFactoryV3SpecialSettingsWidgetsSource.kt", "V3SensorsWidgetMapper.kt",
            "V3ServiceWidgetMapper.kt", "V3SpecialSettingsWidgetMapper.kt",
            "DataFactoryV3GesturesWidgetsSource.kt", "V3GesturesWidgetMapper.kt",
        )
        sources("presentation").forEach { file ->
            reject(file, imports(file).filter { dependency ->
                when {
                    dependency == "com.bailout.stickk.ubi4.data.DataFactory" -> file.name !in compatibilityFiles
                    dependency.startsWith("com.bailout.stickk.ubi4.data.widget.") -> file.name !in compatibilityFiles
                    dependency.startsWith("com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.") -> file.name !in compatibilityFiles
                    dependency.startsWith("com.bailout.stickk.ubi4.data.") || dependency.startsWith("com.bailout.stickk.ubi4.ble.") ||
                        dependency.startsWith("com.bailout.stickk.ubi4.persistence.") -> true
                    else -> false
                }
            })
        }
    }

    @Test fun `gesture adapters do not access rotation storage or send device commands`() {
        val adapters = File(root.parentFile.parentFile, "adapters/widgetDelegateAdaptersV3")
        val forbidden = listOf(
            "ParameterProvider", "ParameterStoreV3", "ParameterTypedValueV3", "ParameterCodecRegistryV3",
            "SettingsProfileManager", "rotationGroupGestures", "BLECommandsV3", "RotationGroupV3",
            "onSendBLERotationGroup", "activeGestureFragmentFilterFlow",
            "LAST_ACTIVE_GESTURE_FILTER", "LAST_HIDE_COLLECTION_BTN_STATE",
            "SELECT_GESTURE_SETTINGS_NUM", "onShowGestureSettings", "saveInt", "startActivity",
            "CollectionGesturesProvider", "getSharedPreferences", "gestureNameList",
        )
        listOf("GesturesTwoSectionDelegateAdapterV3.kt", "RotationGroupItemAdapterV3.java").forEach { name ->
            val code = File(adapters, name).readText()
            assertTrue(forbidden.none { Regex("\\b$it\\b").containsMatchIn(code) },
                "$name must render screen state and pass actions without accessing rotation storage or BLE")
        }
    }

    @Test fun `base widgets fragment does not depend on V3 gesture presentation or adapters`() {
        val base = File(root.parentFile.parentFile, "ui/fragments/base/BaseWidgetsFragment.kt")
        reject(base, imports(base).filter {
            it.startsWith(prefix + "presentation.gestures.") ||
                it.endsWith(".GesturesTwoSectionDelegateAdapterV3") || it.endsWith(".RotationGroupItemAdapterV3")
        })
    }

    @Test fun `base widgets fragment does not coordinate V3 sensors screen state or actions`() {
        val base = File(root.parentFile.parentFile, "ui/fragments/base/BaseWidgetsFragment.kt")
        reject(base, imports(base).filter { it.startsWith(prefix + "presentation.sensors.") })
        val callbacks = listOf("renderV3Plot", "renderV3SensorsButtons", "onV3PlotAction", "onV3SensorsButtonsAction")
        assertTrue(callbacks.none { Regex("\\b$it\\b").containsMatchIn(base.readText()) },
            "Sensors state and actions belong to SensorsFragment; retain only the common adapter set in Base")
    }

    @Test fun `base widgets fragment does not coordinate V3 special settings state or actions`() {
        val base = File(root.parentFile.parentFile, "ui/fragments/base/BaseWidgetsFragment.kt")
        reject(base, imports(base).filter { dependency ->
            listOf("specialsettings", "settingsprofiles", "autologin", "togglesliders").any {
                dependency.startsWith(prefix + "presentation.$it.")
            }
        })
        val callbacks = listOf(
            "renderV3ToggleSliders", "onV3ToggleSliderAction", "renderV3AutoLogin", "onV3AutoLoginChanged",
            "renderV3SettingsProfiles", "onV3SettingsProfileSelected", "onV3SettingsProfileCreateRequested",
            "onV3SettingsProfileRenameRequested",
        )
        assertTrue(callbacks.none { Regex("\\b$it\\b").containsMatchIn(base.readText()) },
            "Special settings state and actions belong to SpecialSettingsFragment; common consumers keep their adapters")
    }

    @Test fun `base widgets fragment does not coordinate V3 service state or actions`() {
        val base = File(root.parentFile.parentFile, "ui/fragments/base/BaseWidgetsFragment.kt")
        reject(base, imports(base).filter {
            it.startsWith(prefix + "presentation.service.") || it.startsWith(prefix + "presentation.spinners.") ||
                it.startsWith(prefix + "domain.service.")
        })
        val callbacks = listOf("renderV3Calibration", "renderV3TextInputs", "renderV3Spinners",
            "onV3CalibrationButtonPressed", "onV3CalibrationButtonReleased", "onV3TextInputChanged",
            "onV3TextInputPrefillRequested", "onV3TextInputSendClicked", "onV3SpinnerAction")
        assertTrue(callbacks.none { Regex("\\b$it\\b").containsMatchIn(base.readText()) },
            "Service state and actions belong to ServiceFragment; common consumers keep their adapters")
    }

    @Test fun `gesture visual catalog cannot read preferences or global application state`() {
        val catalog = File(root.parentFile.parentFile, "ui/gestures/GestureCollectionFactory.kt").readText()
        val forbidden = listOf("SharedPreferences", "getSharedPreferences", "applicationContext", "WDApplication",
            "CollectionGesturesProvider", "LAST_CONNECTION_MAC_UBI4", "SELECT_GESTURE_SETTINGS_NUM")
        assertTrue(forbidden.none { Regex("\\b$it\\b").containsMatchIn(catalog) })
        sources("presentation/gestures").forEach { file ->
            assertTrue(!file.readText().contains("CollectionGesturesProvider"), file.name)
        }
        val fragment = File(root.parentFile.parentFile, "ui/fragments/SprGestureFragment.kt").readText()
        val v3Rendering = fragment.substringAfter("private fun renderV3GesturesScreen(")
            .substringBefore("private fun openV3GestureSettings(")
        assertTrue(!v3Rendering.contains("CollectionGesturesProvider"))
    }

    @Test fun `V3 screen dependency assembly belongs to di instead of fragments`() {
        val ui = File(root.parentFile.parentFile, "ui/fragments")
        listOf("SensorsFragment.kt", "SpecialSettingsFragment.kt", "ServiceFragment.kt", "SprGestureFragment.kt").forEach { name ->
            val fragment = File(ui, name)
            reject(fragment, imports(fragment).filter {
                it.startsWith(prefix + "data.") || it.contains("Repository") || it.contains("UseCase") ||
                    it.contains("DataFactoryV3") || it.endsWith(".SettingsProfileApplierV3")
            })
            assertTrue(!Regex("\\b\\w*Repository\\w*\\b").containsMatchIn(fragment.readText()), name)
            assertTrue(!fragment.readText().contains("getSharedPreferences"), name)
            if (name == "SensorsFragment.kt") {
                val forbidden = listOf("observeSyncProgress", "getBLEController", "refreshWidgetsV3BySwipe")
                assertTrue(forbidden.none { fragment.readText().contains(it) },
                    "Sensors synchronization callbacks belong to di, not the Fragment")
            }
            if (name == "ServiceFragment.kt") {
                val forbidden = listOf("MainActivityUBI4", "EXTRAS_DEVICE_NAME", "getCurrentSerial",
                    "mDeviceName", "applyDeviceNameImmediately")
                assertTrue(forbidden.none { fragment.readText().contains(it) },
                    "Service device-info callbacks belong to di, not the Fragment")
            }
        }
        val base = File(ui, "base/BaseWidgetsFragment.kt")
        reject(base, imports(base).filter {
            it.startsWith(prefix + "data.sensors.") || it.startsWith(prefix + "data.settings.") ||
                it.contains("Repository") || it.contains("UseCase")
        })
        assertTrue(!Regex("\\bcreateV3\\w*Repository\\b").containsMatchIn(base.readText()))
    }

    @Test fun `special settings sensors and service fragments do not assemble the V3 BLE transport`() {
        listOf("SpecialSettingsFragment.kt", "SensorsFragment.kt", "ServiceFragment.kt").forEach { name ->
            val fragment = File(root.parentFile.parentFile, "ui/fragments/$name")
            reject(fragment, imports(fragment).filter { it.contains(".ble.") })
            assertTrue(!fragment.readText().contains("bleCommandWithQueue"), name)
        }
    }

    @Test fun `gestures V3 binding does not assemble BLE transport while shared UBI4 transport remains`() {
        val fragment = File(root.parentFile.parentFile, "ui/fragments/SprGestureFragment.kt")
        val binding = fragment.readText().substringAfter("private fun bindV3Gestures()")
            .substringBefore("\n    private fun ")
        assertTrue(!binding.contains("bleCommandWithQueue"))
        reject(fragment, imports(fragment).filter { it.endsWith(".SERIALPORTCHAR_UUID") })
    }

    @Test fun `four V3 screen factories share transport without owning BLE dispatch`() {
        listOf("V3Sensors", "V3Service", "V3SpecialSettings", "V3Gestures").forEach { name ->
            val file = File(root, "di/${name}ViewModelFactory.kt")
            reject(file, imports(file).filter { it.contains(".ble.") })
            assertTrue(!file.readText().contains("bleCommandWithQueue"), name)
            assertTrue(file.readText().contains("BleDependencies.v3CommandTransport"), name)
        }
        sources("data/transport").forEach { file ->
            reject(file, imports(file).filter { it.contains(".ui.") || it.contains(".di.") || it.startsWith("android") })
        }
    }

    @Test fun `account statistics UI only observes screen state and sends actions`() {
        val ui = File(root, "presentation/accountstatistics")
        listOf("AccountFragmentStatisticsV3.kt", "V3AccountStatisticsChartMapper.kt").forEach { name ->
            val file = File(ui, name)
            assertTrue(file.isFile, "Missing statistics UI: $name")
            reject(file, imports(file).filter {
                it.contains(".data.") || it.contains(".persistence.") || it.contains(".ble.") ||
                    it.contains("Repository") || it.contains("UseCase") || it.endsWith(".CollectionGesturesProvider") ||
                    it.endsWith(".MainActivityUBI4")
            })
            assertTrue(!file.readText().contains("getSharedPreferences"), file.name)
        }
    }

    @Test fun `account profile fragment no longer requests or stores server profile data`() {
        val fragment = File(root.parentFile.parentFile, "ui/fragments/account/mainFragmentV3/AccountFragmentMainV3.kt")
        val code = fragment.readText()
        val forbidden = listOf("Ubi4RequestsApi", "NetworkResult", "EncryptionManagerUtilsUbi4", "requestToken",
            "requestUserData", "requestDeviceList", "requestDeviceInfo", "saveManagerInfo", "saveDeviceInfo",
            "cachedProfileItem", "attemptedRequest", "ACCOUNT_MANAGER_FIO", "ACCOUNT_MODEL_PROSTHESIS")
        assertTrue(forbidden.none { Regex("\\b$it\\b").containsMatchIn(code) })
        reject(fragment, imports(fragment).filter { it.startsWith(prefix + "data.accountprofile.") || it.contains("UseCase") })
    }

    @Test fun `customer service V3 binding uses state without storage or request access`() {
        val ui = File(root.parentFile.parentFile, "ui/fragments/account/customerServiceFragmentUBI4")
        val fragment = File(ui, "AccountFragmentCustomerServiceUBI4.kt")
        val code = fragment.readText()
        val binding = code.substringAfter("private fun bindV3CustomerService(")
            .substringBefore("private fun initializeUbi4Data(")
        val forbidden = listOf("loadText", "getSharedPreferences", "RequestsUBI4", "PreferenceKeysUbi4",
            "accountCustomerServiceList", "Repository", "UseCase")
        assertTrue(forbidden.none { binding.contains(it) })
        assertTrue(code.contains("if (UiState.isInterfaceV3Activated)"))
        reject(fragment, imports(fragment).filter { it.startsWith(prefix + "data.") || it.contains("UseCase") })
        val adapter = File(ui, "AccountCustomerServiceAdapterUbi4.kt")
        reject(adapter, imports(adapter).filter {
            it.contains(".data.") || it.contains(".persistence.") || it.contains("UseCase") || it.contains("Repository")
        })
    }

    @Test fun `prosthesis information V3 binding uses state without storage or request access`() {
        val ui = File(root.parentFile.parentFile, "ui/fragments/account/prosthesisInformationFragmentUBI4")
        val fragment = File(ui, "AccountFragmentProsthesisInformationUBI4.kt")
        val code = fragment.readText()
        val binding = code.substringAfter("private fun bindV3ProsthesisInformation(")
            .substringBefore("private fun initializeUbi4Data(")
        val forbidden = listOf("loadText", "getSharedPreferences", "RequestsUBI4", "PreferenceKeysUbi4",
            "accountProsthesisInformationList", "Repository", "UseCase")
        assertTrue(forbidden.none { binding.contains(it) })
        assertTrue(code.contains("if (UiState.isInterfaceV3Activated)"))
        reject(fragment, imports(fragment).filter { it.startsWith(prefix + "data.") || it.contains("UseCase") })
        listOf("AccountProsthesisInformationAdapterUBI4.kt", "AccountProsthesisInformationItemUBI4.kt").forEach { name ->
            val file = File(ui, name)
            reject(file, imports(file).filter {
                it.contains(".data.") || it.contains(".persistence.") || it.contains("UseCase") || it.contains("Repository")
            })
        }
    }

    @Test fun `games V3 binding renders state without catalog package or network queries`() {
        val fragment = File(root.parentFile.parentFile, "ui/fragments/account/games/AccountGamesFragment.kt")
        val code = fragment.readText()
        val binding = code.substringAfter("private fun bindV3Games(").substringBefore("private fun renderIdleState(")
        assertTrue(binding.contains("uiState.collect"))
        assertTrue(listOf("catalogClient", "getInstalledGameInfo", "GameCatalog.action", "refreshGameManifest", "UseCase", "Repository")
            .none { binding.contains(it) })
        reject(fragment, imports(fragment).filter { it.startsWith(prefix + "data.") || it.contains("UseCase") })
        val client = File(root.parentFile.parentFile, "data/games/GameCatalogClient.kt")
        reject(client, imports(client).filter { it.contains(".ui.") || it.contains(".presentation.") })
    }

    @Test fun `domain setting identifiers match unchanged shared protocol keys`() {
        V3ParameterKeys::class.java.fields.filter { it.type == String::class.java }.forEach { field ->
            val protocol = ConstantManagerUBI4::class.java.getField(field.name).get(null)
            assertEquals(protocol, field.get(null), field.name)
        }
        assertEquals(MobileSettingsKey.AUTO_LOGIN.key, V3AppSettingKeys.AUTO_LOGIN)
    }
}
