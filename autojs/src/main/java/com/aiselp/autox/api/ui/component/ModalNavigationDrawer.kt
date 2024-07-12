package com.aiselp.autox.api.ui.component

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.Render
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


object ModalNavigationDrawer : VueNativeComponent {
    override val tag: String = "ModalNavigationDrawer"
    private const val EVENT_ON_RENDER = "onRender"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val onRender = element.getEvent(EVENT_ON_RENDER)
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val drawerContent = element.findTemplate("drawerContent")
        val scope = rememberCoroutineScope()
        LaunchedEffect(Unit) {
            onRender?.invoke(JsDrawerState(scope, drawerState))
        }
        ModalNavigationDrawer(
            modifier = modifier,
            drawerContent = { drawerContent?.Render() },
            drawerState = drawerState,
            gesturesEnabled = element.props["gesturesEnabled"] as? Boolean ?: true
        ) {
            content()
        }
    }

    class JsDrawerState(val scope: CoroutineScope, val drawerState: DrawerState) {
        fun open() {
            scope.launch {
                drawerState.open()
            }
        }

        fun close() {
            scope.launch {
                drawerState.close()
            }
        }
        fun isOpen(): Boolean {
            return drawerState.isOpen
        }
    }

    @Composable
    private fun StateControl(drawerState: DrawerState, element: ComposeElement?) {
        if (element == null) return
        element.Render()
        LaunchedEffect(element.update) {
            val isOpen = element.props["isOpen"] as? Boolean
            if (isOpen != null) {
                if (isOpen) {
                    drawerState.open()
                } else {
                    drawerState.close()
                }
            }
        }
    }
}