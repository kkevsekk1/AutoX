package org.autojs.autojs.autojs.build

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.apkbuilder.ApkPackager
import com.stardust.autojs.apkbuilder.ManifestEditor
import com.stardust.autojs.apkbuilder.util.StreamUtils
import com.stardust.autojs.project.BuildInfo
import com.stardust.autojs.project.ProjectConfigKt
import com.stardust.autojs.script.EncryptedScriptFileHeader
import com.stardust.autojs.script.JavaScriptFileSource
import com.stardust.pio.PFiles
import com.stardust.util.AdvancedEncryptionStandard
import com.stardust.util.MD5
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.autojs.autojs.build.ApkSigner
import org.autojs.autojs.build.TinySign
import org.autojs.autojs.ui.project.Constant
import pxb.android.StringItem
import pxb.android.axml.AxmlWriter
import zhao.arsceditor.ArscUtil
import zhao.arsceditor.ResDecoder.ARSCDecoder
import zhao.arsceditor.ResDecoder.data.ResTable
import java.io.*
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Created by Stardust on 2017/10/24.
 * Modified by wilinz on 2022/5/23
 */
class ApkBuilder(
    apkInputStream: InputStream?,
    private val mOutApkFile: File,
    private val mWorkspacePath: String
) {
    interface ProgressCallback {
        fun onPrepare(builder: ApkBuilder?)
        fun onBuild(builder: ApkBuilder?)
        fun onSign(builder: ApkBuilder?)
        fun onClean(builder: ApkBuilder?)
    }

    data class AppConfig(
        var appName: String? = null,
        var versionName: String? = null,
        var versionCode: Int = 0,
        var sourcePath: String? = null,
        var packageName: String? = null,
        var ignoredDirs: ArrayList<File> = ArrayList(),
        var icon: (() -> Bitmap)? = null,
        var splashIcon: (() -> Bitmap)? = null,
        var splashText: String? = null,
        var hideLauncher: Boolean = false,
        var serviceDesc: String? = null,
        var excludeLibraries: MutableList<String> = mutableListOf(),
        var excludeAssets: MutableList<String> = mutableListOf(),
        var customOcrModelPath: String? = null
    ) {

        fun addExcludeLibrary(library: String) {
            excludeLibraries.add(library)
        }

        fun ignoreDir(dir: File): AppConfig {
            ignoredDirs.add(dir)
            return this
        }

        fun setIcon(icon: (() -> Bitmap)): AppConfig {
            this.icon = icon
            return this
        }

        fun setIcon(iconPath: String): AppConfig {
            icon = { BitmapFactory.decodeFile(iconPath) }
            return this
        }

        fun setSplashIcon(icon: (() -> Bitmap)): AppConfig {
            this.splashIcon = icon
            return this
        }

        fun setSplashIcon(iconPath: String): AppConfig {
            splashIcon = { BitmapFactory.decodeFile(iconPath) }
            return this
        }

        companion object {
            @JvmStatic
            fun fromProjectConfig(projectDir: String, projectConfig: ProjectConfigKt): AppConfig {
                val icon = projectConfig.icon
                val splashIcon = projectConfig.launchConfig.splashIcon

                val appConfig = AppConfig(
                    appName = projectConfig.name,
                    packageName = projectConfig.packageName,
                    hideLauncher = projectConfig.launchConfig.isHideLauncher,
                    versionCode = projectConfig.versionCode,
                    versionName = projectConfig.versionName,
                    splashText = projectConfig.launchConfig.splashText,
                    serviceDesc = projectConfig.launchConfig.serviceDesc,
                    sourcePath = projectDir,
                ).apply {
                    ignoreDir(File(projectDir, projectConfig.buildDir))
                    icon?.let { setIcon(getIconPath(projectDir, it)) }
                    splashIcon?.let { setIcon(getIconPath(projectDir, it)) }
                }
                return appConfig
            }

            private fun getIconPath(dir: String, icon: String): String {
                return if (PFiles.isDir(dir)) {
                    File(dir, icon).path
                } else File(File(dir).parent, icon).path
            }
        }
    }

    private var mProgressCallback: ProgressCallback? = null
    private val mApkPackager: ApkPackager = ApkPackager(apkInputStream, mWorkspacePath)
    private var mArscPackageName: String? = null
    private var mManifestEditor: ManifestEditor? = null
    private var mAppConfig: AppConfig? = null
    private val mWaitSignApk: String = mOutApkFile.absolutePath + NO_SIGN_APK_SUFFIX
    private var mInitVector: String? = null
    private var mKey: String? = null
    fun setProgressCallback(callback: ProgressCallback?): ApkBuilder {
        mProgressCallback = callback
        return this
    }

    /**
     * 新建工作目录并解压apk
     */
    @Throws(IOException::class)
    fun prepare(): ApkBuilder {
        if (mProgressCallback != null) {
            GlobalAppContext.post(Runnable { mProgressCallback!!.onPrepare(this@ApkBuilder) })
        }
        File(mWorkspacePath).mkdirs()
        mApkPackager.unzip()
        return this
    }

    @Throws(IOException::class)
    fun setScriptFile(path: String?): ApkBuilder {
        if (PFiles.isDir(path)) {
            copyDir("assets/project/", path)
        } else {
            replaceFile("assets/project/main.js", path)
        }
        return this
    }

    @Throws(IOException::class)
    fun copyDir(relativeTargetPath: String?, srcPath: String?) {
        val fromDir = File(srcPath)
        val toDir = File(mWorkspacePath, relativeTargetPath)
        toDir.mkdirs()
        for (child in fromDir.listFiles()) {
            if (child.isFile) {
                if (child.name.endsWith(".js")) {
                    encrypt(toDir, child)
                } else {
                    StreamUtils.write(
                        FileInputStream(child),
                        FileOutputStream(File(toDir, child.name))
                    )
                }
            } else {
                if (!mAppConfig!!.ignoredDirs.contains(child)) {
                    copyDir(PFiles.join(relativeTargetPath, child.name + "/"), child.path)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun encrypt(toDir: File, file: File) {
        val fos = FileOutputStream(File(toDir, file.name))
        encrypt(fos, file)
    }

    @Throws(IOException::class)
    private fun encrypt(fos: FileOutputStream, file: File) {
        try {
            EncryptedScriptFileHeader.writeHeader(
                fos,
                JavaScriptFileSource(file).executionMode.toShort()
            )
            val bytes: ByteArray = AdvancedEncryptionStandard(
                mKey!!.toByteArray(),
                mInitVector!!
            ).encrypt(PFiles.readBytes(file.path))
            fos.write(bytes)
            fos.close()
        } catch (e: Exception) {
            throw IOException(e)
        }
    }

    @Throws(IOException::class)
    fun replaceFile(relativePath: String?, newFilePath: String?): ApkBuilder {
        if (newFilePath!!.endsWith(".js")) {
            encrypt(FileOutputStream(File(mWorkspacePath, relativePath)), File(newFilePath))
        } else {
            StreamUtils.write(
                FileInputStream(newFilePath),
                FileOutputStream(File(mWorkspacePath, relativePath))
            )
        }
        return this
    }

    @Throws(IOException::class)
    fun withConfig(config: AppConfig, projectConfig: ProjectConfigKt?): ApkBuilder {
        mAppConfig = config
        mManifestEditor = editManifest()
            .setAppName(config.appName)
            .setVersionName(config.versionName)
            .setVersionCode(config.versionCode)
            .setPackageName(config.packageName)
        setArscPackageName(config.packageName!!)
        updateProjectConfig(config, projectConfig)

        Log.d(TAG, config.excludeLibraries.toString())
        deleteLibraries(config)
        deleteAssets(config)
        if (!config.customOcrModelPath.isNullOrEmpty()) {
            val dirName = File(config.customOcrModelPath!!).name
            copyDir("/assets/${Constant.Assets.OCR_MODELS}/$dirName", config.customOcrModelPath)
        }
        setScriptFile(config.sourcePath)
        return this
    }

    private fun deleteLibraries(config: AppConfig) {
        config.excludeLibraries.forEach { name ->
            File(mWorkspacePath, "/lib").listFiles()?.forEach { archDir ->
                when (name) {
                    Constant.Libraries.OPEN_CV -> {
                        delete(File(archDir, "/libopencv_java4.so"))
                    }
                    Constant.Libraries.OCR -> {
                        delete(File(archDir, "/libc++_shared.so"))
                        delete(File(archDir, "/libpaddle_light_api_shared.so"))
                        delete(File(archDir, "/libhiai.so"))
                        delete(File(archDir, "/libhiai_ir.so"))
                        delete(File(archDir, "/libhiai_ir_build.so"))
                        delete(File(archDir, "/libNative.so"))
                    }
                    Constant.Libraries.`7ZIP` -> {
                        delete(File(archDir, "/libp7zip.so"))
                    }
                    Constant.Libraries.TERMINAL_EMULATOR -> {
                        delete(File(archDir, "/libjackpal-androidterm5.so"))
                        delete(File(archDir, "/libjackpal-termexec2.so"))
                    }
                }
            }
        }
    }

    private fun deleteAssets(config: AppConfig) {
        config.excludeAssets.forEach { name ->
            delete(File(mWorkspacePath, "/assets/${name}"))
        }
    }

    @Throws(FileNotFoundException::class)
    fun editManifest(): ManifestEditor {
        mManifestEditor = ManifestEditorWithAuthorities(FileInputStream(manifestFile))
        return mManifestEditor!!
    }

    protected val manifestFile: File
        protected get() = File(mWorkspacePath, "AndroidManifest.xml")

    private fun updateProjectConfig(appConfig: AppConfig, projectConfig: ProjectConfigKt?) {
        val config: ProjectConfigKt
        if (PFiles.isFile(appConfig.sourcePath) && projectConfig != null) {
            projectConfig.apply {
                buildInfo = BuildInfo.generate(
                    appConfig.versionCode.toLong()
                )
            }
            config = projectConfig
            PFiles.write(
                File(mWorkspacePath, "assets/project/project.json").path,
                projectConfig.toJson()
            )

        } else {
            config = ProjectConfigKt.fromProjectDir(appConfig.sourcePath!!)!!
            val buildNumber: Long = config.buildInfo.buildNumber
            config.buildInfo = (BuildInfo.generate(buildNumber + 1))
            PFiles.write(ProjectConfigKt.configFileOfDir(appConfig.sourcePath!!), config.toJson())
        }
        mKey =
            MD5.md5(config.packageName + config.versionName + config.mainScriptFile)
        mInitVector =
            MD5.md5(config.buildInfo.buildId + config.name).substring(0, 16)
    }

    @Throws(IOException::class)
    fun build(): ApkBuilder {
        if (mProgressCallback != null) {
            GlobalAppContext.post(Runnable { mProgressCallback!!.onBuild(this@ApkBuilder) })
        }
        mManifestEditor?.commit()


        mManifestEditor?.writeTo(FileOutputStream(manifestFile))
        if (mArscPackageName != null) {
            buildArsc(
                onReplaceIcon = {
                    if (mAppConfig!!.icon != null) {
                        try {
                            //fix file not found bug
                            val file = File(mWorkspacePath, it)
                            mAppConfig!!.icon?.invoke()?.compress(
                                Bitmap.CompressFormat.PNG, 100,
                                file.outputStream()
                            )
                        } catch (e: Exception) {
                            throw RuntimeException(e)
                        }
                    }
                },
                onReplaceSplashIcon = {
                    if (mAppConfig!!.splashIcon != null) {
                        try {
                            val file = File(
                                mWorkspacePath,
                                it
                            )
                            mAppConfig!!.splashIcon?.invoke()?.compress(
                                Bitmap.CompressFormat.PNG, 100,
                                file.outputStream()
                            )
                        } catch (e: Exception) {
                            throw RuntimeException(e)
                        }
                    }
                }
            )
        }
        return this
    }

    private fun createFile(file: File) {
        file.parentFile?.let {
            if (!it.exists()) it.mkdirs()
        }
        if (!file.exists()) file.createNewFile()
    }

    @Throws(Exception::class)
    fun sign(keyStorePath: String?, keyPassword: String?): ApkBuilder {
        if (mProgressCallback != null) {
            GlobalAppContext.post(Runnable { mProgressCallback!!.onSign(this@ApkBuilder) })
        }
        if (keyStorePath != null && keyPassword != null && keyStorePath.isNotEmpty()) {
            val waitSignApk = File(mWaitSignApk)
            val out = FileOutputStream(waitSignApk)
            val zos = ZipOutputStream(out)
            inZip(File(mWorkspacePath), zos)
            zos.close()
            ApkSigner.sign(keyStorePath, keyPassword, waitSignApk, mOutApkFile)
        } else {
            val fos = FileOutputStream(mOutApkFile)
            TinySign.sign(File(mWorkspacePath), fos)
            fos.close()
        }
        return this
    }

    fun cleanWorkspace(): ApkBuilder {
        if (mProgressCallback != null) {
            GlobalAppContext.post { mProgressCallback!!.onClean(this@ApkBuilder) }
        }
        delete(File(mWorkspacePath))
        val waitSignApk = File(mWaitSignApk)
        if (waitSignApk.length() > 0) {
            delete(waitSignApk)
        }
        return this
    }

    @Throws(IOException::class)
    fun setArscPackageName(packageName: String): ApkBuilder {
        mArscPackageName = packageName
        return this
    }

    @Throws(IOException::class)
    private fun buildArsc(
        onReplaceIcon: (path: String) -> Unit,
        onReplaceSplashIcon: (path: String) -> Unit
    ) {
        val oldArsc = File(mWorkspacePath, "resources.arsc")
        val newArsc = File(mWorkspacePath, "resources_new.arsc")
        val decoder =
            ARSCDecoder(BufferedInputStream(FileInputStream(oldArsc)), null as ResTable?, false)
        val fos = FileOutputStream(newArsc)
        decoder.CloneArsc(fos, mArscPackageName, true)
        val util = ArscUtil()
        util.openArsc(newArsc.absolutePath) { config, type, key, value ->
            when (type) {
                "mipmap" -> {
                    if (key == "ic_launcher") {
                        onReplaceIcon(value)
                    }
                }
                "drawable" -> {
                    if (key == "autojs_material") onReplaceSplashIcon(value)
                }
            }
        }
        // 收集字符资源，以准备根据key进行替换

        util.getResouces("string", "[DEFAULT]")
        mAppConfig?.splashText?.let { util.changeResource("powered_by_autojs", it) }
        if (StringUtils.isNotBlank(mAppConfig!!.serviceDesc)) {
            mAppConfig?.serviceDesc?.let {
                util.changeResource(
                    "text_accessibility_service_description",
                    it
                )
            }
        }
        util.saveArsc(oldArsc.absolutePath, newArsc.absolutePath)
        newArsc.delete()
    }

    private fun delete(file: File) {
        if (file.isFile) {
            file.delete()
        } else {
            val files = file.listFiles() ?: return
            for (child in files) {
                delete(child)
            }
            file.delete()
        }
    }

    // 重新压缩apk
    @Throws(Exception::class)
    private fun inZip(dir: File, zos: ZipOutputStream) {
        val md = MessageDigest.getInstance("SHA-256")
        val dos = DigestOutputStream(zos, md)
        delete(File(dir.path + "/META-INF/MANIFEST.MF"))
        delete(File(dir.path + "/META-INF/CERT.RSA"))
        delete(File(dir.path + "/META-INF/CERT.SF"))
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isFile) {
                doFile(file.name, file, zos, dos)
            } else {
                doDir(file.name + "/", file, zos, dos)
            }
        }
    }

    private inner class ManifestEditorWithAuthorities internal constructor(manifestInputStream: InputStream?) :
        ManifestEditor(manifestInputStream) {
        override fun onAttr(attr: AxmlWriter.Attr) {
            if (mAppConfig!!.hideLauncher && attr.value is StringItem && "android.intent.category.LAUNCHER" == (attr.value as StringItem).data) {
                Log.e("attr", "onAttr: " + (attr.value as StringItem).data + "----" + "")
                (attr.value as StringItem).data = "android.intent.category.DEFAULT"
            }
            if ("authorities" == attr.name.data && attr.value is StringItem) {
                (attr.value as StringItem).data = mAppConfig!!.packageName + ".fileprovider"
            } else {
                super.onAttr(attr)
            }
        }
    }

    companion object {
        private const val NO_SIGN_APK_SUFFIX = "_no-sign.apk"
        private val stripPattern = Pattern.compile("^META-INF/(.*)[.](SF|RSA|DSA|MF)$")
        private const val TAG: String = "ApkBuilder"

        @Throws(IOException::class)
        private fun doDir(
            prefix: String,
            dir: File,
            zos: ZipOutputStream,
            dos: DigestOutputStream
        ) {
            zos.putNextEntry(ZipEntry(prefix))
            zos.closeEntry()
            val files = dir.listFiles() ?: return
            for (f in files) {
                if (f.isFile) {
                    doFile(prefix + f.name, f, zos, dos)
                } else {
                    doDir(prefix + f.name + "/", f, zos, dos)
                }
            }
        }

        @Throws(IOException::class)
        private fun doFile(name: String, f: File, zos: ZipOutputStream, dos: DigestOutputStream) {
            zos.putNextEntry(ZipEntry(name))
            val fis = FileUtils.openInputStream(f)
            IOUtils.copy(fis, dos)
            IOUtils.closeQuietly(fis)
            zos.closeEntry()
        }
    }

    init {
        PFiles.ensureDir(mOutApkFile.path)
    }
}