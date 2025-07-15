package com.bailout.stickk.ubi4.utility.firmware

import android.util.Log
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.local.MaxChunkSizeInfo
import com.bailout.stickk.ubi4.utility.CrcCalc
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object FirmwareUpdateUtils {

    var lastFwSize: Long  = 0
        private set
    var lastFwCrc : Long = 0
        private set


    fun buildFwInfoDescriptor(zipFile: File): ByteArray {
        val ini = Properties().apply {
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                generateSequence { zis.nextEntry }
                    .first { it.name.equals("FW_ini.ini", ignoreCase = true) }
                    .also { BufferedReader(InputStreamReader(zis)).use(this::load) }
            }
        }

        // Логируем все ключи
        ini.stringPropertyNames().forEach { k ->
            Log.e("FW_INI", "$k = ${ini.getProperty(k)}")
        }

        val boardName         = ini.getProperty("BoardName",               "Unknown")
        val boardVersion      = ini.getProperty("BoardVersion",            "0").toInt()
        val boardSubVersion   = ini.getProperty("BoardSubVersion",         "0").toInt()
        val boardRevision     = ini.getProperty("BoardRevision",           "0").toInt()
        val boardSubRevision  = ini.getProperty("BoardSubRevision",        "0").toInt()
        val boardInstance     = ini.getProperty("BoardInstance",           "0").toInt()
        val boardType         = ini.getProperty("BoardType",               "0").toInt()
        val boardCode         = ini.getProperty("BoardCode",               "0").toInt()
        val boardAddInfoType  = ini.getProperty("BoardAdditionalInfoType", "0").toInt()
        val boardAddInfo      = ini.getProperty("BoardAdditionalInfo",     "0").toLong()

        val fwName            = ini.getProperty("FwName",                  "")
        val fwMajorVersion    = ini.getProperty("FwMajorVersion",          "0").toInt()
        val fwMinorVersion    = ini.getProperty("FwMinorVersion",          "0").toInt()
        val fwQuickFixVersion = ini.getProperty("FwQuickFix",               "0").toInt()
        val fwSinceLastTag    = ini.getProperty("FWSinceLastTag",          "0").toInt()

        val fwLabel           = ini.getProperty("FwLabel",                 "")
        val fwType            = ini.getProperty("FWType",                  "0").toInt()
        val fwCode            = ini.getProperty("FWCode",                  "0").toInt()

        val fwStartAddress    = ini.getProperty("FWStartAddress",          "0").toLong()
        val fwSize            = ini.getProperty("FWsize",                  "0").toLong()
        val fwCrc             = ini.getProperty("FWCRC",                   "0").toLong()

        val sdkMajorVersion   = ini.getProperty("SDKMajorVersion",         "0").toInt()
        val sdkMinorVersion   = ini.getProperty("SDKMinorVersion",         "0").toInt()
        val sdkQuickFixVersion= ini.getProperty("SDKQuickFix",             "0").toInt()
        val sdkSinceLastTag   = ini.getProperty("SDKSinceLastTag",         "0").toInt()

        val fwAddInfoType     = ini.getProperty("FWAdditionalInfoType",    "0").toInt()
        val fwAddInfo         = ini.getProperty("FWAdditionalInfo",        "0").toLong()

        Log.e("FW_NAME", "boardName = $boardName")
        Log.e("FW_NAME", "boardVersion = $boardVersion")
        Log.e("FW_NAME", "boardBuild = $boardInstance")

        fun fixedField(s: String, len: Int): ByteArray {
            val b = s.toByteArray(Charset.forName("UTF-8"))
            return ByteArray(len).apply {
                val n = b.size.coerceAtMost(len)
                System.arraycopy(b, 0, this, 0, n)
                if (n < len) this[n] = 0
            }
        }

        lastFwSize = fwSize
        lastFwCrc  = fwCrc
        val structSize = 120
        return ByteBuffer.allocate(structSize)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply {
                put(fixedField(boardName, 32))
                put(boardVersion.toByte())
                put(boardSubVersion.toByte())
                put(boardRevision.toByte())
                put(boardSubRevision.toByte())
                putShort(boardInstance.toShort())
                put(boardType.toByte())
                put(boardCode.toByte())

                put(boardAddInfoType.toByte())
                putInt(boardAddInfo.toInt())

                put(fixedField(fwName, 32))
                put(fwMajorVersion.toByte())
                put(fwMinorVersion.toByte())
                put(fwQuickFixVersion.toByte())
                put(fwSinceLastTag.toByte())

                put(fixedField(fwLabel, 16))
                put(fwType.toByte())
                put(fwCode.toByte())

                putInt(fwStartAddress.toInt())
                putInt(fwSize.toInt())
                putInt(fwCrc.toInt())

                put(sdkMajorVersion.toByte())
                put(sdkMinorVersion.toByte())
                put(sdkQuickFixVersion.toByte())
                put(sdkSinceLastTag.toByte())

                put(fwAddInfoType.toByte())
                putInt(fwAddInfo.toInt())
            }.array()
    }


}