package org.autojs.autojs.ui.shortcut

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

class ShortcutIconSelectResult: ActivityResultContract<Void?, Uri?>() {
    override fun createIntent(context: Context, input: Void?): Intent {
        return Intent(context,ShortcutIconSelectActivity_::class.java)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent?.data
    }
}