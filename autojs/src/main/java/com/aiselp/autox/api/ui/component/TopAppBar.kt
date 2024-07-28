package com.aiselp.autox.api.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.Render

object TopAppBar : VueNativeComponent {
    override val tag: String = "TopAppBar"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val titleStr = element.props["title"]
        val title = element.findTemplate("title")
        val actions = element.findTemplate("actions")
        val navigationIcon = element.findTemplate("navigationIcon")
        TopAppBar(
            modifier = modifier,
            title = {
                if (title is ComposeElement) {
                    title.Render()
                } else if (titleStr is String) {
                    Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Text(text = titleStr)
                    }
                }
            }, actions = { actions?.Render() },
            navigationIcon = { navigationIcon?.Render() }
        )
    }
}