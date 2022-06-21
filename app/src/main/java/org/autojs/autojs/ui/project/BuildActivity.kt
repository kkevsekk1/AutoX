package org.autojs.autojs.ui.project

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.google.accompanist.appcompattheme.AppCompatTheme
import com.stardust.autojs.project.ProjectConfigKt
import com.stardust.autojs.project.ProjectConfigKt.Companion.configFileOfDir
import com.stardust.autojs.project.ProjectConfigKt.Companion.fromProjectDir
import com.stardust.pio.PFiles
import com.stardust.util.IntentUtil
import kotlinx.coroutines.*
import org.autojs.autojs.Pref
import org.autojs.autojs.R
import org.autojs.autojs.autojs.build.ApkBuilder
import org.autojs.autojs.autojs.build.ApkBuilder.AppConfig
import org.autojs.autojs.autojs.build.ApkBuilder.AppConfig.Companion.fromProjectConfig
import org.autojs.autojs.build.ApkBuilderPluginHelper
import org.autojs.autojs.build.ApkKeyStore
import org.autojs.autojs.build.ApkSigner
import org.autojs.autojs.external.fileprovider.AppFileProvider
import org.autojs.autojs.model.explorer.ExplorerFileItem
import org.autojs.autojs.model.explorer.Explorers
import org.autojs.autojs.model.script.ScriptFile
import java.io.File

/**
 * Modified by wilinz on 2022/5/23
 */
open class BuildActivity : ComponentActivity(), ApkBuilder.ProgressCallback {

    //    private var projectConfig: ProjectConfigKt? = null
    private var progressDialog: MaterialDialog? = null
    private var source: String? = null
    private var directory: String? = null


    // 单文件打包清爽模式
    private var singleBuildCleanMode = true

    private val viewModel: BuildViewModel by viewModels()

    companion object {
        @JvmField
        val EXTRA_SOURCE = BuildActivity::class.java.name + ".extra_source_file"
        const val TAG = "BuildActivity"
        private val REGEX_PACKAGE_NAME =
            Regex("^([A-Za-z][A-Za-z\\d_]*\\.)+([A-Za-z][A-Za-z\\d_]*)$")

        fun getIntent(context: Context): Intent {
            return Intent(context, BuildActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()

        setContent {
            AppCompatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    BuildPage()
                }
            }
        }
    }

    fun init() {
        source = intent.getStringExtra(EXTRA_SOURCE)
        singleBuildCleanMode = Pref.isSingleBuildCleanModeEnabled()
        if (source != null) {
            directory = source
            setupWithSourceFile(ScriptFile(source))
        }
    }

    private fun setupWithSourceFile(file: ScriptFile) {
        var dir: String = file.parent ?: ""
        if (dir.startsWith(filesDir.path)) {
            dir = Pref.getScriptDirPath() ?: ""
        }
        viewModel.outputPath = dir
        viewModel.appName = file.simplifiedName
        viewModel.packageName = getString(
            R.string.format_default_package_name,
            System.currentTimeMillis()
        )
        setSource(file)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    private fun setSource(file: File) {
        if (file.isFile) {
            viewModel.sourcePath = file.path
            directory = file.parent
            viewModel.projectConfig = fromProjectDir(directory!!, sourceJsonName)
            viewModel.outputPath = directory!!
        } else {
            viewModel.projectConfig = fromProjectDir(file.path)
            viewModel.outputPath = File(source, viewModel.projectConfig!!.buildDir).path
        }
        viewModel.projectConfig?.let { config ->
            viewModel.apply {
                sourcePath = File(source, "").path
                appName = config.name ?: ""
                packageName = config.packageName ?: ""
                versionName = config.versionName ?: ""
                versionCode = config.versionCode.toString()
                icon = config.icon
                mainScriptFile = config.mainScriptFile ?: ""
                isStableMode = config.launchConfig.isStableMode
                isHideLauncher = config.launchConfig.isHideLauncher
                isHideLogs = config.launchConfig.isHideLogs
                isVolumeUpControl = config.launchConfig.isVolumeUpControl
                splashText = config.launchConfig.splashText
                serviceDesc = config.launchConfig.serviceDesc
                splashIcon = config.launchConfig.splashIcon
                appSignKeyPath = config.signingConfig.keyStore
            }
            // 签名相关
            val signConfig = config.signingConfig
            if (signConfig.keyStore.isNotEmpty()) {
                viewModel.appSignKeyPath = signConfig.keyStore
                viewModel.keyStore = ApkSigner.loadApkKeyStore(signConfig.keyStore)
            }

        }
    }

    /**
     * 从viewModel保存配置
     */
    private fun syncProjectConfig(): Boolean {
        if (viewModel.projectConfig == null) {
            viewModel.projectConfig = ProjectConfigKt()
            if (!source.isNullOrEmpty() && PFiles.isFile(source)) {
                viewModel.projectConfig!!.mainScriptFile = File(source).name
            }
        }

        //请求权限列表
        val permissionList = mutableListOf<String>()
        if (viewModel.isRequiredAccessibilityServices) permissionList.add(Constant.Permissions.ACCESSIBILITY_SERVICES)
        if (viewModel.isRequiredBackgroundStart) permissionList.add(Constant.Permissions.BACKGROUND_START)
        if (viewModel.isRequiredDrawOverlay) permissionList.add(Constant.Permissions.DRAW_OVERLAY)

        viewModel.projectConfig?.apply {
            name = viewModel.appName
            versionCode = viewModel.versionCode.toInt()
            versionName = viewModel.versionName
            packageName = viewModel.packageName
            mainScriptFile = viewModel.mainScriptFile
            launchConfig.apply {
                isStableMode = viewModel.isStableMode
                isHideLauncher = viewModel.isHideLauncher
                isHideLogs = viewModel.isHideLogs
                isVolumeUpControl = viewModel.isVolumeUpControl
                splashText = viewModel.splashText
                serviceDesc = viewModel.serviceDesc
                permissions = permissionList
            }
            viewModel.keyStore?.let { apkKeyStore ->
                apkKeyStore.path?.let { signingConfig.keyStore = it }
                apkKeyStore.alias?.let { signingConfig.alias = it }
            }
        }

        return true
    }

    suspend fun saveIcon(bitmap: Bitmap): String? {
        viewModel.projectConfig?.let {
            val iconPath = withContext(Dispatchers.IO) {
                val iconPath = viewModel.projectConfig?.icon ?: "logo"
                // 没有开启清爽模式或者不是单文件
                if (!singleBuildCleanMode || !isSingleFile) {
                    val iconFile = File(directory, iconPath)
                    PFiles.ensureDir(iconFile.path)
                    iconFile.outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                }
                iconPath
            }
            if (!singleBuildCleanMode || !isSingleFile) {
                viewModel.projectConfig!!.icon = iconPath
            }
            return iconPath
        }
        return null
    }

    suspend fun saveSplashIcon(bitmap: Bitmap): String? {
        viewModel.projectConfig?.let {
            val iconPath = withContext(Dispatchers.IO) {
                val iconPath = viewModel.projectConfig?.icon ?: getSourceIconPath("splashIcon")
                // 没有开启清爽模式或者不是单文件
                if (!singleBuildCleanMode || !isSingleFile) {
                    val iconFile = File(directory, iconPath)
                    PFiles.ensureDir(iconFile.path)
                    iconFile.outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                }
                iconPath
            }
            if (!singleBuildCleanMode || !isSingleFile) {
                viewModel.projectConfig!!.launchConfig.splashIcon = iconPath
            }
            return iconPath
        }
        return null
    }

    private fun getSourceIconPath(iconName: String): String = if (isSingleFile) {
        "res/" + iconName + "_" + PFiles.getNameWithoutExtension(source) + ".png"
    } else "res/$iconName.png"

    private suspend fun syncAndBuild() {
        // 同步配置
        if (syncProjectConfig()) {
            viewModel.iconDrawable?.let { saveIcon(it) }
            viewModel.splashIconDrawable?.let { saveSplashIcon(it) }
            writeProjectConfigAndRefreshView()
            doBuildingApk()
        }
    }

    private fun writeProjectConfigAndRefreshView() {
        if (!singleBuildCleanMode || !isSingleFile) {
            PFiles.write(
                configFileOfDir(directory!!, sourceJsonName),
                viewModel.projectConfig!!.toJson()
            )
        }
        val item = ExplorerFileItem(source, null)
        Explorers.workspace().notifyItemChanged(item, item)
    }

    // 单文件打包时，不一样的json文件名
    private val sourceJsonName: String
        get() = if (isSingleFile) {
            PFiles.getNameWithoutExtension(source) + ".json"
        } else ProjectConfigKt.CONFIG_FILE_NAME

    private val isSingleFile: Boolean
        get() = if (source != null) {
            source!!.endsWith(".js")
        } else false

    private fun checkPackageNameValid(text: String): Boolean =
        !REGEX_PACKAGE_NAME.matches(text)

    @SuppressLint("CheckResult")
    private suspend fun doBuildingApk() {
        val appConfig = createAppConfig()
        Log.d(TAG, "doBuildingApk: $appConfig")
        println(cacheDir)
        val tmpDir = File(cacheDir, "build/")
        val outApk = File(
            viewModel.outputPath,
            String.format("%s_v%s.apk", appConfig.appName, appConfig.versionName)
        )
        showProgressDialog()
        try {
            withContext(Dispatchers.IO) {
                callApkBuilder(tmpDir, outApk, appConfig, viewModel.projectConfig)
            }
            onBuildSuccessful(outApk)
        } catch (e: Exception) {
            onBuildFailed(e)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun buildApk(apkKeyStore: ApkKeyStore?) {
        if (!checkInputs(viewModel)) {
            return
        }
        if (apkKeyStore != null && !apkKeyStore.isVerified) {
            showPasswordInputDialog(
                context = this,
                keyStore = apkKeyStore,
                chooseDialog = null,
                onComplete = {
                    GlobalScope.launch(Dispatchers.Main) {
                        syncAndBuild()
                    }
                }
            )
            return
        }
        GlobalScope.launch(Dispatchers.Main) {
            syncAndBuild()
        }
    }

    private fun checkInputs(viewModel: BuildViewModel): Boolean {
        var inputValid = true
        inputValid = inputValid and viewModel.sourcePath.isNotEmpty()
        inputValid = inputValid and viewModel.outputPath.isNotEmpty()
        inputValid = inputValid and viewModel.appName.isNotEmpty()
        inputValid = inputValid and viewModel.versionCode.isNotEmpty()
        inputValid = inputValid and viewModel.versionName.isNotEmpty()
        inputValid = inputValid and viewModel.packageName.isNotEmpty()
        return inputValid
    }

    /**
     * 从viewModel创建配置
     */
    private fun createAppConfig(): AppConfig {
        val excludeLibraries = mutableListOf<String>()
        if (!viewModel.isRequiredOpenCv) excludeLibraries.add(Constant.Libraries.OPEN_CV)
        if (!viewModel.isRequiredOCR) excludeLibraries.add(Constant.Libraries.OCR)
        if (!viewModel.isRequired7Zip) excludeLibraries.add(Constant.Libraries.`7ZIP`)
        if (!viewModel.isRequiredTerminalEmulator) excludeLibraries.add(Constant.Libraries.TERMINAL_EMULATOR)

        val excludeAssets = mutableListOf<String>()
        if (!viewModel.isRequiredDefaultOcrModel) {
            excludeAssets.add(Constant.Assets.OCR_MODELS + "/ocr_v2_for_cpu")
            excludeAssets.add(Constant.Assets.OCR_MODELS + "/ocr_v2_for_cpu(slim)")
        }

        if (viewModel.projectConfig != null) {
            var appConfig = fromProjectConfig(source!!, viewModel.projectConfig!!)
            // 设置了logo/splashIcon没有保存对应文件的时候
            appConfig.excludeLibraries = excludeLibraries
            appConfig.excludeAssets = excludeAssets
            appConfig.customOcrModelPath = viewModel.customOcrModelPath
            if (appConfig.icon == null) {
                viewModel.iconDrawable?.let {
                    appConfig = appConfig.setIcon { it }
                }
            }
            if (appConfig.splashIcon == null) {
                viewModel.splashIconDrawable?.let {
                    appConfig = appConfig.setSplashIcon { it }
                }
            }
            return appConfig
        }


        return AppConfig(
            appName = viewModel.appName,
            sourcePath = viewModel.sourcePath,
            packageName = viewModel.packageName,
            versionCode = viewModel.versionCode.toInt(),
            versionName = viewModel.versionName,
            splashText = viewModel.splashText,
            serviceDesc = viewModel.serviceDesc,
            excludeLibraries = excludeLibraries,
            excludeAssets=excludeAssets,
            customOcrModelPath = viewModel.customOcrModelPath
        ).apply {
            viewModel.iconDrawable?.let {
                setIcon { it }
            }
            viewModel.splashIconDrawable?.let {
                setSplashIcon { it }
            }
        }
    }

    @Throws(Exception::class)
    private fun callApkBuilder(
        tmpDir: File,
        outApk: File,
        appConfig: AppConfig,
        projectConfigKt: ProjectConfigKt?
    ): ApkBuilder {
        val templateApk = ApkBuilderPluginHelper.openTemplateApk(this@BuildActivity)
        val keyStorePath = viewModel.keyStore?.path

        Log.d("callApkBuilder: ", appConfig.toString())
        return ApkBuilder(templateApk, outApk, tmpDir.path)
            .setProgressCallback(this@BuildActivity)
            .prepare()
            .withConfig(appConfig, projectConfigKt)
            .build()
            .sign(keyStorePath, keyStorePath?.let { Pref.getKeyStorePassWord(PFiles.getName(it)) })
            .cleanWorkspace()
    }

    private fun showProgressDialog() {
        progressDialog = MaterialDialog.Builder(this)
            .progress(true, 100)
            .content(R.string.text_on_progress)
            .cancelable(false)
            .show()
    }

    private fun onBuildFailed(error: Throwable) {
        progressDialog?.dismiss()
        progressDialog = null
        Toast.makeText(
            this,
            getString(R.string.text_build_failed) + error.message,
            Toast.LENGTH_SHORT
        ).show()
        Log.e(TAG, "Build failed", error)
    }

    private fun onBuildSuccessful(outApk: File) {
        progressDialog?.dismiss()
        progressDialog = null
        MaterialDialog.Builder(this)
            .title(R.string.text_build_successfully)
            .content(getString(R.string.format_build_successfully, outApk.path))
            .positiveText(R.string.text_install)
            .negativeText(R.string.cancel)
            .onPositive { dialog: MaterialDialog?, which: DialogAction? ->
                IntentUtil.installApkOrToast(
                    this@BuildActivity,
                    outApk.path,
                    AppFileProvider.AUTHORITY
                )
            }
            .show()
    }

    override fun onPrepare(builder: ApkBuilder?) {
        progressDialog?.setContent(R.string.apk_builder_prepare)
    }

    override fun onBuild(builder: ApkBuilder?) {
        progressDialog?.setContent(R.string.apk_builder_build)
    }

    override fun onSign(builder: ApkBuilder?) {
        progressDialog?.setContent(R.string.apk_builder_package)
    }

    override fun onClean(builder: ApkBuilder?) {
        progressDialog?.setContent(R.string.apk_builder_clean)
    }

}