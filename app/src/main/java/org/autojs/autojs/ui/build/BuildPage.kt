package org.autojs.autojs.ui.build

import android.content.Context
import android.os.Environment
import android.text.InputType
import android.util.Log
import android.view.View
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.stardust.app.DialogUtils
import com.stardust.pio.PFile
import com.stardust.pio.PFiles
import org.autojs.autojs.Pref
import org.autojs.autojs.build.ApkKeyStore
import org.autojs.autojs.build.ApkSigner
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder
import org.autojs.autojs.ui.filechooser.FileChooserDialogBuilder
import org.autojs.autoxjs.R
import java.io.File

fun buildApk(model: BuildViewModel, context: Context, apkKeyStore: ApkKeyStore?) {
//    if (!model.checkInputs()) {
//        toast(context, R.string.text_invalid_config)
//        return
//    }
    if (apkKeyStore != null && !apkKeyStore.isVerified) {
        showPasswordInputDialog(
            context = context,
            keyStore = apkKeyStore,
            chooseDialog = null,
            onComplete = {
                model.buildApk()
            }
        )
    } else model.buildApk()
}

@Composable
fun MyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape =
        MaterialTheme.shapes.small.copy(bottomEnd = ZeroCornerSize, bottomStart = ZeroCornerSize),
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(
        backgroundColor = Color.Transparent,
        textColor = MaterialTheme.colors.onSurface
    )
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors
    )
}

fun selectSourceFilePath(context: Context, scriptPath: String, onResult: (File) -> Unit) {
    val initialDir = File(scriptPath).parent
    FileChooserDialogBuilder(context)
        .title(R.string.text_source_file_path)
        .dir(
            Environment.getExternalStorageDirectory().path,
            initialDir ?: Pref.getScriptDirPath()
        )
        .singleChoice { file: PFile -> onResult(file) }
        .show()
}

fun selectOutputDirPath(context: Context, outputPath: String, onResult: (File) -> Unit) {
    val initialDir =
        if (File(outputPath).exists()) outputPath else Pref.getScriptDirPath()
    FileChooserDialogBuilder(context)
        .title(R.string.text_output_apk_path)
        .dir(initialDir)
        .chooseDir()
        .singleChoice { dir: PFile -> onResult(dir) }
        .show()
}

fun chooseSign(
    context: Context,
    currentKeyStore: ApkKeyStore?,
    onKeyStoreChange: (ApkKeyStore?) -> Unit,
) {
    val keyStoreList = ApkSigner.loadKeyStore()
    //默认选中位置
    val selectedIndex = currentKeyStore?.let { getSelectIndex(it, keyStoreList) } ?: 0

    DialogUtils.showDialog(
        ThemeColorMaterialDialogBuilder(context)
            .title(R.string.text_sign_choose)
            .items(getSignItems(keyStoreList))
            .autoDismiss(false)
            .itemsCallbackSingleChoice(selectedIndex) { dialog: MaterialDialog?, itemView: View?, position: Int, text: CharSequence? ->
                if (position <= 0) {
                    onKeyStoreChange(null)
                    return@itemsCallbackSingleChoice true
                } else {
                    val keyStore = keyStoreList[position - 1]
                    onKeyStoreChange(keyStore)
                    //如果已保存密码
                    if (keyStore.isVerified) {
                        return@itemsCallbackSingleChoice true
                    }
                    //否则输入密码
                    showPasswordInputDialog(context, keyStore, dialog, {})
                    return@itemsCallbackSingleChoice false
                }
            }
            .negativeText(R.string.cancel)
            .onNegative { d: MaterialDialog, w: DialogAction? -> d.dismiss() }
            .negativeColorRes(R.color.text_color_secondary)
            .neutralText(R.string.text_sign_manage)
            .positiveText(R.string.ok)
            .onPositive { d: MaterialDialog, w: DialogAction? -> d.dismiss() }
            .onNeutral { d: MaterialDialog?, w: DialogAction? ->
                context.startActivity(SignManageActivityKt.getIntent(context))
            }
            .build())
}

private fun getSelectIndex(keyStore: ApkKeyStore, keyStoreList: List<ApkKeyStore>): Int {
    var index = 0
    val len = keyStoreList.size
    for (i in 1..len) {
        val item = keyStoreList[i - 1]
        if (keyStore.name == null) return -1
        Log.d(BuildActivity.TAG, "name: ->" + item.name)
        Log.d(BuildActivity.TAG, "name: =>" + keyStore.name)
        Log.d(BuildActivity.TAG, "contains: =>" + keyStore.name?.let { item.name!!.contains(it) })

        if (item.name != null && item.name!!.contains(PFiles.getName(keyStore.name!!))) {
            index = i
            Log.d(BuildActivity.TAG, "index: =>$index")
            break
        }
    }
    return index
}

private fun getSignItems(keyStoreList: List<ApkKeyStore>): List<String?> {
    val list: MutableList<String?> = ArrayList()
    list.add("默认签名")
    for (item in keyStoreList) {
        list.add(item.name)
    }
    return list
}

fun showPasswordInputDialog(
    context: Context,
    keyStore: ApkKeyStore,
    chooseDialog: MaterialDialog?,
    onComplete: (String) -> Unit
) {
    DialogUtils.showDialog(ThemeColorMaterialDialogBuilder(context).title(R.string.text_sign_password)
        .inputType(InputType.TYPE_TEXT_VARIATION_PASSWORD)
        .autoDismiss(false)
        .canceledOnTouchOutside(false)
        .input(
            context.getString(R.string.text_sign_password_input),
            "",
            false
        ) { dialog, input -> }
        .onPositive { dialog: MaterialDialog, which: DialogAction? ->
            val password = dialog.inputEditText!!
                .text.toString()
            if (ApkSigner.checkKeyStore(keyStore.path, password)) {
//                Pref.setKeyStorePassWord(PFiles.getName(keyStore.path), password)
                dialog.dismiss()
                chooseDialog?.dismiss()
                keyStore.isVerified = true
                onComplete(password)
            } else {
                dialog.inputEditText!!.error = context.getString(R.string.text_verification_failed)
            }
        }
        .onNeutral { dialog: MaterialDialog, which: DialogAction? -> dialog.dismiss() }
        .build())
}

