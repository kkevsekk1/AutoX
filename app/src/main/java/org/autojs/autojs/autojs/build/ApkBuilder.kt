package org.autojs.autojs.autojs.build

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
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
import pxb.android.StringItem
import pxb.android.axml.AxmlWriter
import zhao.arsceditor.ArscUtil
import zhao.arsceditor.ResDecoder.ARSCDecoder
import zhao.arsceditor.ResDecoder.data.ResTable
import java.io.*
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.concurrent.Callable
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Created by Stardust on 2017/10/24.
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

    class AppConfig {
        var appName: String? = null
        var versionName: String? = null
        var versionCode = 0
        var sourcePath: String? = null
        var packageName: String? = null
        var ignoredDirs = ArrayList<File>()
        var icon: Callable<Bitmap>? = null
        var splashIcon: Callable<Bitmap>? = null
        var splashText: String? = null
        var hideLauncher = false
        var serviceDesc: String? = null
        fun ignoreDir(dir: File): AppConfig {
            ignoredDirs.add(dir)
            return this
        }

        fun setAppName(appName: String?): AppConfig {
            this.appName = appName
            return this
        }

        fun setVersionName(versionName: String?): AppConfig {
            this.versionName = versionName
            return this
        }

        fun setVersionCode(versionCode: Int): AppConfig {
            this.versionCode = versionCode
            return this
        }

        fun setSourcePath(sourcePath: String?): AppConfig {
            this.sourcePath = sourcePath
            return this
        }

        fun setPackageName(packageName: String?): AppConfig {
            this.packageName = packageName
            return this
        }

        fun setIcon(icon: Callable<Bitmap>?): AppConfig {
            this.icon = icon
            return this
        }

        fun setIcon(iconPath: String?): AppConfig {
            icon = Callable<Bitmap> { BitmapFactory.decodeFile(iconPath) }
            return this
        }

        @JvmName("getIcon1")
        fun getIcon(): Callable<Bitmap>? {
            return icon
        }

        fun setSplashIcon(icon: Callable<Bitmap>?): AppConfig {
            splashIcon = icon
            return this
        }

        fun setSplashIcon(iconPath: String?): AppConfig {
            splashIcon = Callable<Bitmap> { BitmapFactory.decodeFile(iconPath) }
            return this
        }

        @JvmName("getSplashIcon1")
        fun getSplashIcon(): Callable<Bitmap>? {
            return splashIcon
        }

        fun setSplashText(splashText: String?): AppConfig {
            this.splashText = splashText
            return this
        }

        fun setServiceDesc(serviceDesc: String?): AppConfig {
            this.serviceDesc = serviceDesc
            return this
        }

        fun setHideLauncher(hideLauncher: Boolean): AppConfig {
            this.hideLauncher = hideLauncher
            return this
        }

        companion object {
            @JvmStatic
            fun fromProjectConfig(projectDir: String?, projectConfig: ProjectConfigKt): AppConfig {
                val icon = projectConfig.icon
                val splashIcon = projectConfig.launchConfig?.splashIcon
                val appConfig = AppConfig()
                    .setAppName(projectConfig.name)
                    .setPackageName(projectConfig.packageName)
                    .setHideLauncher(projectConfig.launchConfig!!.isHideLauncher)
                    .ignoreDir(File(projectDir, projectConfig.buildDir))
                    .setVersionCode(projectConfig.versionCode.toInt())
                    .setVersionName(projectConfig.versionName)
                    .setSplashText(projectConfig.launchConfig!!.splashText)
                    .setServiceDesc(projectConfig.launchConfig!!.serviceDesc)
                    .setSourcePath(projectDir)
                if (icon != null) {
                    appConfig.setIcon(getIconPath(projectDir, icon))
                }
                if (splashIcon != null) {
                    appConfig.setSplashIcon(getIconPath(projectDir, splashIcon))
                }
                return appConfig
            }

            private fun getIconPath(dir: String?, icon: String): String {
                return if (PFiles.isDir(dir)) {
                    File(dir, icon).path
                } else File(File(dir).parent, icon).path
            }
        }
    }

    private var mProgressCallback: ProgressCallback? = null
    private val mApkPackager: ApkPackager
    private var mArscPackageName: String? = null
    private var mManifestEditor: ManifestEditor? = null
    private var mAppConfig: AppConfig? = null
    private val mWaitSignApk: String
    private var mInitVector: String? = null
    private var mKey: String? = null
    fun setProgressCallback(callback: ProgressCallback?): ApkBuilder {
        mProgressCallback = callback
        return this
    }

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
    fun copyDir(relativePath: String?, path: String?) {
        val fromDir = File(path)
        val toDir = File(mWorkspacePath, relativePath)
        toDir.mkdir()
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
                    copyDir(PFiles.join(relativePath, child.name + "/"), child.path)
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
                JavaScriptFileSource(file).executionMode as Short
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
    fun withConfig(config: AppConfig): ApkBuilder {
        mAppConfig = config
        mManifestEditor = editManifest()
            .setAppName(config.appName)
            .setVersionName(config.versionName)
            .setVersionCode(config.versionCode)
            .setPackageName(config.packageName)
        setArscPackageName(config.packageName)
        updateProjectConfig(config)
        setScriptFile(config.sourcePath)
        return this
    }

    @Throws(FileNotFoundException::class)
    fun editManifest(): ManifestEditor {
        mManifestEditor = ManifestEditorWithAuthorities(FileInputStream(manifestFile))
        return mManifestEditor!!
    }

    protected val manifestFile: File
        protected get() = File(mWorkspacePath, "AndroidManifest.xml")

    private fun updateProjectConfig(appConfig: AppConfig) {
        val config: ProjectConfigKt
        if (!PFiles.isDir(appConfig.sourcePath)) {
            config = ProjectConfigKt(
                mainScriptFile = "main.js",
                name = appConfig.appName!!,
                packageName = appConfig.packageName!!,
                versionName = appConfig.versionName!!,
                versionCode = appConfig.versionCode.toLong(),
                buildInfo = BuildInfo.generate(
                    appConfig.versionCode.toLong()
                )
            )
            PFiles . write (File(mWorkspacePath, "assets/project/project.json").path, config.toJson())

        } else {
            config = ProjectConfigKt.fromProjectDir(appConfig.sourcePath!!)!!
            val buildNumber: Long = config.buildInfo.buildNumber
            config.buildInfo=(BuildInfo.generate(buildNumber + 1))
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
        if (mAppConfig!!.icon != null) {
            try {
                val bitmap: Bitmap? = mAppConfig!!.icon!!.call()
                if (bitmap != null) {
                    bitmap.compress(
                        Bitmap.CompressFormat.PNG, 100,
                        FileOutputStream(File(mWorkspacePath, "res/mipmap/ic_launcher.png"))
                    )
                }
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
        if (mAppConfig!!.splashIcon != null) {
            try {
                val bitmap: Bitmap? = mAppConfig!!.splashIcon!!.call()
                if (bitmap != null) {
                    bitmap.compress(
                        Bitmap.CompressFormat.PNG, 100,
                        FileOutputStream(
                            File(
                                mWorkspacePath,
                                "res/drawable-mdpi-v4/autojs_material.png"
                            )
                        )
                    )
                }
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
        mManifestEditor?.writeTo(FileOutputStream(manifestFile))
        if (mArscPackageName != null) {
            buildArsc()
        }
        return this
    }

    @Throws(Exception::class)
    fun sign(keyStorePath: String?, keyPassword: String?): ApkBuilder {
        if (mProgressCallback != null) {
            GlobalAppContext.post(Runnable { mProgressCallback!!.onSign(this@ApkBuilder) })
        }
        if (keyStorePath != null) {
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
            GlobalAppContext.post(Runnable { mProgressCallback!!.onClean(this@ApkBuilder) })
        }
        delete(File(mWorkspacePath))
        val waitSignApk = File(mWaitSignApk)
        if (waitSignApk.length() > 0) {
            delete(waitSignApk)
        }
        return this
    }

    @Throws(IOException::class)
    fun setArscPackageName(packageName: String?): ApkBuilder {
        mArscPackageName = packageName
        return this
    }

    @Throws(IOException::class)
    private fun buildArsc() {
        val oldArsc = File(mWorkspacePath, "resources.arsc")
        val newArsc = File(mWorkspacePath, "resources_new.arsc")
        val decoder =
            ARSCDecoder(BufferedInputStream(FileInputStream(oldArsc)), null as ResTable?, false)
        val fos = FileOutputStream(newArsc)
        decoder.CloneArsc(fos, mArscPackageName, true)
        val util = ArscUtil()
        util.openArsc(newArsc.absolutePath)
        // 收集字符资源，以准备根据key进行替换
        util.getResouces("string", "[DEFAULT]")
        util.changeResouce("powered_by_autojs", mAppConfig!!.splashText)
        if (StringUtils.isNotBlank(mAppConfig!!.serviceDesc)) {
            util.changeResouce("text_accessibility_service_description", mAppConfig!!.serviceDesc)
        }
        util.saveArsc(oldArsc.absolutePath, newArsc.absolutePath)
        newArsc.delete()
    }

    private fun delete(file: File) {
        if (file.isFile) {
            file.delete()
        } else {
            val files = file.listFiles()
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
        val `arr$` = dir.listFiles()
        val `len$` = `arr$`.size
        for (`i$` in 0 until `len$`) {
            val f = `arr$`[`i$`]
            if (f.isFile) {
                doFile(f.name, f, zos, dos)
            } else {
                doDir(f.name + "/", f, zos, dos)
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

        @Throws(IOException::class)
        private fun doDir(
            prefix: String,
            dir: File,
            zos: ZipOutputStream,
            dos: DigestOutputStream
        ) {
            zos.putNextEntry(ZipEntry(prefix))
            zos.closeEntry()
            val `arr$` = dir.listFiles()
            val `len$` = `arr$`.size
            for (`i$` in 0 until `len$`) {
                val f = `arr$`[`i$`]
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
        mWaitSignApk = mOutApkFile.absolutePath + NO_SIGN_APK_SUFFIX
        mApkPackager = ApkPackager(apkInputStream, mWorkspacePath)
        PFiles.ensureDir(mOutApkFile.path)
    }
}