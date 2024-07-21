package com.aiselp.autox.api.ui.component

import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.RenderColumn

object ModalBottomSheet : VueNativeComponent {
    override val tag: String = "ModalBottomSheet"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val onDismissRequest = element.getEvent("onDismissRequest")
        val tonalElevation = parseFloat(element.props["tonalElevation"])
        val sheetMaxWidth = parseFloat(element.props["sheetMaxWidth"])
        ModalBottomSheet(
            onDismissRequest = { onDismissRequest?.invoke() },
            modifier = modifier,
            sheetMaxWidth = sheetMaxWidth?.dp ?: BottomSheetDefaults.SheetMaxWidth,
            tonalElevation = tonalElevation?.dp ?: BottomSheetDefaults.Elevation,
        ) {
            element.children.forEach {
                RenderColumn(element = it)
            }
        }
    }

}