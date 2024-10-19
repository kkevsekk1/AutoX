package com.aiselp.autox.ui.material3.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp


@Composable
fun SettingOptionSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier.clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            it.invoke()
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(text = title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingOptionSwitch(
    title: String,
    value: MutableState<Boolean>,
    icon: ImageVector? = null,
) {
    SettingOptionSwitch(title, value.value, { value.value = it }, icon?.let {
        { Icon(imageVector = it, contentDescription = null) }
    })
}

@Composable
fun SettingOptionSwitch(
    title: String,
    icon: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingOptionSwitch(title, checked, onCheckedChange, icon?.let {
        { Icon(imageVector = it, contentDescription = null) }
    })
}