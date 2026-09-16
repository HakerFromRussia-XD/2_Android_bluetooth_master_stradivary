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
                    it.startsWith("androidx.lifecycle.") || it.startsWith("android.view.")
            })
        }
    }

    @Test fun `presentation does not hold repositories or build use cases`() {
        val repositories = sources("domain").filter { it.name.endsWith("Repository.kt") }.map { it.nameWithoutExtension }
        sources("presentation").forEach { file ->
            val code = file.readText()
            reject(file, repositories.filter { Regex("\\b$it\\b").containsMatchIn(code) })
            reject(file, imports(file).filter { it.startsWith(prefix + "data.") || it.startsWith(prefix + "di.") })
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

    @Test fun `domain setting identifiers match unchanged shared protocol keys`() {
        V3ParameterKeys::class.java.fields.filter { it.type == String::class.java }.forEach { field ->
            val protocol = ConstantManagerUBI4::class.java.getField(field.name).get(null)
            assertEquals(protocol, field.get(null), field.name)
        }
        assertEquals(MobileSettingsKey.AUTO_LOGIN.key, V3AppSettingKeys.AUTO_LOGIN)
    }
}
