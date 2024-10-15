package com.aiselp.autox.ui.material3.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSingleChoiceInputBox(
    label: String,
    options: List<String>,
    selected: Int = 0,
    onSelected: (Int) -> Unit
) {
    var state by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(options.getOrNull(selected) ?: "") }
    ExposedDropdownMenuBox(
        expanded = state,
        onExpandedChange = { state = it },
    ) {
        OutlinedTextField(
            modifier = Modifier.menuAnchor(),
            value = text,
            label = { Text(text = label) },
            singleLine = true,
            readOnly = true, onValueChange = {},
            suffix = {
                Icon(
                    modifier = Modifier.rotate(if (state) 180f else 0f),
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }
        )
        ExposedDropdownMenu(expanded = state, onDismissRequest = { state = false }) {
            options.forEachIndexed { index, value ->
                DropdownMenuItem(text = { Text(text = value) }, onClick = {
                    state = false
                    text = options[index]
                    onSelected(index)
                })
            }
        }
    }
}