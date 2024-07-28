package com.aiselp.autox.api.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.Render

object Scaffold : VueNativeComponent {
    override val tag: String = "Scaffold"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val topBar = element.findTemplate("topBar")
        val bottomBar = element.findTemplate("bottomBar")
        val floatingActionButton = element.findTemplate("floatingActionButton")
        androidx.compose.material3.Scaffold(
            modifier = modifier,
            topBar = { topBar?.Render() },
            bottomBar = { bottomBar?.Render() },
            floatingActionButton = { floatingActionButton?.Render() }
        ) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                content()
            }
        }
    }

}