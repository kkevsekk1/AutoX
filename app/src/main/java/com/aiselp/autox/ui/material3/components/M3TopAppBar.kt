package com.aiselp.autox.ui.material3.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3TopAppBar(title: String, content: @Composable () -> Unit) {
    TopAppBar(title = {
        Text(text = title)
    })
}