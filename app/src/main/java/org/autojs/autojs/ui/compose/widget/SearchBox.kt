package org.autojs.autojs.ui.compose.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun SearchBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable() () -> Unit = {},
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colors.primary),
        modifier = modifier
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            decorationBox = { InnerTextField ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    Box {
                        InnerTextField()
                        if (value.isEmpty()) {
                            placeholder()
                        }
                    }
                }
            },
            cursorBrush = SolidColor(MaterialTheme.colors.primary),
            modifier = Modifier.padding(8.dp),
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions
        )
    }
}

@Composable
fun SearchBox2(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable() () -> Unit = {},
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        decorationBox = { InnerTextField ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Box {
                    InnerTextField()
                    if (value.isEmpty()) {
                        placeholder()
                    }
                }
            }
        },
        cursorBrush = SolidColor(MaterialTheme.colors.onPrimary),
        modifier = modifier.padding(8.dp),
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle= TextStyle(color = MaterialTheme.colors.onPrimary)
    )
}