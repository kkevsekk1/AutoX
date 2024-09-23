package org.autojs.autojs.external.tasker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aiselp.autox.ui.material3.components.BackTopAppBar
import com.aiselp.autox.ui.material3.theme.AppTheme
import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractAppCompatPluginActivity
import org.autojs.autojs.external.ScriptIntents
import org.autojs.autojs.model.explorer.ExplorerDirPage
import org.autojs.autojs.model.explorer.ExplorerItem
import org.autojs.autojs.model.explorer.Explorers
import org.autojs.autojs.ui.edit.EditorView
import org.autojs.autojs.ui.explorer.ExplorerViewKt
import org.autojs.autoxjs.R

/**
 * Created by Stardust on 2017/3/27.
 */
open class TaskPrefEditActivity : AbstractAppCompatPluginActivity() {
    private var mSelectedScriptFilePath: String? = null
    private var mPreExecuteScript: String? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launcher = registerForActivityResult(
            ScriptEditActivityResult(
                getString(R.string.text_pre_execute_script),
                getString(R.string.summary_pre_execute_script),
            )
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                mPreExecuteScript = result.data!!.getStringExtra(EditorView.EXTRA_CONTENT)
            }
        }
        val explorerViewKt = ExplorerViewKt(this).apply {
            setExplorer(
                Explorers.external(),
                ExplorerDirPage.createRoot(Environment.getExternalStorageDirectory())
            )
            setOnItemClickListener { _, item: ExplorerItem? ->
                mSelectedScriptFilePath = item!!.path
                finish()
            }
        }

        setContent {
            BackHandler {
                if (explorerViewKt.canGoBack()) {
                    explorerViewKt.goBack()
                } else finish()
            }
            AppTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    BackTopAppBar(title = stringResource(id = R.string.text_please_choose_a_script),
                        actions = {
                            TextButton(onClick = {
                                mPreExecuteScript = null
                                mSelectedScriptFilePath = null
                            }) {
                                Text(text = stringResource(id = R.string.text_clear))
                            }
                        }
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = mPreExecuteScript ?: "",
                            label = { Text(text = stringResource(id = R.string.text_pre_execute_script)) },
                            onValueChange = { mPreExecuteScript = it },
                            maxLines = 5,
                        )
                        Spacer(modifier = Modifier.width(1.dp))
                        ElevatedButton(onClick = { launcher.launch(mPreExecuteScript) }) {
                            Text(text = stringResource(id = R.string.text_edit))
                        }
                    }
                    AndroidView(factory = { explorerViewKt })
                }
            }
        }
    }

    override fun isBundleValid(bundle: Bundle): Boolean {
        return ScriptIntents.isTaskerBundleValid(bundle)
    }

    override fun onPostCreateWithPreviousResult(bundle: Bundle, s: String) {
        mSelectedScriptFilePath = bundle.getString(ScriptIntents.EXTRA_KEY_PATH)
        mPreExecuteScript = bundle.getString(ScriptIntents.EXTRA_KEY_PRE_EXECUTE_SCRIPT)
    }

    override fun getResultBundle(): Bundle? {
        val bundle = Bundle()
        bundle.putString(ScriptIntents.EXTRA_KEY_PATH, mSelectedScriptFilePath)
        bundle.putString(ScriptIntents.EXTRA_KEY_PRE_EXECUTE_SCRIPT, mPreExecuteScript)
        return bundle
    }

    override fun getResultBlurb(bundle: Bundle): String {
        var blurb = bundle.getString(ScriptIntents.EXTRA_KEY_PATH)
        if (TextUtils.isEmpty(blurb)) {
            blurb = bundle.getString(ScriptIntents.EXTRA_KEY_PRE_EXECUTE_SCRIPT)
        }
        if (TextUtils.isEmpty(blurb)) {
            blurb = getString(R.string.text_path_is_empty)
        }
        return blurb!!
    }
    class ScriptEditActivityResult(val title: String, val summary: String) :
        ActivityResultContract<String?, ActivityResult>() {
        override fun createIntent(context: Context, input: String?): Intent {
            return Intent(context, TaskerScriptEditActivity::class.java)
                .putExtra(EditorView.EXTRA_CONTENT, input ?: "")
                .putExtra("summary", summary)
                .putExtra(EditorView.EXTRA_NAME, title)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): ActivityResult {
            return ActivityResult(resultCode, intent)
        }
    }
}
