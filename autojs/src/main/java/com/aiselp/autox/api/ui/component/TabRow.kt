package com.aiselp.autox.api.ui.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.Render

object TabRow : VueNativeComponent {
    override val tag: String = "TabRow"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val selectedTabIndex = element.props["selectedTabIndex"] as? Int ?: 0
        val contentColor = parseColor(element.props["contentColor"])
        val containerColor = parseColor(element.props["containerColor"])
        val divider = element.findTemplate("divider")
        val type = element.props["type"] as? String

        when (type) {
            "scrollable" -> ScrollableTabRow(selectedTabIndex = selectedTabIndex,
                modifier = modifier,
                containerColor = containerColor ?: TabRowDefaults.primaryContainerColor,
                contentColor = contentColor ?: TabRowDefaults.primaryContentColor,
                divider = { divider?.Render() ?: HorizontalDivider() }) {
                content()
            }

            "primary" -> PrimaryTabRow(selectedTabIndex = selectedTabIndex,
                modifier = modifier,
                containerColor = containerColor ?: TabRowDefaults.primaryContainerColor,
                contentColor = contentColor ?: TabRowDefaults.primaryContentColor,
                divider = { divider?.Render() ?: HorizontalDivider() }) {
                content()
            }

            "primaryScrollable" -> PrimaryScrollableTabRow(selectedTabIndex = selectedTabIndex,
                modifier = modifier,
                containerColor = containerColor ?: TabRowDefaults.primaryContainerColor,
                contentColor = contentColor ?: TabRowDefaults.primaryContentColor,
                divider = { divider?.Render() ?: HorizontalDivider() }) {
                content()
            }

            "secondary" -> SecondaryTabRow(selectedTabIndex = selectedTabIndex,
                modifier = modifier,
                containerColor = containerColor ?: TabRowDefaults.primaryContainerColor,
                contentColor = contentColor ?: TabRowDefaults.primaryContentColor,
                divider = { divider?.Render() ?: HorizontalDivider() }) {
                content()
            }

            "secondaryScrollable" -> SecondaryScrollableTabRow(selectedTabIndex = selectedTabIndex,
                modifier = modifier,
                containerColor = containerColor ?: TabRowDefaults.primaryContainerColor,
                contentColor = contentColor ?: TabRowDefaults.primaryContentColor,
                divider = { divider?.Render() ?: HorizontalDivider() }) {
                content()
            }

            else -> TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = modifier,
                containerColor = containerColor ?: TabRowDefaults.primaryContainerColor,
                contentColor = contentColor ?: TabRowDefaults.primaryContentColor,
                divider = { divider?.Render() ?: HorizontalDivider() }
            ) {
                content()
            }
        }

    }
}