package com.aiselp.autox.api.ui.component

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.aiselp.autox.api.ui.ComposeElement

object View : VueNativeComponent {
    override val tag: String = "View"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val view = element.props["view"] as? View
        val update = element.getEvent("update")
        if (update != null) {
            AndroidView(modifier = modifier, factory = {
                view ?: View(it)
            }, update = { update.invoke(it) })
        } else {
            AndroidView(modifier = modifier, factory = {
                view ?: View(it)
            })
        }
    }

}