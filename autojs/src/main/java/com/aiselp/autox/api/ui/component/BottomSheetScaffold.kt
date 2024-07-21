package com.aiselp.autox.api.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.Render

object BottomSheetScaffold : VueNativeComponent {
    override val tag: String = "BottomSheetScaffold"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val scaffoldState = rememberBottomSheetScaffoldState()
        val sheetPeekHeight = parseFloat(element.props["sheetPeekHeight"])
        val sheetMaxWidth = parseFloat(element.props["sheetMaxWidth"])
        val sheetContainerColor = parseColor(element.props["sheetContainerColor"])
        val sheetSwipeEnabled = element.props["sheetSwipeEnabled"] as? Boolean
        val topBar = element.findTemplate("topBar")
        val sheetTonalElevation = parseFloat(element.props["sheetTonalElevation"])
        val sheetShadowElevation = parseFloat(element.props["sheetShadowElevation"])
        LaunchedEffect(Unit) {
            element.getEvent("onRender")?.invoke(scaffoldState)
        }
        BottomSheetScaffold(
            modifier = modifier,
            sheetContent = { element.findTemplate("sheetContent")?.Render() },
            scaffoldState = scaffoldState,
            sheetPeekHeight = sheetPeekHeight?.dp ?: BottomSheetDefaults.SheetPeekHeight,
            sheetMaxWidth = sheetMaxWidth?.dp ?: BottomSheetDefaults.SheetMaxWidth,
            sheetContainerColor = sheetContainerColor ?: BottomSheetDefaults.ContainerColor,
            sheetSwipeEnabled = sheetSwipeEnabled ?: true,
            topBar = topBar?.let { { it.Render() } },
            sheetTonalElevation = sheetTonalElevation?.dp ?: BottomSheetDefaults.Elevation,
            sheetShadowElevation = sheetShadowElevation?.dp ?: BottomSheetDefaults.Elevation,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                content()
            }
        }
    }

}