package com.aiselp.autox.api.ui.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.Render
import com.aiselp.autox.api.ui.RenderColumn

object ExposedDropdownMenuBox : VueNativeComponent {
    override val tag: String = "ExposedDropdownMenuBox"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val expanded = element.props["expanded"] as? Boolean ?: false
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { element.getEvent("onExpandedChange")?.invoke(it) },
            modifier = modifier
        ) {
            element.children.find {
                it.tag == OutlinedTextField.tag || it.tag == TextField.tag
            }?.let {
                it.modifier = it.modifier.menuAnchor()
                it.Render()
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    element.getEvent("onDismissRequest")?.invoke()
                        ?: element.getEvent("onExpandedChange")?.invoke(false)
                },
            ) {
                element.children.find { it.tag == "menu" }?.children?.forEach {
                    RenderColumn(element = it)
                }
            }
        }
    }

}