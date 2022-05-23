package org.autojs.autojs.ui.widget

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.SwipeRefreshState

/**
 * @author wilinz
 * @date 2022/5/23
 */
@Composable
fun MySwipeRefresh(
    state: SwipeRefreshState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    swipeEnabled: Boolean = true,
    refreshTriggerDistance: Dp = 80.dp,
    indicatorAlignment: Alignment = Alignment.TopCenter,
    indicatorPadding: PaddingValues = PaddingValues(0.dp),
    indicator: @Composable (state: SwipeRefreshState, refreshTrigger: Dp) -> Unit = { s, trigger ->
        SwipeRefreshIndicator(
            s, trigger,
            contentColor = MaterialTheme.colors.primary
        )
    },
    clipIndicatorToPadding: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    SwipeRefresh(
        state = state,
        onRefresh = onRefresh,
        modifier = modifier,
        swipeEnabled = swipeEnabled,
        refreshTriggerDistance = refreshTriggerDistance,
        indicatorAlignment = indicatorAlignment,
        indicatorPadding = indicatorPadding,
        indicator = indicator,
        clipIndicatorToPadding = clipIndicatorToPadding,
        content = content
    )
}