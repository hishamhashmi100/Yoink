package io.github.hishamhashmi100.yoink.ui.home

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.hishamhashmi100.yoink.R
import io.github.hishamhashmi100.yoink.download.YouTubeUrl

/** For people who copy links instead of sharing them. [onSubmit] gets a link with a valid video ID. */
@Composable
fun AddLinkDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    val context = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }
    var showError by rememberSaveable { mutableStateOf(false) }

    fun submit() {
        if (YouTubeUrl.extractId(text) != null) onSubmit(text.trim()) else showError = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_link_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    showError = false
                },
                label = { Text(stringResource(R.string.add_link_hint)) },
                singleLine = true,
                isError = showError,
                supportingText = if (showError) {
                    { Text(stringResource(R.string.share_not_youtube)) }
                } else {
                    null
                },
                trailingIcon = {
                    IconButton(onClick = {
                        clipboardText(context)?.let {
                            text = it
                            showError = false
                        }
                    }) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = stringResource(R.string.paste))
                    }
                },
            )
        },
        confirmButton = { TextButton(onClick = ::submit) { Text(stringResource(R.string.next)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private fun clipboardText(context: Context): String? {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return null
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()
}
