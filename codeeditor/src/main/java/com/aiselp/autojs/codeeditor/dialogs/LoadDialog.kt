package com.aiselp.autojs.codeeditor.dialogs

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.aiselp.autox.ui.material3.components.BaseDialog
import com.aiselp.autox.ui.material3.components.DialogController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoadDialog : DialogController(
    DialogProperties(
        dismissOnBackPress = false,
        dismissOnClickOutside = false
    )
) {
    var title: String by mutableStateOf("加载中")
    var content by mutableStateOf("")


    suspend fun setContent(text: String) {
        withContext(Dispatchers.Main) {
            content = text
        }
    }
    @Composable
    fun Dialog() {
        val scope = rememberCoroutineScope()
        BaseDialog(
            onDismissRequest = { scope.launch { dismiss() } },
            title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = content)
            }
        }
    }
}
