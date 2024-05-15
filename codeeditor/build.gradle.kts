import okhttp3.OkHttpClient
import okhttp3.Request
import okio.use

plugins {
    alias(libs.plugins.autojs.android.library)
    id("com.yanzhenjie.andserver")
    id("kotlin-kapt")
}

android {
    namespace = "com.aiselp.autojs.codeeditor"
    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(projects.autojs)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.andserver.api)
    implementation(libs.androidx.constraintlayout)
    kapt(libs.andserver.processor)
    implementation(libs.kotlinx.coroutines.android)
    api(libs.androidx.webkit)
    implementation(libs.google.gson)
    implementation(libs.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

tasks.register("downloadEditor") {
    val tag = "v0.4.0"
    val version = 4
    val uri = "https://github.com/aiselp/vscode-mobile/releases/download/${tag}/dist.zip"
    val assetsDir = File(projectDir, "/src/main/assets/codeeditor")
    val versionFile = File(assetsDir, "version.txt")
    doFirst {
        logger.log(LogLevel.LIFECYCLE, "start downloadEditor")
        assetsDir.mkdirs()
        if (versionFile.isFile) {
            val dowversion = versionFile.readText().toInt()
            if (dowversion == version) {
                logger.log(LogLevel.LIFECYCLE, "skip download")
                return@doFirst
            }
        }
        val response = OkHttpClient.Builder().build().newCall(
            Request.Builder().url(uri).build()
        ).execute()
        check(response.isSuccessful) { "download error response code:${response.code}" }
        response.body!!.byteStream().use {
            File(assetsDir, "dist.zip").outputStream().use { out ->
                it.copyTo(out)
            }
        }
        versionFile.writeText(version.toString())
    }
}
tasks.findByName("preBuild")?.dependsOn("downloadEditor")
tasks.findByName("preDebugBuild")?.dependsOn("downloadEditor")
tasks.names.forEach {
//    logger.error(it)
}