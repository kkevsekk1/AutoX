package org.autojs.autojs.ui.main.components

import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.stardust.util.IntentUtil
import org.autojs.autojs.ui.log.LogActivityKt
import org.autojs.autojs.ui.widget.WebDataKt
import org.autojs.autoxjs.R

//主界面日志按钮
@Composable
fun LogButton() {
    val context = LocalContext.current
    IconButton(onClick = { LogActivityKt.start(context) }) {
        Icon(
            painter = painterResource(id = R.drawable.ic_logcat),
            contentDescription = stringResource(id = R.string.text_logcat)
        )
    }
}

//文档界面菜单按钮
@Composable
fun DocumentPageMenuButton(getWebView:()-> WebView) {
    val context = LocalContext.current
    Box {
        var expanded by remember { mutableStateOf(false) }
        fun dismissMenu() {
            expanded = false
        }
        IconButton({ expanded = true }) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }) {
            DropdownMenuItem(onClick = {
                dismissMenu()
                getWebView().loadUrl(WebDataKt.homepage)
            }) {
                Icon(Icons.Default.Home, contentDescription = null)
                Text(text = "回到主页")
            }
            DropdownMenuItem(onClick = {
                dismissMenu()
                getWebView().url?.let {
                    IntentUtil.browse(context, it)
                }
            }) {
                Icon(
                    painterResource(id = R.drawable.ic_external_link),
                    contentDescription = null
                )
                Text(text = stringResource(id = R.string.text_browser_open))
            }
            DropdownMenuItem(onClick = {
                dismissMenu()
                getWebView().clearCache(false)
                getWebView().reload()
            }) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text(text = "刷新")
            }
        }
    }
}