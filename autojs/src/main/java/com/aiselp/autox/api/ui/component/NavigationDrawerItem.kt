package com.aiselp.autox.api.ui.component

import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.ComposeTextNode
import com.aiselp.autox.api.ui.Render

object NavigationDrawerItem : VueNativeComponent {
    override val tag: String = "NavigationDrawerItem"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val selected = element.props["selected"] as? Boolean
        NavigationDrawerItem(
            modifier = modifier,
            selected = selected ?: false,
            onClick = { element.getEvent("onClick")?.invoke() },
            icon = {
                element.findTemplate("icon")?.Render() ?: run {
                    element.props["icon"]?.let {
                        val el = ComposeElement("Icon")
                        el.props["src"] = it
                        el.Render()
                    }
                }
            },
            label = {
                element.findTemplate("label")?.Render() ?: run {
                    (element.props["label"] as? String)?.let {
                        ComposeTextNode(it).Render()
                    }
                }
            },
        )
    }

}