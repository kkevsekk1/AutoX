package org.autojs.autojs.ui.compose.widget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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

@Composable
fun MyAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    properties: DialogProperties = DialogProperties()
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirmClick) {
                Text(text = stringResource(id = R.string.ok))
            }
        },
        modifier = modifier,
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
        title = title,
        text = text,
        shape = shape,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        properties = properties
    )
}

@Composable
fun MyAlertDialog1(
    onDismissRequest: () -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    properties: DialogProperties = DialogProperties()
) {
    MyAlertDialog(
        onDismissRequest,
        modifier = modifier,
        title = title,
        text = text,
        buttons = {
            Row(Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(id = R.string.cancel))
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onConfirmClick) {
                    Text(text = stringResource(id = R.string.ok))
                }
            }
        },
        shape = shape,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        properties = properties
    )
}

@Composable
fun MyAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    buttons: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    properties: DialogProperties = DialogProperties()
) {
    MyDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = shape,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        properties = properties
    ) {
        title?.invoke()
        Spacer(modifier = Modifier.height(8.dp))
        text?.invoke()
        Spacer(modifier = Modifier.height(8.dp))
        buttons?.invoke()
    }
}

@Composable
fun MyDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    properties: DialogProperties = DialogProperties(),
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest, properties) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = backgroundColor,
            shape = shape,
            contentColor = contentColor
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                content()
            }
        }
    }
}