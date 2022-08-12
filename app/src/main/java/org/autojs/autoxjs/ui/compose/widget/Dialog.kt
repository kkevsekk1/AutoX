package org.autojs.autoxjs.ui.compose.widget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.autojs.autoxjs.R

@Composable
fun AskSaveDialog(
    isShowDialog: Boolean,
    onDismissRequest: () -> Unit,
    onConfirmClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    if (isShowDialog) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = stringResource(id = R.string.text_alert)) },
            text = {
                Text(text = stringResource(id = R.string.edit_exit_without_save_warn))
            },
            buttons = {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismissClick) {
                        Text(text = stringResource(id = R.string.text_exit_directly))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onConfirmClick) {
                        Text(text = stringResource(id = R.string.text_save_and_exit))
                    }
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(id = R.string.text_cancel))
                    }
                }
            }
        )
    }
}

@Composable
fun ProgressDialog(
    isShowDialog: Boolean,
    onDismissRequest: () -> Unit,
    text: @Composable() () -> Unit
) {
    if (isShowDialog) {
        Dialog(

            onDismissRequest = onDismissRequest,
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.width(16.dp))
                    text()
                }
            }
        }
    }
}