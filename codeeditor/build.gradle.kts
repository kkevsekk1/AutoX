import java.net.URL
plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    namespace = "com.aiselp.autojs.codeeditor"
    compileSdk = 33

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.kotlinx.coroutines.android)
    api(libs.nanohttpd.webserver)
    api(libs.androidx.webkit)
    implementation(libs.google.gson)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

tasks.register("downloadEditor") {
    val tag = "dev-0.3.0"
    val version = 3
    val uri = "https://github.com/aiselp/vscode-mobile/releases/download/${tag}/dist.zip"
    val assetsDir = File(projectDir, "/src/main/assets/codeeditor")
    val versionFile = File(assetsDir, "version.txt")
    doFirst {
        logger.log(org.gradle.api.logging.LogLevel.LIFECYCLE,"start downloadEditor")
        if (versionFile.isFile){
            val dowversion = versionFile.readText().toInt()
            if (dowversion == version) {
                logger.log(org.gradle.api.logging.LogLevel.LIFECYCLE,"skip download")
                return@doFirst
            }
        }
        URL(uri).openStream().use {
            File(assetsDir, "dist.zip").outputStream().use { out->
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