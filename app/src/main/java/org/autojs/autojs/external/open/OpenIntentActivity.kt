package org.autojs.autojs.external.open

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stardust.autojs.script.StringScriptSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.autojs.autojs.Pref
import org.autojs.autojs.external.ScriptIntents
import org.autojs.autojs.model.script.Scripts
import org.autojs.autojs.ui.BaseActivity
import org.autojs.autojs.ui.edit.EditActivity
import org.autojs.autojs.ui.log.LogActivityKt
import org.autojs.autoxjs.R
import java.io.File

/**
 * Created by Stardust on 2017/2/2.
 */
class OpenIntentActivity : BaseActivity() {
    override val layoutId = R.layout.empty_layout

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private lateinit var menus: Map<String, (file: Uri) -> Job?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menus = mapOf(
            getString(R.string.text_edit_script) to ::editFile,
            getString(R.string.text_edit_script) + "(新编辑器)" to ::editFile2,
            getString(R.string.text_import_script) to ::importFile,
            getString(R.string.text_run_script) to ::runFile,
        )
        onNewIntent(intent)
    }

    private suspend fun showMenu(intent: Intent) {
        val uri = intent.data ?: return finish()
        val fileName = File(uri.path!!).name
        val items = menus.keys.toTypedArray()
        val item = CompletableDeferred<String>()
        MaterialAlertDialogBuilder(this)
            .setTitle(fileName)
            .setItems(items) { dialog, which ->
                dialog.dismiss()
                item.complete(items[which])
            }
            .setOnDismissListener { item.cancel("canceled") }
            .show()
        menus[item.await()]?.invoke(uri)?.join()
    }

    private fun editFile(file: Uri): Job {
        val path = file.path!!
        val job = Job()
        job.complete()
        if (file.scheme == "file" && File(path).isFile()) {
            EditActivity.editFile(this, path, false)
        } else {
            EditActivity.editFile(this, file, false)
        }
        return job
    }

    private fun editFile2(file: Uri): Job? {
        val path = file.path!!
        if (file.scheme == "file" && File(path).isFile()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                com.aiselp.autojs.codeeditor.EditActivity.editFile(this, File(path))
            }
            return Job().apply { complete() }
        }
        showToast(R.string.edit_and_run_handle_intent_error)
        return null
    }

    private fun importFile(file: Uri): Job {
        val fileName = File(file.path!!).name
        val scriptDirPath = Pref.getScriptDirPath()
        val job = Job()
        val editText = EditText(this).apply {
            setInputType(InputType.TYPE_CLASS_TEXT)
            setHint("请输入文件名")
            setText(fileName)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.text_name)
            .setView(editText)
            .setPositiveButton(R.string.ok) { _, _ ->
                coroutineScope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            val newFile = File(scriptDirPath, editText.text.toString())
                            check(!newFile.exists()) { getString(R.string.text_file_exists) }
                            when (file.scheme) {
                                "file" -> {
                                    File(file.path!!).copyTo(newFile, overwrite = false)
                                }

                                "content" -> {
                                    this@OpenIntentActivity.contentResolver.openInputStream(file)
                                        .use {
                                            check(it != null) { "importFile failed" }
                                            newFile.outputStream().use { out ->
                                                it.copyTo(out)
                                            }
                                        }
                                }

                                else -> {
                                    cancel(getString(R.string.edit_and_run_handle_intent_error))
                                }
                            }
                        }
                    }.onSuccess {
                        job.complete()
                        showToast(R.string.text_import_succeed)
                    }.onFailure {
                        showToast(it.message ?: "unknown error")
                    }
                }
            }
            .setOnDismissListener { job.cancel("canceled") }
            .show()
        return job
    }

    private fun runFile(file: Uri) = coroutineScope.launch {
        when (file.scheme) {
            "file" -> {
                ScriptIntents.handleIntent(this@OpenIntentActivity, intent)
            }

            "content" -> withContext(Dispatchers.IO) {
                this@OpenIntentActivity.contentResolver.openInputStream(file)
                    .use {
                        check(it != null) { "runFile failed" }
                        it.bufferedReader().readText()
                    }.let { Scripts.run(StringScriptSource(it)) }
            }

            else -> throw IllegalArgumentException("unknown scheme: ${file.scheme}")
        }
        LogActivityKt.start(this@OpenIntentActivity)
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun showToast(resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_LONG).show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        coroutineScope.launch {
            runCatching {
                showMenu(intent)
            }.onFailure {
                it.printStackTrace()
                Toast.makeText(
                    this@OpenIntentActivity,
                    R.string.edit_and_run_handle_intent_error,
                    Toast.LENGTH_LONG
                ).show()
            }
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

}