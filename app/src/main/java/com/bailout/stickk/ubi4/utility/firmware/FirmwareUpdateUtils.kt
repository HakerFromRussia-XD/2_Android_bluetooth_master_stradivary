package com.bailout.stickk.ubi4.utility.firmware

import android.util.Log
import com.bailout.stickk.ubi4.firmware.FirmwareInfoDescriptorBuilder
import com.bailout.stickk.ubi4.firmware.FirmwareUpdatePackage
import java.io.File
import java.io.InputStreamReader
import java.util.Properties
import java.util.zip.ZipFile

object FirmwareUpdateUtils {

    var lastFwSize: Long  = 0
        private set
    var lastFwCrc : Long = 0
        private set

    var lastLocalVersionString: String? = null
        private set

    fun buildFwInfoDescriptor(zipFile: File): ByteArray {
        return readFirmwareDescriptor(zipFile).bytes
    }

    fun readFirmwarePackage(zipFile: File): FirmwareUpdatePackage {
        val descriptor = readFirmwareDescriptor(zipFile)
        val payload = readFirmwareBytes(zipFile)
        return FirmwareUpdatePackage(
            name = zipFile.name,
            descriptor = descriptor.bytes,
            payload = payload,
            descriptorFirmwareSize = descriptor.firmwareSize,
            descriptorFirmwareCrc = descriptor.firmwareCrc,
            localVersionString = descriptor.localVersionString
        )
    }

    fun readFirmwareBytes(zipFile: File): ByteArray =
        ZipFile(zipFile).use { zip ->
            val entry = zip.entries().toList()
                .first { !it.isDirectory && it.name.endsWith(".bin", ignoreCase = true) }
            zip.getInputStream(entry).use { it.readBytes() }
        }

    private fun readFirmwareDescriptor(zipFile: File) =
        readIniProperties(zipFile)
            .also(::logIniProperties)
            .let { ini ->
                FirmwareInfoDescriptorBuilder.build(ini.toStringMap()).also { descriptor ->
                    lastFwSize = descriptor.firmwareSize
                    lastFwCrc = descriptor.firmwareCrc
                    lastLocalVersionString = descriptor.localVersionString

                    Log.e("FW_NAME", "boardName = ${ini.getProperty("BoardName", "Unknown")}")
                    Log.e("FW_NAME", "boardVersion = ${ini.getProperty("BoardVersion", "0")}")
                    Log.e("FW_NAME", "boardBuild = ${ini.getProperty("BoardInstance", "0")}")
                }
            }

    private fun readIniProperties(zipFile: File): Properties =
        ZipFile(zipFile).use { zip ->
            val entry = zip.entries().toList()
                .first { !it.isDirectory && it.name.equals("FW_ini.ini", ignoreCase = true) }
            Properties().apply {
                zip.getInputStream(entry).use { input ->
                    InputStreamReader(input).use { reader -> load(reader) }
                }
            }
        }

    private fun logIniProperties(ini: Properties) {
        ini.stringPropertyNames().forEach { key ->
            Log.e("FW_INI", "$key = ${ini.getProperty(key)}")
        }
    }

    private fun Properties.toStringMap(): Map<String, String> =
        stringPropertyNames().associateWith { key -> getProperty(key) }

}
