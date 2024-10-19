package com.aiselp.autox.ui.material3.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope


@Composable
fun <T> Watch(
    value: MutableState<T>,
    block: suspend CoroutineScope.(newValue: T) -> Unit
) {
    val init = remember { mutableStateOf(false) }
    LaunchedEffect(value.value) {
        if (!init.value) {
            init.value = true
            return@LaunchedEffect
        }
        block(value.value)
    }
}