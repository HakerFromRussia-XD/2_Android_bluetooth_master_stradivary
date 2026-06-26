package com.bailout.stickk.ubi4.data.state

data class DashboardSlotContentInput(
    val name: String,
    val value: String,
    val path: String
)

data class DashboardSlotContentStructure(
    val index: Int,
    val title: String,
    val parameters: List<DashboardSlotContentInput>
)

data class DashboardSlotContentStructureGroup(
    val title: String,
    val structures: List<DashboardSlotContentStructure>
)

data class DashboardSlotContentParsed(
    val inputs: List<DashboardSlotContentInput>,
    val structureGroups: List<DashboardSlotContentStructureGroup>
)

object DashboardSlotContentSchemas {
    private data class SlotSchema(
        val dataCode: Int,
        val version: Int,
        val subVersion: Int,
        val fields: List<SlotField>
    )

    private data class SlotField(
        val name: String,
        val type: String,
        val offset: Int,
        val size: Int,
        val format: String = "decimal",
        val array: Int = 0,
        val fields: List<SlotField> = emptyList()
    )

    fun parse(state: DashboardSlotContentUiState): DashboardSlotContentParsed {
        val data = state.data
        if (data.isEmpty()) return DashboardSlotContentParsed(emptyList(), emptyList())

        val schema = findSchema(state)
            ?: return parseRawBytes(data)

        val inputs = mutableListOf<DashboardSlotContentInput>()
        val groups = mutableListOf<DashboardSlotContentStructureGroup>()

        schema.fields.forEach { field ->
            if (field.array > 0 && field.fields.isNotEmpty()) {
                groups += parseStructureArray(state, field)
            } else {
                inputs += parseInput(state, field, field.offset)
            }
        }

        return DashboardSlotContentParsed(inputs, groups)
    }

    fun updateValue(state: DashboardSlotContentUiState, path: String, value: String): List<Int> {
        val data = state.data.toMutableList()
        if (path.startsWith(RAW_BYTE_PATH_PREFIX)) {
            val index = path.substringAfter(RAW_BYTE_PATH_PREFIX).toIntOrNull()
            if (index != null) data.writeScalar(index, 1, TYPE_UINT8, value)
            return data
        }

        val pathInfo = SlotInputPath.decode(path) ?: return data
        if (pathInfo.arrayCount > 1) {
            data.writeArray(pathInfo.offset, pathInfo.size, pathInfo.type, value, pathInfo.arrayCount)
        } else {
            data.writeScalar(pathInfo.offset, pathInfo.size, pathInfo.type, value)
        }
        return data
    }

    private fun findSchema(state: DashboardSlotContentUiState): SlotSchema? =
        schemas.firstOrNull {
            it.dataCode == state.dataCode &&
                it.version == state.version &&
                it.subVersion == state.subVersion
        } ?: schemas.firstOrNull {
            it.dataCode == state.dataCode &&
                it.version == state.version
        } ?: schemas.firstOrNull {
            it.dataCode == state.dataCode
        }

    private fun parseStructureArray(
        state: DashboardSlotContentUiState,
        field: SlotField
    ): DashboardSlotContentStructureGroup {
        val structures = (0 until field.array).map { index ->
            val baseOffset = field.offset + index * field.size
            DashboardSlotContentStructure(
                index = index,
                title = "${field.name}[$index]",
                parameters = field.fields.map { child ->
                    parseInput(state, child, baseOffset + child.offset)
                }
            )
        }

        return DashboardSlotContentStructureGroup(
            title = field.name,
            structures = structures
        )
    }

    private fun parseInput(
        state: DashboardSlotContentUiState,
        field: SlotField,
        absoluteOffset: Int
    ): DashboardSlotContentInput {
        val data = state.data
        val arrayCount = field.array.coerceAtLeast(1)
        val path = SlotInputPath(
            offset = absoluteOffset,
            size = field.size,
            type = field.type,
            arrayCount = arrayCount
        ).encode()
        val formattedValue = if (arrayCount > 1) {
            data.formatArray(absoluteOffset, field.size, field.type, field.format, arrayCount)
        } else {
            data.formatScalar(absoluteOffset, field.size, field.type, field.format)
        }
        return DashboardSlotContentInput(
            name = field.name,
            value = state.editedValues[path] ?: formattedValue,
            path = path
        )
    }

    private fun parseRawBytes(data: List<Int>): DashboardSlotContentParsed =
        DashboardSlotContentParsed(
            inputs = data.mapIndexed { index, value ->
                DashboardSlotContentInput(
                    name = "byte[$index]",
                    value = "0x${value.toHexByte()}",
                    path = "$RAW_BYTE_PATH_PREFIX$index"
                )
            },
            structureGroups = emptyList()
        )

    private fun List<Int>.formatArray(
        offset: Int,
        size: Int,
        type: String,
        format: String,
        count: Int
    ): String =
        (0 until count)
            .joinToString(prefix = "[", postfix = "]", separator = ", ") { index ->
                formatScalar(offset + index * size, size, type, format)
            }

    private fun List<Int>.formatScalar(offset: Int, size: Int, type: String, format: String): String {
        if (offset < 0 || offset + size > this.size) return ""
        return when (type.normalizedType()) {
            TYPE_STRING -> readString(offset, size)
            TYPE_BITFIELD -> {
                val value = readUInt(offset, size)
                if (value == 0L) "None" else "0x${value.toString(16).uppercase()}"
            }
            TYPE_FLOAT -> Float.fromBits(readUInt(offset, size).toInt()).toString()
            TYPE_DOUBLE -> Double.fromBits(readUInt(offset, size)).toString()
            TYPE_INT8, TYPE_INT16, TYPE_INT32 -> readInt(offset, size).toString()
            else -> {
                val value = readUInt(offset, size)
                if (format == FORMAT_HEX) {
                    "0x${value.toHex(size)}"
                } else {
                    value.toString()
                }
            }
        }
    }

    private fun MutableList<Int>.writeArray(
        offset: Int,
        size: Int,
        type: String,
        value: String,
        count: Int
    ) {
        value.parseValueList()
            .take(count)
            .forEachIndexed { index, item ->
                writeScalar(offset + index * size, size, type, item)
            }
    }

    private fun MutableList<Int>.writeScalar(offset: Int, size: Int, type: String, value: String) {
        if (offset < 0 || offset + size > this.size) return
        when (type.normalizedType()) {
            TYPE_STRING -> writeString(offset, size, value)
            TYPE_FLOAT -> writeUInt(offset, size, value.toFloatOrNull()?.toRawBits()?.toLong() ?: return)
            TYPE_DOUBLE -> writeUInt(offset, size, value.toDoubleOrNull()?.toRawBits() ?: return)
            TYPE_INT8, TYPE_INT16, TYPE_INT32 -> writeInt(offset, size, value.parseNumber() ?: return)
            TYPE_BITFIELD -> {
                if (value.equals("None", ignoreCase = true)) {
                    writeUInt(offset, size, 0)
                } else {
                    writeUInt(offset, size, value.parseNumber() ?: return)
                }
            }
            else -> writeUInt(offset, size, value.parseNumber() ?: return)
        }
    }

    private fun MutableList<Int>.writeString(offset: Int, size: Int, value: String) {
        repeat(size) { index ->
            this[offset + index] = value.getOrNull(index)?.code ?: 0
        }
    }

    private fun MutableList<Int>.writeInt(offset: Int, size: Int, value: Long) {
        val mask = if (size >= 8) -1L else (1L shl (size * 8)) - 1L
        writeUInt(offset, size, value and mask)
    }

    private fun MutableList<Int>.writeUInt(offset: Int, size: Int, value: Long) {
        repeat(size) { index ->
            this[offset + index] = ((value shr (index * 8)) and 0xFF).toInt()
        }
    }

    private fun List<Int>.readUInt(offset: Int, size: Int): Long {
        var value = 0L
        repeat(size) { index ->
            value = value or ((u8(offset + index).toLong() and 0xFF) shl (index * 8))
        }
        return value
    }

    private fun List<Int>.readInt(offset: Int, size: Int): Long {
        val unsigned = readUInt(offset, size)
        val bits = size * 8
        val signBit = 1L shl (bits - 1)
        return if ((unsigned and signBit) != 0L) {
            unsigned - (1L shl bits)
        } else {
            unsigned
        }
    }

    private fun List<Int>.readString(offset: Int, size: Int): String =
        buildString {
            for (index in 0 until size) {
                val byte = u8(offset + index)
                if (byte == 0) break
                append(byte.toChar())
            }
        }

    private fun String.parseValueList(): List<String> =
        trim()
            .removePrefix("[")
            .removeSuffix("]")
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun String.parseNumber(): Long? {
        val normalized = trim()
        if (normalized.isEmpty()) return null
        return if (normalized.startsWith("0x", ignoreCase = true)) {
            normalized.drop(2).toLongOrNull(16)
        } else {
            normalized.toLongOrNull()
        }
    }

    private fun List<Int>.u8(offset: Int): Int =
        getOrNull(offset) ?: 0

    private fun Int.toHexByte(): String =
        and(0xFF).toString(16).uppercase().padStart(2, '0')

    private fun Long.toHex(size: Int): String {
        val hex = toString(16).uppercase()
        return if (size == 1) hex.padStart(2, '0') else hex
    }

    private fun String.normalizedType(): String =
        lowercase().removeSuffix("_t")

    private data class SlotInputPath(
        val offset: Int,
        val size: Int,
        val type: String,
        val arrayCount: Int
    ) {
        fun encode(): String =
            listOf(PATH_PREFIX, offset, size, type, arrayCount).joinToString(PATH_SEPARATOR)

        companion object {
            fun decode(path: String): SlotInputPath? {
                val parts = path.split(PATH_SEPARATOR)
                if (parts.size != 5 || parts[0] != PATH_PREFIX) return null
                return SlotInputPath(
                    offset = parts[1].toIntOrNull() ?: return null,
                    size = parts[2].toIntOrNull() ?: return null,
                    type = parts[3],
                    arrayCount = parts[4].toIntOrNull() ?: return null
                )
            }
        }
    }

    private const val RAW_BYTE_PATH_PREFIX = "raw:"
    private const val PATH_PREFIX = "field"
    private const val PATH_SEPARATOR = "|"
    private const val FORMAT_HEX = "hex"
    private const val TYPE_BITFIELD = "bitfield"
    private const val TYPE_DOUBLE = "double"
    private const val TYPE_FLOAT = "float"
    private const val TYPE_INT8 = "int8"
    private const val TYPE_INT16 = "int16"
    private const val TYPE_INT32 = "int32"
    private const val TYPE_STRING = "string"
    private const val TYPE_UINT8 = "uint8"

    private fun f(
        name: String,
        type: String,
        offset: Int,
        size: Int,
        format: String = "decimal",
        array: Int = 0,
        fields: List<SlotField> = emptyList()
    ) = SlotField(name, type, offset, size, format, array, fields)

    private val schemas = listOf(
        SlotSchema(
            dataCode = 1,
            version = 1,
            subVersion = 0,
            fields = listOf(
                f("BootloaderCode", "uint8", 0, 1),
                f("BootloaderVersion", "uint8", 1, 1),
                f("BootloaderSubVersion", "uint8", 2, 1),
                f("ProtocolVersion", "uint8", 3, 1),
                f("ProtocolSubVersion", "uint8", 4, 1),
                f("BootloaderAdditionalInfoType", "uint8", 5, 1),
                f("BootloaderCRC", "uint8", 6, 1),
                f("BootloaderFlags", "bitfield", 7, 1),
                f("BootloaderStartAddress", "uint32", 8, 4, FORMAT_HEX),
                f("BootloaderSize", "uint32", 12, 4),
                f("BootloaderAdditionalInfo", "uint32", 16, 4, FORMAT_HEX)
            )
        ),
        SlotSchema(
            dataCode = 2,
            version = 1,
            subVersion = 0,
            fields = listOf(
                f("FWName", "string", 0, 32),
                f("FWMajorVersion", "uint8", 32, 1),
                f("FWMinorVersion", "uint8", 33, 1),
                f("FWQuickFixVersion", "uint8", 34, 1),
                f("FWSinceLastTag", "uint8", 35, 1),
                f("FWLabel", "string", 36, 16),
                f("FWType", "uint8", 52, 1),
                f("FWCode", "uint8", 53, 1),
                f("FWStartAddress", "uint32", 54, 4, FORMAT_HEX),
                f("FWSize", "uint32", 58, 4),
                f("FWCRC", "uint32", 62, 4, FORMAT_HEX),
                f("SDKMajorVersion", "uint8", 66, 1),
                f("SDKMinorVersion", "uint8", 67, 1),
                f("SDKQuickFixVersion", "uint8", 68, 1),
                f("SDKSinceLastTag", "uint8", 69, 1),
                f("FWAdditionalInfoType", "uint8", 70, 1),
                f("FWAdditionalInfo", "uint32", 71, 4, FORMAT_HEX)
            )
        ),
        SlotSchema(
            dataCode = 3,
            version = 1,
            subVersion = 2,
            fields = listOf(
                f("DeviceName", "string", 0, 32),
                f("DeviceVersion", "uint8", 32, 1),
                f("DeviceSubVersion", "uint8", 33, 1),
                f("DeviceLabel", "string", 34, 16),
                f("DeviceType", "uint8", 50, 1),
                f("DeviceCode", "uint8", 51, 1),
                f("DeviceRole", "uint8", 52, 1),
                f("DeviceAddress", "uint8", 53, 1, FORMAT_HEX),
                f("DeviceUUID_Prefix", "string", 54, 16),
                f("DeviceUUID", "uint32", 70, 4, FORMAT_HEX),
                f("DeviceAdditionalInfoType", "uint8", 74, 1),
                f("DeviceAdditionalInfo", "uint32", 75, 4, FORMAT_HEX),
                f("DeviceIsCopyable", "uint8", 79, 1)
            )
        ),
        SlotSchema(
            dataCode = 4,
            version = 1,
            subVersion = 0,
            fields = listOf(
                f("BoardName", "string", 0, 32),
                f("BoardVersion", "uint8", 32, 1),
                f("BoardSubVersion", "uint8", 33, 1),
                f("BoardRev", "uint8", 34, 1),
                f("BoardSubRev", "uint8", 35, 1),
                f("BoardBuild", "uint16", 36, 2),
                f("BoardType", "uint8", 38, 1),
                f("BoardCode", "uint8", 39, 1),
                f("BoardAdditionalInfoType", "uint8", 40, 1),
                f("BoardAdditionalInfo", "uint32", 41, 4, FORMAT_HEX)
            )
        ),
        SlotSchema(
            dataCode = 5,
            version = 1,
            subVersion = 0,
            fields = listOf(
                f("ProductName", "string", 0, 32),
                f("ProductVersion", "uint8", 32, 1),
                f("ProductSubVersion", "uint8", 33, 1),
                f("ProductLabel", "string", 34, 16),
                f("ProductType", "uint8", 50, 1),
                f("ProductCode", "uint8", 51, 1),
                f("ProductUUID_Prefix", "string", 52, 16),
                f("ProductUUID", "uint32", 68, 4, FORMAT_HEX),
                f("ProductAdditionalInfoType", "uint8", 72, 1),
                f("ProductAdditionalInfo", "uint32", 73, 4, FORMAT_HEX)
            )
        ),
        SlotSchema(
            dataCode = 6,
            version = 1,
            subVersion = 0,
            fields = listOf(
                f("ProductionDate_Day", "uint8", 0, 1),
                f("ProductionDate_Month", "uint8", 1, 1),
                f("ProductionDate_Year", "uint16", 2, 2),
                f("LastServiceDate_Day", "uint8", 4, 1),
                f("LastServiceDate_Month", "uint8", 5, 1),
                f("LastServiceDate_Year", "uint16", 6, 2)
            )
        ),
        gestureSlotSchema(dataCode = 9, gestureCount = 16),
        gestureSlotSchema(dataCode = 10, gestureCount = 15),
        SlotSchema(
            dataCode = 15,
            version = 1,
            subVersion = 1,
            fields = listOf(
                f("Threshold_OpenUpper", "uint8", 0, 1),
                f("Threshold_OpenLower", "uint8", 1, 1),
                f("Threshold_CloseUpper", "uint8", 2, 1),
                f("Threshold_CloseLower", "uint8", 3, 1),
                f("CurrentGesture", "uint8", 4, 1),
                f("HandControlMode", "uint8", 5, 1),
                f("GlobalForce", "uint8", 6, 1),
                f("GlobalSpeed", "uint8", 7, 1),
                f("MaxCurrent", "uint32", 8, 4),
                f("FirstFingerChannel", "uint8", 12, 1),
                f("FingerChannel", "uint8", 13, 1),
                f("ChangeGestureMode", "uint8", 14, 1),
                f("EMG_LockTimer_Raw", "uint8", 15, 1, FORMAT_HEX),
                f("EMG_LockTimer_Value", "uint8", 15, 1),
                f("EMG_ChangeGestureTimer_Raw", "uint8", 16, 1, FORMAT_HEX),
                f("EMG_ChangeGestureTimer_Value", "uint8", 16, 1)
            )
        ),
        SlotSchema(
            dataCode = 20,
            version = 1,
            subVersion = 0,
            fields = listOf(
                f("Size", "uint16", 0, 2),
                f("Count", "uint8", 2, 1),
                f("ItemSize", "uint8", 3, 1),
                f("gesture_key_items", "gesture_key_item", 4, 5, array = 16, fields = listOf(
                    f("slot", "uint8", 0, 1),
                    f("pos", "uint8", 1, 1),
                    f("id", "uint8", 2, 1),
                    f("image_key", "uint8", 3, 1),
                    f("code", "uint8", 4, 1)
                ))
            )
        ),
        SlotSchema(
            dataCode = 21,
            version = 1,
            subVersion = 0,
            fields = listOf(
                f("Size", "uint8", 0, 1),
                f("Count", "uint8", 1, 1),
                f("ItemSize", "uint16", 2, 2),
                f("gesture_string_items", "gesture_string_item", 4, 21, array = 15, fields = listOf(
                    f("slot", "uint8", 0, 1),
                    f("pos", "uint8", 1, 1),
                    f("id", "uint8", 2, 1),
                    f("image_key", "uint8", 3, 1),
                    f("code", "uint8", 4, 1),
                    f("name", "string", 5, 16)
                ))
            )
        ),
        SlotSchema(
            dataCode = 22,
            version = 1,
            subVersion = 0,
            fields = listOf(
                f("gesture_items", "gesture_item", 0, 2, array = 8, fields = listOf(
                    f("gesture_id", "uint8", 0, 1),
                    f("gesture_image_code", "uint8", 1, 1)
                ))
            )
        ),
        SlotSchema(
            dataCode = 25,
            version = 1,
            subVersion = 0,
            fields = listOf(
                f("EnergySavingTimeout", "uint8", 0, 1),
                f("DisplayOrientation", "uint8", 1, 1)
            )
        ),
        SlotSchema(
            dataCode = 29,
            version = 1,
            subVersion = 0,
            fields = listOf(
                f("OpenGain", "uint8", 0, 1),
                f("CloseGain", "uint8", 1, 1),
                f("SwitchChannels", "uint8", 2, 1),
                f("MaxGainValue", "uint8", 3, 1),
                f("EMGMode", "uint8", 4, 1)
            )
        )
    )

    private fun gestureSlotSchema(dataCode: Int, gestureCount: Int): SlotSchema =
        SlotSchema(
            dataCode = dataCode,
            version = 1,
            subVersion = 0,
            fields = listOf(
                f("Size", "uint8", 0, 1),
                f("Count", "uint8", 1, 1),
                f("ItemSize", "uint16", 2, 2),
                f("NumDrive", "uint8", 4, 1),
                f("gestures", "gesture", 5, 25, array = gestureCount, fields = listOf(
                    f("id", "uint8", 0, 1),
                    f("open_positions", "uint8", 1, 1, FORMAT_HEX, array = 6),
                    f("close_positions", "uint8", 7, 1, FORMAT_HEX, array = 6),
                    f("open_to_close_time", "uint8", 13, 1, FORMAT_HEX, array = 6),
                    f("close_to_open_time", "uint8", 19, 1, FORMAT_HEX, array = 6)
                ))
            )
        )
}
