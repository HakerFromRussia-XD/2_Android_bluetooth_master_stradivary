package com.bailout.stickk.ubi4.firmware

object FirmwareInfoDescriptorBuilder {
    private const val STRUCT_SIZE = 120
    const val FW_DESCRIPTOR_SIZE_OFFSET = 103

    fun build(properties: Map<String, String>): FirmwareInfoDescriptor {
        val boardName = properties.getString("BoardName", "Unknown")
        val boardVersion = properties.getInt("BoardVersion")
        val boardSubVersion = properties.getInt("BoardSubVersion")
        val boardRevision = properties.getInt("BoardRevision")
        val boardSubRevision = properties.getInt("BoardSubRevision")
        val boardInstance = properties.getInt("BoardInstance")
        val boardType = properties.getInt("BoardType")
        val boardCode = properties.getInt("BoardCode")
        val boardAddInfoType = properties.getInt("BoardAdditionalInfoType")
        val boardAddInfo = properties.getLong("BoardAdditionalInfo")

        val fwName = properties.getString("FwName")
        val fwMajorVersion = properties.getInt("FwMajorVersion")
        val fwMinorVersion = properties.getInt("FwMinorVersion")
        val fwQuickFixVersion = properties.getInt("FwQuickFix")
        val fwSinceLastTag = properties.getInt("FWSinceLastTag")

        val fwLabel = properties.getString("FwLabel")
        val fwType = properties.getInt("FWType")
        val fwCode = properties.getInt("FWCode")

        val fwStartAddress = properties.getLong("FWStartAddress")
        val fwSize = properties.getLong("FWsize")
        val fwCrc = properties.getLong("FWCRC")

        val sdkMajorVersion = properties.getInt("SDKMajorVersion")
        val sdkMinorVersion = properties.getInt("SDKMinorVersion")
        val sdkQuickFixVersion = properties.getInt("SDKQuickFix")
        val sdkSinceLastTag = properties.getInt("SDKSinceLastTag")

        val fwAddInfoType = properties.getInt("FWAdditionalInfoType")
        val fwAddInfo = properties.getLong("FWAdditionalInfo")
        val localVersionString =
            "$fwMajorVersion.$fwMinorVersion.$fwQuickFixVersion.$fwSinceLastTag"

        val writer = LittleEndianWriter(STRUCT_SIZE)
        writer.putFixedString(boardName, 32)
        writer.putByte(boardVersion)
        writer.putByte(boardSubVersion)
        writer.putByte(boardRevision)
        writer.putByte(boardSubRevision)
        writer.putShort(boardInstance)
        writer.putByte(boardType)
        writer.putByte(boardCode)

        writer.putByte(boardAddInfoType)
        writer.putInt(boardAddInfo)

        writer.putFixedString(fwName, 32)
        writer.putByte(fwMajorVersion)
        writer.putByte(fwMinorVersion)
        writer.putByte(fwQuickFixVersion)
        writer.putByte(fwSinceLastTag)

        writer.putFixedString(fwLabel, 16)
        writer.putByte(fwType)
        writer.putByte(fwCode)

        writer.putInt(fwStartAddress)
        writer.putInt(fwSize)
        writer.putInt(fwCrc)

        writer.putByte(sdkMajorVersion)
        writer.putByte(sdkMinorVersion)
        writer.putByte(sdkQuickFixVersion)
        writer.putByte(sdkSinceLastTag)

        writer.putByte(fwAddInfoType)
        writer.putInt(fwAddInfo)

        return FirmwareInfoDescriptor(
            bytes = writer.toByteArray(),
            firmwareSize = fwSize,
            firmwareCrc = fwCrc,
            localVersionString = localVersionString
        )
    }

    fun patchFirmwareSizeInPlace(descriptor: ByteArray, fwSize: Int) {
        require(descriptor.size >= FW_DESCRIPTOR_SIZE_OFFSET + 4) {
            "Firmware descriptor is too small: ${descriptor.size}"
        }
        descriptor[FW_DESCRIPTOR_SIZE_OFFSET] = (fwSize and 0xFF).toByte()
        descriptor[FW_DESCRIPTOR_SIZE_OFFSET + 1] = ((fwSize shr 8) and 0xFF).toByte()
        descriptor[FW_DESCRIPTOR_SIZE_OFFSET + 2] = ((fwSize shr 16) and 0xFF).toByte()
        descriptor[FW_DESCRIPTOR_SIZE_OFFSET + 3] = ((fwSize shr 24) and 0xFF).toByte()
    }

    fun parseIniProperties(iniText: String): Map<String, String> =
        iniText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith(";") && !it.startsWith("!") }
            .mapNotNull { line ->
                val separatorIndex = listOf(line.indexOf('='), line.indexOf(':'))
                    .filter { it >= 0 }
                    .minOrNull()
                    ?: return@mapNotNull null
                val key = line.substring(0, separatorIndex).trim()
                val value = line.substring(separatorIndex + 1).trim()
                key.takeIf { it.isNotEmpty() }?.let { it to value }
            }
            .toMap()

    private fun Map<String, String>.getString(key: String, default: String = ""): String =
        this[key] ?: default

    private fun Map<String, String>.getInt(key: String, default: Int = 0): Int =
        this[key]?.toIntOrNull() ?: default

    private fun Map<String, String>.getLong(key: String, default: Long = 0L): Long =
        this[key]?.toLongOrNull() ?: default

    private class LittleEndianWriter(size: Int) {
        private val bytes = ByteArray(size)
        private var position = 0

        fun putFixedString(value: String, length: Int) {
            val encoded = value.encodeToByteArray()
            val copyLength = encoded.size.coerceAtMost(length)
            encoded.copyInto(bytes, destinationOffset = position, endIndex = copyLength)
            position += length
        }

        fun putByte(value: Int) {
            bytes[position] = value.toByte()
            position += 1
        }

        fun putShort(value: Int) {
            bytes[position] = (value and 0xFF).toByte()
            bytes[position + 1] = ((value shr 8) and 0xFF).toByte()
            position += 2
        }

        fun putInt(value: Long) {
            bytes[position] = (value and 0xFFL).toByte()
            bytes[position + 1] = ((value shr 8) and 0xFFL).toByte()
            bytes[position + 2] = ((value shr 16) and 0xFFL).toByte()
            bytes[position + 3] = ((value shr 24) and 0xFFL).toByte()
            position += 4
        }

        fun toByteArray(): ByteArray = bytes
    }
}
