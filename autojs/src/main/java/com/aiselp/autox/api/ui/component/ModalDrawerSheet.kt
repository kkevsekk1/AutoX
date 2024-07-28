package com.aiselp.autox.api.ui.component

import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement

object ModalDrawerSheet : VueNativeComponent {
    override val tag: String = "ModalDrawerSheet"

    @Composable
    override fun Render(
        modifier: Modifier, element: ComposeElement, content: @Composable () -> Unit
    ) {
        ModalDrawerSheet(
            modifier = modifier,
        ) {
            content()
        }
    }
}