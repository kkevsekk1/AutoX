package com.aiselp.autox.ui.material3.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class DialogController(
    val properties: DialogProperties = DialogProperties(),
    initShow: Boolean = false,
) {
    var showState by mutableStateOf(initShow)
        protected set

    open fun onShow() {}
    suspend fun show() = withContext(Dispatchers.Main) {
        showState = true
        onShow()
    }

    open fun onPositiveClick() {}
    open fun onNegativeClick() {}
    open fun onNeutralClick() {}

    open fun onDismiss() {}
    suspend fun dismiss() = withContext(Dispatchers.Main) {
        showState = false
        onDismiss()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogController.BaseDialog(
    onDismissRequest: () -> Unit,
    title: @Composable RowScope.() -> Unit,
    positiveText: String? = null,
    onPositiveClick: (() -> Unit)? = null,
    negativeText: String? = null,
    onNegativeClick: (() -> Unit)? = null,
    neutralText: String? = null,
    onNeutralClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {

    if (!showState) return
    BasicAlertDialog(
        onDismissRequest = onDismissRequest, properties = properties
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) { title() }
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.heightIn(max = 500.dp)) { content() }
                Spacer(modifier = Modifier.height(16.dp))
                if (positiveText == null && negativeText == null && neutralText == null) {
                    Spacer(modifier = Modifier.height(16.dp))
                } else Row(verticalAlignment = Alignment.CenterVertically) {
                    neutralText?.let {
                        TextButton(
                            onClick = onNeutralClick ?: this@BaseDialog::onNeutralClick
                        ) {
                            Text(text = it)
                        }
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        negativeText?.let {
                            TextButton(
                                onClick = onNegativeClick ?: this@BaseDialog::onNegativeClick
                            ) {
                                Text(text = it)
                            }
                        }
                        positiveText?.let {
                            TextButton(
                                onClick = onPositiveClick ?: this@BaseDialog::onPositiveClick
                            ) {
                                Text(text = it)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialogController.AlertDialog(
    title: String,
    positiveText: String? = null,
    onPositiveClick: (() -> Unit)? = null,
    negativeText: String? = null,
    onNegativeClick: (() -> Unit)? = null,
    neutralText: String? = null,
    onNeutralClick: (() -> Unit)? = null,
    content: String,
) {
    val scope = rememberCoroutineScope()
    fun d() {
        scope.launch { dismiss() }
    }
    BaseDialog(onDismissRequest = { d();onDismiss() },
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        positiveText = positiveText,
        onPositiveClick = onPositiveClick ?: { d();onPositiveClick() },
        negativeText = negativeText,
        onNegativeClick = onNegativeClick ?: { d();onNegativeClick() },
        neutralText = neutralText,
        onNeutralClick = onNeutralClick ?: { d();onNegativeClick() },
        content = { Text(text = content) })
}