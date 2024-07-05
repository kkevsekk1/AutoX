package com.aiselp.autox.api.ui

import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.component.VueComponent
import com.aiselp.autox.ui.material3.theme.AppTheme

private const val TAG = "VueRender"
fun AppCompatActivity.render(element: ComposeElement) {
    setContent {
        AppTheme {
            element.Render()
        }
    }
}

@Composable
fun ComposeElement.Render() {
    if (this.status == ComposeElement.Status.Mounting){
        Log.w(TAG, "Already mounted: $tag")
        return
    }
    this.status = ComposeElement.Status.Mounting
    val component = VueComponent.map[tag]
    if (component != null) {
        this.update
        val modifier: Modifier = this.modifier
        component.Render(modifier, this) {
            children.forEach {
                it.Render()
            }
        }
    } else {
        Log.w(TAG, "Unknown tag: $tag")
    }
    this.status = ComposeElement.Status.Mounted
}