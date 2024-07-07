import com.android.build.gradle.internal.tasks.factory.dependsOn
import okhttp3.Request
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

plugins {
    alias(libs.plugins.autojs.android.application)
    alias(libs.plugins.autojs.android.application.compose)
    alias(libs.plugins.kotlin.serialization)
}

val propFile: File = File("E:/资料/jks/autojs-app/sign.properties")
val properties = Properties()
if (propFile.exists()) {
    propFile.inputStream().reader().use {
        properties.load(it)
    }
}

android {
    defaultConfig {
        applicationId = "org.autojs.autoxjs"
        versionCode = AndroidConfigConventions.VERSION_CODE
        versionName = AndroidConfigConventions.VERSION_NAME
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//        multiDexEnabled = true
        buildConfigField("boolean", "isMarket", "false")
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["resourcePackageName"] = applicationId.toString()
                arguments["androidManifestFile"] = "$projectDir/src/main/AndroidManifest.xml"
            }
        }
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }
    lint {
        abortOnError = false
        disable += listOf("MissingTranslation", "ExtraTranslation")
    }
    signingConfigs {
        if (propFile.exists()) {
            getByName("release") {
                storeFile = file(properties.getProperty("storeFile"))
                storePassword = properties.getProperty("storePassword")
                keyAlias = properties.getProperty("keyAlias")
                keyPassword = properties.getProperty("keyPassword")
            }
        }
    }
    splits {
        // Configures multiple APKs based on ABI.
        abi {
            // Enables building multiple APKs per ABI.
            isEnable = true

            // By default all ABIs are included, so use reset() and include to specify that we only
            // want APKs for x86 and x86_64.
            // Resets the list of ABIs that Gradle should create APKs for to none.
            reset()

            // Specifies a list of ABIs that Gradle should create APKs for.
            include("armeabi-v7a", "arm64-v8a")

            // Specifies that we do not want to also generate a universal APK that includes all ABIs.
            isUniversalApk = true
        }
    }
    buildTypes {
        named("debug") {
            isShrinkResources = false
            isMinifyEnabled = false
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android.txt"),
                    "proguard-rules.pro"
                )
            )
            if (propFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        named("release") {
            isShrinkResources = false
            isMinifyEnabled = false
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android.txt"),
                    "proguard-rules.pro"
                )
            )
            if (propFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    flavorDimensions.add("channel")
    productFlavors {
        create("common") {
            versionCode = AndroidConfigConventions.VERSION_CODE
            versionName = AndroidConfigConventions.VERSION_NAME
            buildConfigField("String", "CHANNEL", "\"common\"")
//            buildConfigField("String", "APPID", "\"?id=21\"")
            manifestPlaceholders.putAll(mapOf("appName" to "@string/app_name"))
        }
        create("v6") {
            applicationIdSuffix = ".v6"
            versionCode = AndroidConfigConventions.VERSION_CODE
            versionName = AndroidConfigConventions.VERSION_NAME
            buildConfigField("String", "CHANNEL", "\"v6\"")
//            buildConfigField("String", "APPID", "\"?id=23\"")
            manifestPlaceholders.putAll(mapOf("appName" to "Autox.js v6"))
        }
    }

    sourceSets {
        getByName("main") {
            res.srcDirs("src/main/res", "src/main/res-i18n")
            jniLibs.srcDirs("/libs")
        }
    }

    configurations.all {
        resolutionStrategy.force("com.google.code.findbugs:jsr305:3.0.2")
        exclude(group = "org.jetbrains", module = "annotations-java5")
//        exclude(group = "com.atlassian.commonmark",) module = "commonmark"
        exclude(group = "com.github.atlassian.commonmark-java", module = "commonmark")
    }
    packaging {
        resources {
            pickFirsts += "META-INF/io.netty.versions.properties"
            pickFirsts += "META-INF/INDEX.LIST"
        }
    }
    namespace = "org.autojs.autoxjs"
}

dependencies {
    implementation(projects.autojs)
    implementation(projects.apkbuilder)
    implementation(projects.codeeditor)

    implementation(projects.core.network)

    implementation(libs.androidx.localbroadcastmanager)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.webkit)

    implementation(libs.bundles.accompanist)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material3)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    implementation(libs.androidx.activity.compose)

    androidTestImplementation(libs.espresso.core)
    testImplementation(libs.junit)
    // Kotlin携程
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.appcompat)
    implementation(libs.androidx.cardview)
    implementation(libs.material)
    // Personal libraries
    implementation(libs.mutabletheme)
    // Material Dialogs
    implementation(libs.core)
    // Common Markdown
    implementation(libs.commonmark.java)
    // Android issue reporter (a github issue reporter)
    implementation(libs.android.issue.reporter) {
        exclude(group = "com.afollestad.material-dialogs")
        exclude(group = "com.android.support")
    }
    // MultiLevelListView
    implementation(libs.android.multi.level.listview)
    // Licenses Dialog
    implementation(libs.licensesdialog)
    // Expandable RecyclerView
    implementation(libs.android.expandablerecyclerview)
    // FlexibleDivider
    implementation(libs.recyclerview.flexibledivider)
    // Commons-lang
    implementation(libs.commons.lang3)
    // 证书签名相关
    implementation(libs.bcpkix.jdk15on)
    // Expandable RecyclerView
    implementation(libs.expandablerecyclerview)
    // RxJava
    implementation(libs.rxjava2)
    implementation(libs.rxjava2.rxandroid)

    // Glide
    implementation(libs.glide)
    // joda time
    implementation(libs.android.joda)
    // Tasker Plugin
    implementation(libs.android.plugin.client.sdk.`for`.locale)
    // Flurry
    implementation(libs.analytics)
    // tencent
    implementation(libs.crashreport)
    api(libs.tbssdk)
    // MaterialDialogCommon
    implementation(libs.material.dialogs.commons)
    // WorkManager
    implementation(libs.androidx.work.runtime)
    // Android job
    implementation(libs.android.job)
    implementation(libs.androidx.multidex)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.lifecycle.service)

    implementation(libs.androidx.savedstate.ktx)
    implementation(libs.androidx.savedstate)

    implementation(libs.bundles.ktor)
    // qr scan
    implementation(libs.quickie.bundled)
    // Fab button with menu, please do not upgrade, download dependencies will be error after upgrade
    //noinspection GradleDependency
    implementation(libs.speed.dial.compose)
    // TextView markdown
    implementation(libs.markwon.core)
    implementation(libs.androidx.viewpager2)
    implementation(libs.coil.compose)

    debugImplementation(libs.leakcanary.android)

    implementation(libs.core.ktx)
}

fun copyTemplateToAPP(isDebug: Boolean, to: File) {
    val outName = if (isDebug) "template-debug" else "template-release"
    val outFile = project(":inrt").buildOutputs.named(outName).get().outputFile
    copy {
        from(outFile)
        into(to)
        delete(File(to, "template.apk"))
        rename(outFile.name, "template.apk")
    }
    logger.lifecycle("buildTemplate success, debugMode: $isDebug")
}

val assetsDir = File(projectDir, "src/main/assets")
if (!File(assetsDir, "template.apk").isFile) {
    tasks.named("preBuild").dependsOn("buildTemplateApp")
}

tasks.register("buildTemplateApp") {
    dependsOn(":inrt:assembleTemplateRelease")
    doFirst {
        copyTemplateToAPP(false, assetsDir)
    }
}
tasks.register("buildDebugTemplateApp") {
    dependsOn(":inrt:assembleTemplateDebug")
    doFirst {
        copyTemplateToAPP(true, assetsDir)
    }
}
tasks.named("clean").configure {
    doFirst {
        delete(File(assetsDir, "template.apk"))
    }
}
// 离线文档下载安装
val docsDir = File(projectDir, "src/main/assets/docs")
tasks.named("preBuild").dependsOn("installationDocumentation")
tasks.register("installationDocumentation") {
    val docV1Uri = "https://codeload.github.com/kkevsekk1/kkevsekk1.github.io/zip/refs/heads/main"
    val docV1Dir = File(docsDir, "v1")
    doFirst {
        if (File(docV1Dir, "index.html").isFile) {
            return@doFirst
        }
        okhttp3.OkHttpClient().newCall(Request.Builder().url(docV1Uri).build()).execute()
            .use { response ->
                check(response.isSuccessful) { "installationDocumentation failed" }
                val body = response.body!!
                ZipInputStream(body.byteStream()).use { zip ->
                    var zipEntry: ZipEntry?
                    while (true) {
                        zipEntry = zip.nextEntry ?: break
                        val file = File(docV1Dir, zipEntry.name.replaceFirst(Regex(".+?/"), ""))
                        if (zipEntry.isDirectory) {
                            file.mkdirs()
                        } else {
                            file.outputStream().use {
                                zip.copyTo(it)
                            }
                        }
                        zip.closeEntry()
                    }
                }
            }
    }
}
tasks.named("clean").configure {
    doFirst { delete(docsDir) }
}
