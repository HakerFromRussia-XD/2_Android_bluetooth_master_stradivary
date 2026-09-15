package com.bailout.stickk.ubi4.versions.v3.presentation.service

import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoField

data class V3ServiceTextInputUiState(
    val text: String = "",
    val canSend: Boolean = false,
    /** Changes only when the previous UI would explicitly move the cursor to the end. */
    val cursorRevision: Long = 0,
)

enum class V3TextInputMessage { LIMIT_REACHED, ENTER_TEXT, SENT, PREPARATION_FAILED }
data class V3TextInputFeedback(val id: Long, val field: V3DeviceInfoField, val message: V3TextInputMessage)
