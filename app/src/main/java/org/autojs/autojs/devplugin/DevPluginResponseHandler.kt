package org.autojs.autojs.devplugin

import android.annotation.SuppressLint
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.stardust.app.GlobalAppContext
import com.stardust.app.GlobalAppContext.toast
import com.stardust.autojs.project.ProjectConfig
import com.stardust.autojs.servicecomponents.BinderScriptListener
import com.stardust.autojs.servicecomponents.EngineController
import com.stardust.autojs.servicecomponents.TaskInfo
import com.stardust.io.Zip
import com.stardust.pio.PFiles
import com.stardust.util.MD5
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.autojs.autojs.Pref
import org.autojs.autoxjs.R
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Created by Stardust on 2017/5/11.
 */
class DevPluginResponseHandler(private val cacheDir: File) : Handler {

    companion object {
        val TAG = DevPluginResponseHandler::class.java.simpleName
    }

    private val router = Router.RootRouter("type")
        .handler("command", Router("command")
            .handler("run") { data: JsonObject ->
                val script = data["script"].asString
                val name = getName(data) ?: ""
                val id = data["id"].asString
                runScript(id, name, script)
                true
            }
            .handler("stop") { data: JsonObject ->
                val id = data["id"].asString
                stopScript(id)
                true
            }
            .handler("save") { data: JsonObject ->
                val script = data["script"].asString
                val name = getName(data) ?: ""
                saveScript(name, script)
                true
            }
            .handler("rerun") { data: JsonObject ->
                val id = data["id"].asString
                val script = data["script"].asString
                val name = getName(data) ?: ""
                try {
                    stopScript(id)
                } catch (e: Exception) {
                }
                runScript(id, name, script)
                true
            }
            .handler("stopAll") { data: JsonObject? ->
                EngineController.stopAllScript()
                true
            })
        .handler("bytes_command", Router("command")
            .handler("run_project") { data: JsonObject ->
                launchProject(data["dir"].asString)
                true
            }
            .handler("save_project") { data: JsonObject ->
                saveProject(data["name"].asString, data["dir"].asString)
                true
            })

    private val mScriptExecutions = HashMap<String, Int>()

    override fun handle(data: JsonObject): Boolean {
        return router.handle(data)
    }

    suspend fun handleBytes1(data: JsonObject, bytes: Bytes): File = withContext(Dispatchers.IO) {
        val id = data["data"].asJsonObject["id"].asString
        val projectDir = MD5.md5(id)
        val dir = File(cacheDir, projectDir)
        Zip.unzip(ByteArrayInputStream(bytes.bytes), dir)
        dir
    }

    private fun runScript(viewId: String, name: String, script: String) {
        val name1 = if (name.isEmpty()) "[$viewId].js"
        else PFiles.getName(name)
        val file = File(GlobalAppContext.get().cacheDir, "remote/remote-$name1")
        file.parentFile!!.mkdirs()
        file.writeText(script)
        EngineController.runScript(file, object : BinderScriptListener {
            override fun onStart(taskInfo: TaskInfo) {
                mScriptExecutions[viewId] = taskInfo.id
            }

            override fun onSuccess(taskInfo: TaskInfo) {
                mScriptExecutions.remove(viewId)
            }

            override fun onException(taskInfo: TaskInfo, e: Throwable) {
                mScriptExecutions.remove(viewId)
            }
        })

    }

    private fun launchProject(dir: String) {
        try {
            val project = ProjectConfig.fromProject(File(dir))
            EngineController.launchProject(project!!)
        } catch (e: Exception) {
            e.printStackTrace()
            toast(R.string.text_invalid_project)
        }
    }

    private fun stopScript(viewId: String) {
        val id = mScriptExecutions[viewId]
        if (id != null) {
            EngineController.stopScript(id)
        }
    }

    private fun getName(data: JsonObject): String? {
        val element = data["name"]
        return if (element is JsonNull) {
            null
        } else element.asString
    }

    private fun saveScript(name: String, script: String) {
        val name1 = if (name.isEmpty()) "untitled" else PFiles.getName(name)
        //PFiles.getNameWithoutExtension(name);
//        if (!name1.endsWith(".js")) {
//            name = name + ".js";
//        }
        val file = File(Pref.getScriptDirPath(), name1)
        PFiles.ensureDir(file.path)
        PFiles.write(file, script)
        toast(R.string.text_script_save_successfully)
    }

    @SuppressLint("CheckResult")
    private fun saveProject(name: String, dir: String) {
        val name1 = if (name.isEmpty()) "untitled" else PFiles.getNameWithoutExtension(name)
        val toDir = File(Pref.getScriptDirPath(), name1)
        CoroutineScope(Dispatchers.IO).launch {
            flow<String> {
                copyDir(File(dir), toDir)
                emit(toDir.path)
            }
                .flowOn(Dispatchers.Main)
                .catch {
                    toast(R.string.text_project_save_error, it.message)
                }.collect {
                    toast(R.string.text_project_save_success, it)
                }
        }
    }

    private fun copyDir(fromDir: File, toDir: File) {
        toDir.mkdirs()
        val files = fromDir.listFiles()
        if (files == null || files.isEmpty()) {
            return
        }
        for (file in files) {
            if (file.isDirectory) {
                copyDir(file, File(toDir, file.name))
            } else {
                val fos = FileOutputStream(File(toDir, file.name))
                PFiles.write(FileInputStream(file), fos, true)
            }
        }
    }

    init {
        if (cacheDir.exists()) {
            if (cacheDir.isDirectory) {
                PFiles.deleteFilesOfDir(cacheDir)
            } else {
                cacheDir.delete()
                cacheDir.mkdirs()
            }
        }
    }
}