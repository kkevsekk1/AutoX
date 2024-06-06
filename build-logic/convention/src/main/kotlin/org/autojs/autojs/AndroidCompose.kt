package org.autojs.autojs

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        buildFeatures {
            compose = true
        }

        dependencies {
            val bom = libs.findLibrary("androidx-compose-bom").get()
            add("implementation", platform(bom))
            add("androidTestImplementation", platform(bom))
            add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
            add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
        }

        testOptions {
            unitTests {
                // For Robolectric
                isIncludeAndroidResources = true
            }
        }
    }
    extensions.configure<ComposeCompilerGradlePluginExtension> {
        fun relativeToRootProject(dir: String) =
            rootProject.layout.buildDirectory.dir(projectDir.toRelativeString(rootDir))
                .map { it.dir(dir) }

        metricsDestination = relativeToRootProject("compose-metrics")
        reportsDestination = relativeToRootProject("compose-reports")
        stabilityConfigurationFile =
            rootProject.layout.projectDirectory.file("compose_compiler_config.conf")

        enableStrongSkippingMode = true
        includeSourceInformation = true
        enableNonSkippingGroupOptimization = true
    }
}

