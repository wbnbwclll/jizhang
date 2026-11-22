package com.wbnbwclll.voiceledger.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.wbnbwclll.voiceledger.R
import com.wbnbwclll.voiceledger.speech.VoiceInputController

@Composable
fun VoiceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onVoiceText: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    onContinue: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val controller = remember(context) { VoiceInputController(context) }
    val voice by controller.state.collectAsState()
    val currentOnVoiceText by rememberUpdatedState(onVoiceText)
    var focused by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionDenied = !granted
        if (granted) controller.start { currentOnVoiceText(it) }
    }

    DisposableEffect(controller) {
        onDispose { controller.release() }
    }

    val colors = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.bodyLarge
    val canContinue = onContinue != null && value.isNotBlank() &&
        !voice.isRecording && !voice.isLoadingModel
    val continueEntry = {
        if (canContinue) {
            focus.clearFocus()
            keyboard?.hide()
            onContinue?.invoke()
        }
        Unit
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.hasFocus }
                .semantics { contentDescription = label },
            textStyle = textStyle.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            maxLines = 3,
            keyboardOptions = KeyboardOptions(
                imeAction = if (onContinue == null) ImeAction.Default else ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { continueEntry() }),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (focused) 2.dp else 1.dp,
                            color = if (focused) colors.primary else colors.outline,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(start = 14.dp, end = 4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty()) {
                            Text(placeholder, style = textStyle, color = colors.onSurfaceVariant)
                        }
                        innerTextField()
                    }
                    if (onContinue != null && value.isNotBlank()) {
                        TextButton(onClick = continueEntry, enabled = canContinue) {
                            Text(stringResource(R.string.quick_entry_continue))
                        }
                    }
                    IconButton(
                        enabled = !voice.isLoadingModel,
                        onClick = {
                            if (voice.isRecording) {
                                controller.stop()
                            } else {
                                focus.clearFocus()
                                keyboard?.hide()
                                if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                                    PackageManager.PERMISSION_GRANTED
                                ) {
                                    permissionDenied = false
                                    controller.start { currentOnVoiceText(it) }
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                    ) {
                        if (voice.isLoadingModel) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                painter = painterResource(
                                    if (voice.isRecording) R.drawable.ic_voice_stop else R.drawable.ic_microphone
                                ),
                                contentDescription = stringResource(
                                    if (voice.isRecording) R.string.voice_stop else R.string.voice_start
                                ),
                                tint = if (voice.isRecording) colors.error else colors.primary
                            )
                        }
                    }
                }
            }
        )

        val message = when {
            voice.isLoadingModel -> stringResource(R.string.voice_loading)
            voice.isRecording -> voice.partialText.ifBlank { stringResource(R.string.voice_recording) }
            permissionDenied -> stringResource(R.string.voice_permission_denied)
            else -> voice.message
        }
        if (message != null) {
            Text(
                text = message,
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = if (voice.isRecording || voice.isLoadingModel) colors.onSurfaceVariant else colors.error
            )
        }
    }
}
