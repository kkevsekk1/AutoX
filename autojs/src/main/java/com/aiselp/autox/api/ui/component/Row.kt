package com.aiselp.autox.api.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.RenderRow

object Row : VueNativeComponent {
    override val tag: String = "row"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = parseVerticalAlignment(element.props["verticalAlignment"] as? String),
            horizontalArrangement = parseHorizontalArrangement(element.props["horizontalArrangement"] as? String)
        ) {
            element.children.forEach {
                parseFloat(it.props["weight"])?.let { weight ->
                    it.modifier = it.modifier.weight(weight)
                }
                RenderRow(it)
            }
        }
    }
}