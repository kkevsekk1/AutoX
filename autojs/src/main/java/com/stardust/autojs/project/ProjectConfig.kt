package com.stardust.autojs.project

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.R
import com.stardust.pio.PFiles
import java.io.File
import java.util.zip.CRC32

/**
 * Modified by wilinz on 2022/5/23
 */
@Keep
data class ProjectConfig(
    var name: String? = null,
    var packageName: String? = null,
    var versionCode: Int = 1,
    var versionName: String = "1.0.0",
    var icon: String? = null,
    @SerializedName("main")
    var mainScript: String? = null,
    var scripts: Map<String, ScriptConfig> = HashMap(),
    @SerializedName("build")
    var buildInfo: BuildInfo = BuildInfo(),
    var launchConfig: LaunchConfig = LaunchConfig(),
    @SerializedName("useFeatures")
    var features: ArrayList<String> = arrayListOf(),
    var sourcePath: String? = null,
    var projectDirectory: String? = null,
    var outputPath: String? = null,
    val buildDir: String = "build",
    var ignoredDirs: List<String> = emptyList(),
    var libs: MutableList<String> = mutableListOf(),
    var abis: MutableList<String> = arrayListOf<String>().apply { addAll(Constant.Abi.abis) },
    var assets: List<Asset> = emptyList(),
    var signingConfig: SigningConfig = SigningConfig(),
    @SerializedName("encrypt-code")
    var isEncrypt:Boolean = false
) {

    fun getAbsolutePath(name: String): String {
        return if (name.startsWith("/")) name
        else File(this.projectDirectory, name).absolutePath
    }

    fun toJson(): String {
        return GSON.toJson(this)
    }

    companion object {

        const val CONFIG_FILE_NAME = "project.json"
        private val GSON = GsonBuilder().serializeNulls().setPrettyPrinting().create()

        fun fromJson(json: String): ProjectConfig? {
            val config = GSON.fromJson(json, ProjectConfig::class.java)
            return if (!isValid(config)) null else config
        }

        private fun isValid(config: ProjectConfig): Boolean {
            return with(config) {
                !name.isNullOrBlank()
                        && !packageName.isNullOrEmpty()
                        && versionName.isNotEmpty()
                        && !mainScript.isNullOrEmpty()
                        && versionCode != -1
            }
        }

        fun fromAssets(context: Context, path: String): ProjectConfig? {
            return try {
                fromJson(context.assets.open(path).reader().use { it.readText() })
            } catch (e: Exception) {
                null
            }
        }

        fun fromProject(path: File): ProjectConfig? {
            val file = with(path) {
                if (isFile) return@with this
                if (isDirectory) return@with File(this, CONFIG_FILE_NAME)
                null
            }
            return try {
                file?.let { fromJson(it.readText()) }
            } catch (_: Exception) {
                null
            }

        }

        fun configFileOfDir(projectDir: String, configName: String = CONFIG_FILE_NAME): String {
            return PFiles.join(projectDir, configName)
        }

    }
}

@Keep
data class Asset(
    var form: String,
    var to: String
)

@Keep
data class BuildInfo(
    @SerializedName("build_id")
    var buildId: String? = null,
    @SerializedName("build_number")
    var buildNumber: Long = 0,
    @SerializedName("build_time")
    var buildTime: Long = 0
) {
    companion object {
        fun generate(buildNumber: Long): BuildInfo {
            val buildTime = System.currentTimeMillis()
            return BuildInfo(
                buildNumber = buildNumber,
                buildTime = System.currentTimeMillis(),
                buildId = generateBuildId(buildNumber, buildTime)
            )
        }

        private fun generateBuildId(buildNumber: Long, buildTime: Long): String {
            val crc32 = CRC32()
            crc32.update((buildNumber.toString() + "" + buildTime).toByteArray())
            return String.format("%08X", crc32.value) + "-" + buildNumber
        }
    }


}

@Keep
data class LaunchConfig(
    @SerializedName("displaySplash")
    var displaySplash: Boolean = true,
    @SerializedName("hideLogs")
    var isHideLogs: Boolean = false,
    @SerializedName("permissions")
    var permissions: List<String> = emptyList(),
    @SerializedName("serviceDesc")
    var serviceDesc: String = GlobalAppContext.get()
        .getString(R.string.text_accessibility_service_description),
    @SerializedName("splashIcon")
    var splashIcon: String? = null,
    @SerializedName("splashText")
    var splashText: String = "Powered by Autoxjs.com",
    @SerializedName("stableMode")
    var isStableMode: Boolean = false,
    @SerializedName("volumeUpcontrol")
    var isVolumeUpControl: Boolean = false,
    @SerializedName("hideLauncher")
    var isHideLauncher: Boolean = false,
    @SerializedName("hideAccessibilityServices")
    var isHideAccessibilityServices: Boolean = false,
)