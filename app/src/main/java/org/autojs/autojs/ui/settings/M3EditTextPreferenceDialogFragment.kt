package org.autojs.autojs.ui.settings

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.preference.EditTextPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class M3EditTextPreferenceDialogFragment(val preference: EditTextPreference) : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(preference.dialogTitle)
            .setIcon(preference.dialogIcon)
            .setPositiveButton(preference.positiveButtonText) { _, _ ->
            }
            .setNegativeButton(preference.negativeButtonText) { _, _ ->
            }
        val view = onCreateDialogView()
        if (view != null) {
            onBindDialogView(view)
            builder.setView(view)
        } else {
            builder.setMessage(preference.dialogMessage)
        }
        return builder.create()
    }

    open fun onCreateDialogView(): View? {
        return if (preference.dialogLayoutResource != 0) {
            layoutInflater.inflate(preference.dialogLayoutResource, null)
        } else null
    }

    open fun onBindDialogView(view: View) {
        val editText = view.findViewById<EditText>(android.R.id.edit)
        checkNotNull(editText) {
            IllegalStateException(
                "Dialog view must contain an EditText with id" + " @android:id/edit"
            )
        }
        editText.setText(preference.text)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
    }
}