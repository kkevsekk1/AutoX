package com.aiselp.autox.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.Render
import com.aiselp.autox.ui.material3.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppDialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val builder = intent.getLongExtra(TAG, -1).let {
            if (it == -1L || !dialogs.containsKey(it)) {
                Log.e(TAG, "onCreate: builder is null")
                return finish()
            }
            dialogs.remove(it)!!
        }
        builder.build(this)
    }

    abstract class AppDialogBuilder(val el: ComposeElement, val scope: CoroutineScope) {
        var show by mutableStateOf(true)
        open val dismissOnBackPress = true
        open val dismissOnClickOutside = true
        open val securePolicy: SecureFlagPolicy = SecureFlagPolicy.Inherit

        abstract fun onDismiss()
        fun dismiss() = scope.launch(Dispatchers.Main) {
            show = false
        }

        fun build(activity: AppCompatActivity) {
            activity.setContent {
                if (!show) {
                    LaunchedEffect(Unit) {
                        onDismiss()
                        activity.finish()
                    }
                    return@setContent
                }
                AppTheme(dynamicColor = false) {
                    Dialog(
                        onDismissRequest = { show = false },
                        properties = DialogProperties(
                            dismissOnBackPress = dismissOnBackPress,
                            dismissOnClickOutside = dismissOnClickOutside,
                            securePolicy = securePolicy,
                        )
                    ) {
                        el.Render()
                    }
                }
            }
        }
    }

    companion object {
        private var id = 0L
        private val dialogs = mutableMapOf<Long, AppDialogBuilder>()
        fun showDialog(context: Context, builder: AppDialogBuilder, scope: CoroutineScope) =
            scope.launch(Dispatchers.Main) {
                val eid = id++
                dialogs[eid] = builder
                context.startActivity(
                    Intent(context, AppDialogActivity::class.java)
                        .putExtra(TAG, eid)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }

        private const val TAG = "AppDialogActivity"
    }
}