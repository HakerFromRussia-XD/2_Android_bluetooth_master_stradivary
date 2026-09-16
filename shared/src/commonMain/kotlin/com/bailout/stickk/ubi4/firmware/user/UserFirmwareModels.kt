package com.bailout.stickk.ubi4.firmware.user

import com.bailout.stickk.ubi4.firmware.FirmwareInfoDescriptorBuilder
import com.bailout.stickk.ubi4.firmware.FirmwareUpdatePackage
import com.bailout.stickk.ubi4.firmware.MotoricaCrc32
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class UserFirmwareVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<UserFirmwareVersion> {
    init { require(listOf(major, minor, patch).all { it in 0..255 }) }
    override fun compareTo(other: UserFirmwareVersion): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
    override fun toString(): String = "$major.$minor.$patch"
    companion object {
        fun parse(value: String?): UserFirmwareVersion? {
            val parts = value?.split('.') ?: return null
            if (parts.size != 3) return null
            val numbers = parts.map { it.toIntOrNull() ?: return null }
            return runCatching { UserFirmwareVersion(numbers[0], numbers[1], numbers[2]) }.getOrNull()
        }
    }
}

@Serializable
data class AssemblyModule(val address: Int, val name: String = "", val file: String, val sha256: String)

@Serializable
data class FirmwareAssembly(
    val format: Int,
    val kind: String,
    val created_at: String,
    val created_by: String,
    val label: String = "",
    val product: String,
    val config_version: Int,
    val modules: List<AssemblyModule>,
    val checksum: String
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun parse(text: String): FirmwareAssembly {
            val document = json.parseToJsonElement(text).jsonObject
            val assembly = json.decodeFromJsonElement<FirmwareAssembly>(document)
            require(assembly.format == 1 && assembly.kind == "assembly") { "Unsupported assembly format" }
            require(assembly.created_at.isNotBlank() && assembly.created_by.isNotBlank() && assembly.product.isNotBlank())
            require(assembly.config_version >= 0)
            require(assembly.modules.map { it.address }.distinct().size == assembly.modules.size) { "Duplicate board address" }
            assembly.modules.forEach {
                require(it.address in 0..255 && it.sha256.matches(Regex("[0-9a-f]{64}")))
                require(it.file.isNotBlank() && !it.file.startsWith('/') && '\\' !in it.file && ':' !in it.file)
                require(it.file.split('/').none { part -> part in listOf("", ".", "..") })
                require(it.file.endsWith(".zip", ignoreCase = true)) { "User update requires a firmware ZIP" }
            }
            require(assembly.checksum == firmwareSha256(canonical(JsonObject(document - "checksum")).encodeToByteArray())) {
                "Assembly checksum mismatch"
            }
            return assembly
        }

        // The assembly schema contains only integer numbers. Slots and their values are separate documents.
        internal fun canonical(value: JsonElement): String = when (value) {
            is JsonObject -> value.keys.sorted().joinToString(",", "{", "}") {
                JsonPrimitive(it).toString() + ":" + canonical(value.getValue(it))
            }
            is JsonArray -> value.joinToString(",", "[", "]") { canonical(it) }
            else -> value.toString()
        }
    }
}

/** Slots keep their schema version/subversion; quickfiks is the requested optional extension.
 * These fields are not the main-program version and never appear in assembly.modules. */
@Serializable
data class FirmwareModuleSlotVersion(val version: Int, val subversion: Int, val quickfiks: Int = 0)

data class UserFirmwareArchive(val descriptorText: String, val payload: ByteArray) {
    fun version(): UserFirmwareVersion {
        val fields = FirmwareInfoDescriptorBuilder.parseIniProperties(descriptorText)
        fun field(name: String) = fields[name]?.toIntOrNull() ?: error("Missing firmware field: $name")
        require(field("FWType") == 0) { "Only main firmware is allowed" }
        require(payload.isNotEmpty()) { "Empty firmware image" }
        return UserFirmwareVersion(field("FwMajorVersion"), field("FwMinorVersion"), field("FwQuickFix"))
    }
    fun validateImage() {
        version()
        val fields = FirmwareInfoDescriptorBuilder.parseIniProperties(descriptorText)
        val crc = fields["FWCRC"]?.toLongOrNull() ?: error("Missing firmware CRC")
        require(MotoricaCrc32.calculate(payload) == crc) { "Firmware image CRC mismatch" }
    }
    fun packageFor(name: String): FirmwareUpdatePackage {
        version()
        val descriptor = FirmwareInfoDescriptorBuilder.build(FirmwareInfoDescriptorBuilder.parseIniProperties(descriptorText))
        return FirmwareUpdatePackage(name, descriptor.bytes, payload, descriptor.firmwareSize,
            descriptor.firmwareCrc, descriptor.localVersionString)
    }
}

interface UserFirmwareArchiveReader {
    /** Callback on the main thread; null means an invalid/unreadable archive. */
    fun read(path: String, callback: (UserFirmwareArchive?) -> Unit)
}

expect fun firmwareSha256(bytes: ByteArray): String
expect suspend fun writeFirmwareJournal(path: String, text: String)

@Serializable
data class UserFirmwareTarget(val module: AssemblyModule, val version: UserFirmwareVersion, val path: String)

data class UserFirmwareBoard(val address: Int, val version: UserFirmwareVersion?, val isMain: Boolean)

@Serializable
data class UserFirmwareJournal(
    val deviceId: String,
    val targets: List<UserFirmwareTarget>,
    val completed: Set<Int> = emptySet(),
    val attempted: Set<Int> = emptySet()
)

object UserFirmwarePolicy {
    fun queue(boards: List<UserFirmwareBoard>, targets: List<UserFirmwareTarget>): List<UserFirmwareTarget> {
        val present = boards.associateBy { it.address }
        return targets.filter { target ->
            val board = present[target.module.address] ?: return@filter false
            val installed = requireNotNull(board.version) { "Unknown firmware version at ${board.address}" }
            installed < target.version
        }.sortedBy { if (it.module.address == 0) 1 else 0 }
    }
    fun completed(board: UserFirmwareBoard, target: UserFirmwareTarget): Boolean =
        board.isMain && board.version == target.version
    fun retry(board: UserFirmwareBoard, target: UserFirmwareTarget): Boolean =
        !board.isMain && board.version != null && board.version != target.version
}

data class UserFirmwareUiState(
    val phase: String = "idle",
    val boardNumber: Int = 0,
    val boardCount: Int = 0,
    val progress: Int = 0,
    val detail: String = ""
) {
    val blocksInteraction: Boolean get() = phase in setOf("offered", "preparing", "updating", "verifying", "waiting", "complete")
}

object UserFirmwareActivity { var isActive: Boolean = false
    internal set
}
