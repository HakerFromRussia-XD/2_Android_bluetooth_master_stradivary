package com.bailout.stickk.ubi4.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.data.local.repository.SETTINGS_PROFILE_NAME_MAX_LENGTH

private val SettingsProfileDialogBackground = Color(0xFF373737)
private val SettingsProfileDialogScreenBackground = Color(0xFF2A2A2A)
private val SettingsProfileDialogBorder = Color(0xFF444444)
private val SettingsProfileDialogText = Color(0xFFFCFCFC)
private val SettingsProfileDialogSecondaryText = Color(0xFF838383)
private val SettingsProfileDialogAccent = Color(0xFF0A84FF)

private val SettingsProfileDialogFontFamily = FontFamily(
    Font(R.font.sf_pro_display_light, weight = FontWeight.Normal),
    Font(R.font.sf_pro_text_bold, weight = FontWeight.Bold)
)

@Composable
fun SettingsProfileNameDialog(
    currentName: String,
    onDismissRequest: () -> Unit,
    onSave: (String) -> Unit,
    maxLength: Int = SETTINGS_PROFILE_NAME_MAX_LENGTH
) {
    var name by remember(currentName, maxLength) {
        mutableStateOf(currentName.take(maxLength))
    }
    val normalizedName = name.trim()
    val canSave = normalizedName.isNotEmpty() && normalizedName != currentName.trim()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        SettingsProfileNameDialogCard(
            title = stringResource(R.string.settings_profile_name_dialog_title),
            fieldLabel = stringResource(R.string.settings_profile_name_dialog_field_label),
            cancelLabel = stringResource(R.string.settings_profile_name_dialog_cancel),
            saveLabel = stringResource(R.string.settings_profile_name_dialog_save),
            name = name,
            maxLength = maxLength,
            canSave = canSave,
            requestFocus = true,
            onNameChange = { name = it.take(maxLength) },
            onCancel = onDismissRequest,
            onSave = { onSave(normalizedName) }
        )
    }
}

@Composable
private fun SettingsProfileNameDialogCard(
    title: String,
    fieldLabel: String,
    cancelLabel: String,
    saveLabel: String,
    name: String,
    maxLength: Int,
    canSave: Boolean,
    requestFocus: Boolean,
    onNameChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
        }
    }

    Surface(
        modifier = Modifier
            .width(270.dp),
        shape = RoundedCornerShape(16.dp),
        color = SettingsProfileDialogBackground,
        elevation = 12.dp
    ) {
        Column {
            Text(
                text = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                color = SettingsProfileDialogText,
                fontFamily = SettingsProfileDialogFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    label = {
                        Text(
                            text = fieldLabel,
                            fontFamily = SettingsProfileDialogFontFamily,
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = SettingsProfileDialogText,
                        fontFamily = SettingsProfileDialogFontFamily,
                        fontSize = 14.sp
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (canSave) {
                                onSave()
                            }
                        }
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = SettingsProfileDialogText,
                        cursorColor = SettingsProfileDialogText,
                        focusedBorderColor = SettingsProfileDialogBorder,
                        unfocusedBorderColor = SettingsProfileDialogBorder,
                        focusedLabelColor = SettingsProfileDialogSecondaryText,
                        unfocusedLabelColor = SettingsProfileDialogSecondaryText,
                        backgroundColor = SettingsProfileDialogScreenBackground
                    )
                )

                Text(
                    text = "${name.length}/$maxLength",
                    modifier = Modifier.align(Alignment.End),
                    color = SettingsProfileDialogSecondaryText,
                    fontFamily = SettingsProfileDialogFontFamily,
                    fontSize = 12.sp
                )
            }

            Divider(
                color = SettingsProfileDialogBorder,
                thickness = 1.dp
            )

            TextButton(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                enabled = canSave,
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = SettingsProfileDialogAccent,
                    disabledContentColor = SettingsProfileDialogSecondaryText
                )
            ) {
                Text(
                    text = saveLabel,
                    fontFamily = SettingsProfileDialogFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Divider(
                color = SettingsProfileDialogBorder,
                thickness = 1.dp
            )

            TextButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = SettingsProfileDialogAccent
                )
            ) {
                Text(
                    text = cancelLabel,
                    fontFamily = SettingsProfileDialogFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsProfileNameDialogPreview(
    title: String,
    fieldLabel: String,
    cancelLabel: String,
    saveLabel: String,
    initialName: String
) {
    var name by remember { mutableStateOf(initialName) }
    val canSave = name.trim().isNotEmpty() && name.trim() != initialName.trim()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsProfileDialogScreenBackground),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
        )
        SettingsProfileNameDialogCard(
            title = title,
            fieldLabel = fieldLabel,
            cancelLabel = cancelLabel,
            saveLabel = saveLabel,
            name = name,
            maxLength = SETTINGS_PROFILE_NAME_MAX_LENGTH,
            canSave = canSave,
            requestFocus = false,
            onNameChange = { name = it.take(SETTINGS_PROFILE_NAME_MAX_LENGTH) },
            onCancel = {},
            onSave = {}
        )
    }
}

@Preview(
    name = "Settings profile name dialog — RU",
    showBackground = true,
    backgroundColor = 0xFF2A2A2A,
    widthDp = 390,
    heightDp = 360
)
@Composable
private fun SettingsProfileNameDialogRuPreview() {
    SettingsProfileNameDialogPreview(
        title = "Переименовать профиль",
        fieldLabel = "Название профиля",
        cancelLabel = "Отмена",
        saveLabel = "Сохранить",
        initialName = "Профиль №1"
    )
}

@Preview(
    name = "Settings profile name dialog — EN",
    showBackground = true,
    backgroundColor = 0xFF2A2A2A,
    widthDp = 390,
    heightDp = 360
)
@Composable
private fun SettingsProfileNameDialogEnPreview() {
    SettingsProfileNameDialogPreview(
        title = "Rename profile",
        fieldLabel = "Profile name",
        cancelLabel = "Cancel",
        saveLabel = "Save",
        initialName = "Profile #1"
    )
}
