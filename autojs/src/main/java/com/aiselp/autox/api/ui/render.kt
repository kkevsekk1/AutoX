package com.aiselp.autox.api.ui

import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
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
fun ComposeElement.Render(@SuppressLint("ModifierParameter") modifier: Modifier? = null) {
    if (this.status == ComposeElement.Status.Mounting) {
        Log.w(TAG, "Already mounted: $tag")
        return
    }
    this.status = ComposeElement.Status.Mounting
    val component = VueComponent.map[tag]
    if (component != null) {
        this.update
        val modifier1: Modifier = modifier ?: run {
            var modifier1 = this.modifier
            modifierExts.forEach {
                modifier1 = it.Ext.invoke(modifier1)
            }
            return@run modifier1
        }
        component.Render(modifier1, this) {
            children.forEach {
                it.Render()
            }
        }
    } else {
        Log.w(TAG, "Unknown tag: $tag")
    }
    this.status = ComposeElement.Status.Mounted
}

@Composable
fun RowScope.RenderRow(element: ComposeElement) {
    var modifier = element.modifier
    element.modifierExts.forEach {
        modifier = it.RowExt.invoke(this, modifier)
    }
    element.Render(modifier)
}

@Composable
fun ColumnScope.RenderColumn(element: ComposeElement) {
    var modifier = element.modifier
    element.modifierExts.forEach {
        modifier = it.ColumnExt.invoke(this, modifier)
    }
    element.Render(modifier)
}
