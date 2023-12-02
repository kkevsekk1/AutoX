package com.aiselp.autojs.codeeditor.dialogs

import android.app.Activity
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.aiselp.autojs.codeeditor.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoadDialog(activity: Activity) {
    val dialog: AlertDialog = AlertDialog.Builder(
        activity
    )
        .setTitle("加载中")
        .setView(R.layout.load)
        .setCancelable(false)
        .create()
    val textView: TextView by lazy {
        dialog.findViewById(R.id.textView)!!
    }

    init {
    }

    suspend fun setContent(text: String) {
        withContext(Dispatchers.Main) {
            textView.text = text
        }
    }

    fun show() {
        dialog.show()
    }
}