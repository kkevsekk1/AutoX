package org.autojs.autojs.devplugin

import android.annotation.SuppressLint
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.stardust.app.GlobalAppContext.toast
import com.stardust.autojs.execution.ScriptExecution
import com.stardust.autojs.project.ProjectLauncher
import com.stardust.autojs.script.StringScriptSource
import com.stardust.io.Zip
import com.stardust.pio.PFiles
import com.stardust.util.MD5
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.autojs.autojs.Pref
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.model.script.Scripts.run
import org.autojs.autoxjs.R
import java.io.*
import java.net.URLEncoder

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
                AutoJs.getInstance().scriptEngineService.stopAllAndToast()
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

    private val mScriptExecutions = HashMap<String, ScriptExecution?>()

    override fun handle(data: JsonObject): Boolean {
        return router.handle(data)
    }

    @Deprecated("use handleBytes1")
    fun handleBytes(data: JsonObject, bytes: Bytes): Observable<File> {
        val id = data["data"].asJsonObject["id"].asString
        val idMd5 = MD5.md5(id)
        return Observable.fromCallable {
            val dir = File(cacheDir, idMd5)
            Zip.unzip(ByteArrayInputStream(bytes.bytes), dir)
            dir
        }
            .subscribeOn(Schedulers.io())
    }

    suspend fun handleBytes1(data: JsonObject, bytes: Bytes): File = withContext(Dispatchers.IO) {
        val id = data["data"].asJsonObject["id"].asString
        val projectDir = URLEncoder.encode(id, Charsets.UTF_8.toString())
        val dir = File(cacheDir, projectDir)
        Zip.unzip(ByteArrayInputStream(bytes.bytes), dir)
        dir
    }

    private fun runScript(viewId: String, name: String, script: String) {
        val name1 = if (name.isEmpty()) "[$viewId]"
        else PFiles.getNameWithoutExtension(name)
        mScriptExecutions[viewId] = run(StringScriptSource("[remote]$name1", script))
    }

    private fun launchProject(dir: String) {
        try {
            ProjectLauncher(dir)
                .launch(AutoJs.getInstance().scriptEngineService)
        } catch (e: Exception) {
            e.printStackTrace()
            toast(R.string.text_invalid_project)
        }
    }

    private fun stopScript(viewId: String) {
        val execution = mScriptExecutions[viewId]
        execution?.engine?.forceStop()
        mScriptExecutions.remove(viewId)
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
        Observable.fromCallable {
            copyDir(File(dir), toDir)
            toDir.path
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { dest: String? -> toast(R.string.text_project_save_success, dest) }
            ) { err: Throwable -> toast(R.string.text_project_save_error, err.message) }
    }

    @Throws(FileNotFoundException::class)
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