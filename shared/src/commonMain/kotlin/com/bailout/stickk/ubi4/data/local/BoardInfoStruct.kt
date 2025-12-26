package com.bailout.stickk.ubi4.data.local

import com.bailout.stickk.ubi4.data.BoardInfoStructSerializer
import kotlinx.serialization.Serializable

@Serializable(with = BoardInfoStructSerializer::class)
data class BoardInfoStruct(
    val boardName: String = "",
    val boardVersion: Int = 0,
    val boardSubVersion: Int = 0,
    val boardRev: Int = 0,
    val boardSubRev: Int = 0,
    val boardBuild: Int = 0,
    val boardType: Int = 0,
    val boardCode: Int = 0,
    val boardAdditionalInfoType: Int = 0,
    val boardAdditionalInfo: Long = 0
) {
    val boardVersionString: String
        get() = "$boardVersion.$boardSubVersion.$boardBuild"
}