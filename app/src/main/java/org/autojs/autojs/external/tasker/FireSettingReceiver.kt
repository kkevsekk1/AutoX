package org.autojs.autojs.external.tasker

import android.content.Context
import android.os.Bundle
import com.stardust.autojs.script.StringScriptSource
import com.twofortyfouram.locale.sdk.client.receiver.AbstractPluginSettingReceiver
import org.autojs.autojs.external.ScriptIntents
import org.autojs.autojs.model.script.ScriptFile
import org.autojs.autojs.model.script.Scripts

/**
 * Created by Stardust on 2017/3/27.
 */
class FireSettingReceiver : AbstractPluginSettingReceiver() {

    override fun isBundleValid(bundle: Bundle): Boolean {
        return ScriptIntents.isTaskerBundleValid(bundle)
    }

    override fun isAsync(): Boolean {
        return true
    }

    override fun firePluginSetting(context: Context, bundle: Bundle) {
        val path = bundle.getString(ScriptIntents.EXTRA_KEY_PATH)
        val script = bundle.getString(ScriptIntents.EXTRA_KEY_PRE_EXECUTE_SCRIPT)

        if (path != null) {
            Scripts.run(ScriptFile(path))
        }else if (script != null) {
            Scripts.run(StringScriptSource(script))
        }
    }

    companion object {
        private const val TAG = "FireSettingReceiver"
    }
}
