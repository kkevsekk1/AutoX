package com.stardust.autojs.project

import com.stardust.autojs.ScriptEngineService
import com.stardust.autojs.execution.ExecutionConfig
import com.stardust.autojs.script.JavaScriptFileSource
import java.io.File

/**
 * Modified by wilinz on 2022/5/23
 */
class ProjectLauncher(private val mProjectDir: String) {
    private val mProjectConfig: ProjectConfig = ProjectConfig.fromProjectDir(mProjectDir)!!
    private val mMainScriptFile: File = File(mProjectDir, mProjectConfig.mainScript)
    fun launch(service: ScriptEngineService) {
        val config = ExecutionConfig()
        config.workingDirectory = mProjectDir
        config.scriptConfig.features = mProjectConfig.features
        service.execute(JavaScriptFileSource(mMainScriptFile), config)
    }

}