package com.stardust.autojs.project

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.Keep
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.R
import com.stardust.pio.PFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.CRC32

/**
 * Modified by wilinz on 2022/5/23
 */
@Keep
data class ProjectConfig(
    @SerializedName("name")
    var name: String? = null,
    @SerializedName("packageName")
    var packageName: String? = null,
    @SerializedName("versionCode")
    var versionCode: Int = 1,
    @SerializedName("versionName")
    var versionName: String = "1.0.0",
    @SerializedName("icon")
    var icon: String? = null,
    @SerializedName("main")
    var mainScript: String? = null,
    @SerializedName("scripts")
    var scripts: Map<String, ScriptConfig> = HashMap(),
    @SerializedName("build")
    var buildInfo: BuildInfo = BuildInfo(),
    @SerializedName("launchConfig")
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
) {

    fun getAbsolutePath(name: String): String {
        return if (name.startsWith("/")) name
        else File(this.projectDirectory, name).absolutePath
    }

    companion object {

        const val CONFIG_FILE_NAME = "project.json"
        private val GSON = GsonBuilder().serializeNulls().setPrettyPrinting().create()

        fun fromJson(json: String): ProjectConfig? {
            val config = GSON.fromJson(json, ProjectConfig::class.java)
            return if (!isValid(config)) null else config
        }

        fun isValid(config: ProjectConfig): Boolean {
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

        fun fromFile(path: String): ProjectConfig? {
            return try {
                fromJson(File(path).readText())
            } catch (e: Exception) {
                null
            }
        }

        fun fromProjectDir(path: String): ProjectConfig? {
            return fromFile(configFileOfDir(path))
        }

        fun fromProjectDir(path: String, configName: String): ProjectConfig? {
            return fromFile(configFileOfDir(path, configName))
        }

        suspend fun fromAssetsAsync(context: Context, path: String): ProjectConfig? =
            withContext(Dispatchers.IO) {
                return@withContext try {
                    fromJson(context.assets.open(path).reader().use { it.readText() })
                } catch (e: Exception) {
                    null
                }
            }

        suspend fun fromFileAsync(path: String): ProjectConfig? = withContext(Dispatchers.IO) {
            return@withContext try {
                fromJson(File(path).readText())
            } catch (e: Exception) {
                null
            }
        }

        suspend fun fromProjectDirAsync(path: String): ProjectConfig? = withContext(Dispatchers.IO) {
            return@withContext fromFile(configFileOfDir(path))
        }

        suspend fun fromProjectDirAsync(path: String, configName: String): ProjectConfig? =
            withContext(Dispatchers.IO) {
                return@withContext fromFile(configFileOfDir(path, configName))
            }

        fun configFileOfDir(projectDir: String): String {
            return PFiles.join(projectDir, CONFIG_FILE_NAME)
        }

        fun configFileOfDir(projectDir: String, configName: String): String {
            return PFiles.join(projectDir, configName)
        }

    }

    fun toJson(): String {
        return GSON.toJson(this)
    }

    fun getScriptConfig(path: String): ScriptConfig {
        val scriptConfig = scripts[path] ?: ScriptConfig()
        if (features.isEmpty()) {
            return scriptConfig
        }
        //
        val features = ArrayList(scriptConfig.features)
        for (feature in features) {
            if (!this.features.contains(feature)) {
                this.features.add(feature)
            }
        }
        scriptConfig.features = features
        return scriptConfig
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
    var displaySplash: Boolean = false,
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
)