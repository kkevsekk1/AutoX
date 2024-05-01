package com.aiselp.autox.ui.material3.components

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun InputBox(
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    label: String? = null,
    maxLines: Int = 1,
) {
    OutlinedTextField(value = value,
        maxLines = maxLines,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label?.let {
            { Text(text = label) }
        })
}