package org.autojs.autojs.ui.project

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.stardust.autojs.project.ProjectConfigKt
import org.autojs.autojs.build.ApkKeyStore

/**
 * @author wilinz
 * @date 2022/5/23
 */
class BuildViewModel(private val app: Application) : AndroidViewModel(app) {
    var sourcePath by mutableStateOf("")
    var outputPath by mutableStateOf("")
    var customOcrModelPath by mutableStateOf("")
    var appName by mutableStateOf("")
    var packageName by mutableStateOf("")
    var versionName by mutableStateOf("1.0.0")
    var versionCode by mutableStateOf("1")
    var mainScriptFile by mutableStateOf("main.js")
    var isHideLauncher by mutableStateOf(false)
    var isStableMode by mutableStateOf(false)
    var isHideLogs by mutableStateOf(false)
    var isVolumeUpControl by mutableStateOf(true)
    var splashText by mutableStateOf("")
    var launcherIcon by mutableStateOf("")
    var serviceDesc by mutableStateOf("")
    var icon by mutableStateOf<String?>(null)
    var splashIcon by mutableStateOf<String?>(null)
    var iconDrawable by mutableStateOf<Bitmap?>(null)
    var splashIconDrawable by mutableStateOf<Bitmap?>(null)
    var appSignKeyPath by mutableStateOf<String?>(null)
    var keyStore by mutableStateOf<ApkKeyStore?>(null)

    var isRequiredAccessibilityServices by mutableStateOf(true)
    var isRequiredBackgroundStart by mutableStateOf(false)
    var isRequiredDrawOverlay by mutableStateOf(false)

    var isRequiredOpenCv by mutableStateOf(false)
    var isRequiredOCR by mutableStateOf(false)
    var isRequired7Zip by mutableStateOf(false)
    var isRequiredTerminalEmulator by mutableStateOf(true)
    var isRequiredDefaultOcrModel by mutableStateOf(false)
    var projectConfig: ProjectConfigKt? = null
}