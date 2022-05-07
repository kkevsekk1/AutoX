package org.autojs.autojs.ui.project

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

class BuildViewModel(private val app: Application) : AndroidViewModel(app) {
    var scriptPath by mutableStateOf("")
    var outputPath by mutableStateOf("")
    var appName by mutableStateOf("")
    var packageName by mutableStateOf("")
    var versionName by mutableStateOf("1.0.0")
    var versionCode by mutableStateOf("0")
    var mainScriptFile by mutableStateOf("main.js")
    var isHideLauncher by mutableStateOf(false)
    var isStableMode by mutableStateOf(false)
    var isHideLogs by mutableStateOf(false)
    var isVolumeUpControl by mutableStateOf(false)
    var splashText by mutableStateOf("")
    var launcherIcon by mutableStateOf("")
    var serviceDesc by mutableStateOf("")
    var icon by mutableStateOf<String?>(null)
    var splashIcon by mutableStateOf<String?>(null)
    var iconDrawable by mutableStateOf<Bitmap?>(null)
    var splashIconDrawable by mutableStateOf<Bitmap?>(null)
    var appSignKeyPath by mutableStateOf("")
}