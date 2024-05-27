pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        //首选国外镜像加快github CI
        google()
        mavenCentral()
        maven("https://www.jitpack.io")
        maven("https://120.25.164.233:8081/nexus/content/groups/public/")
        maven("https://maven.aliyun.com/repository/central")
        google { url = uri("https://maven.aliyun.com/repository/google") }
        mavenCentral { url = uri("https://maven.aliyun.com/repository/public") }
    }
}
rootProject.name = "AutoX"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
include(":app")
include(":automator")
include(":common")
include(":autojs")
include(":inrt")
include(":apkbuilder")
include(":LocalRepo:libtermexec")
include(":LocalRepo:emulatorview")
include(":LocalRepo:term")
include(":LocalRepo:p7zip")
include(":LocalRepo:OpenCV")
include(":paddleocr")
include(":codeeditor")
include(":core:model")
include(":core:network")
