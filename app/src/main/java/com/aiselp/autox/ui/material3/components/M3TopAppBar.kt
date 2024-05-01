package com.aiselp.autox.ui.material3.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.autojs.autoxjs.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3TopAppBar(
    title: String,
    onNavigationClick: () -> Unit,
    actions: @Composable (RowScope.() -> Unit) = {},
) {
    TopAppBar(
        title = { Text(text = title) },
        actions = actions,
        navigationIcon = {
            IconButton(onClick = onNavigationClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.desc_back)
                )
            }
        })
}