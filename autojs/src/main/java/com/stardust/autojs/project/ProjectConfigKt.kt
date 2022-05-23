package com.stardust.autojs.project

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.stardust.pio.PFiles
import java.io.File
import java.util.zip.CRC32

/**
 * Modified by wilinz on 2022/5/23
 */
data class ProjectConfigKt(
    @SerializedName("assets")
    var assets: List<String> = emptyList(),
    @SerializedName("build")
    var buildInfo: BuildInfo = BuildInfo(),
    @SerializedName("launchConfig")
    var launchConfig: LaunchConfigKt = LaunchConfigKt(),
    @SerializedName("main")
    var mainScriptFile: String? = null,
    @SerializedName("name")
    var name: String? = null,
    @SerializedName("packageName")
    var packageName: String? = null,
    @SerializedName("versionCode")
    var versionCode: Int = -1,
    @SerializedName("versionName")
    var versionName: String? = null,
    @SerializedName("icon")
    var icon: String? = null,
    @SerializedName("scripts")
    var scriptConfigs: Map<String, ScriptConfig> = HashMap(),
    @SerializedName("useFeatures")
    var features: List<String> = emptyList(),
    @SerializedName("signingConfig")
    var signingConfig: SigningConfig = SigningConfig()
) {
    companion object {

        const val CONFIG_FILE_NAME = "project.json"
        private val GSON = GsonBuilder().setPrettyPrinting().create()

        fun fromJson(json: String): ProjectConfigKt? {
            val config = GSON.fromJson(json, ProjectConfigKt::class.java)
            return if (!isValid(config)) null else config
        }

        private fun isValid(config: ProjectConfigKt): Boolean {
            return with(config) {
                !name.isNullOrEmpty()
                        && !packageName.isNullOrEmpty()
                        && !versionName.isNullOrEmpty()
                        && !mainScriptFile.isNullOrEmpty()
                        && versionCode != -1
            }
        }

        fun fromAssets(context: Context, path: String): ProjectConfigKt? {
            return try {
                fromJson(context.assets.open(path).reader().readText())
            } catch (e: Exception) {
                null
            }
        }

        fun fromFile(path: String): ProjectConfigKt? {
            return try {
                fromJson(File(path).readText())
            } catch (e: Exception) {
                null
            }
        }

        fun fromProjectDir(path: String): ProjectConfigKt? {
            return fromFile(configFileOfDir(path))
        }

        fun fromProjectDir(path: String, configName: String): ProjectConfigKt? {
            return fromFile(configFileOfDir(path, configName))
        }

        fun configFileOfDir(projectDir: String): String {
            return PFiles.join(projectDir, CONFIG_FILE_NAME)
        }

        fun configFileOfDir(projectDir: String, configName: String): String {
            return PFiles.join(projectDir, configName)
        }
    }

    val buildDir: String = "build"

    fun toJson(): String {
        return GSON.toJson(this)
    }

    fun getScriptConfig(path: String): ScriptConfig {
        val config = scriptConfigs[path] ?: ScriptConfig()
        if (features.isEmpty()) {
            return config
        }
        val features = ArrayList(config.features)
        for (feature in features) {
            if (!features.contains(feature)) {
                features.add(feature)
            }
        }
        config.features = features
        return config
    }
}

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

data class LaunchConfigKt(
    @SerializedName("displaySplash")
    var displaySplash: Boolean = false,
    @SerializedName("hideLogs")
    var isHideLogs: Boolean = false,
    @SerializedName("permissions")
    var permissions: List<String> = emptyList(),
    @SerializedName("serviceDesc")
    var serviceDesc: String = "使脚本自动操作(点击、长按、滑动等)所需，若关闭则只能执行不涉及自动操作的脚本。",
    @SerializedName("splashIcon")
    var splashIcon: String? = null,
    @SerializedName("splashText")
    var splashText: String = "Powered by Autoxjs.com",
    @SerializedName("stableMode")
    var isStableMode: Boolean = false,
    @SerializedName("volumeUpcontrol")
    var isVolumeUpControl: Boolean = true,
    @SerializedName("hideLauncher")
    var isHideLauncher: Boolean = false
)