package org.autojs.autojs.ui.project

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.text.InputType
import android.util.Log
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.stardust.app.DialogUtils
import com.stardust.pio.PFile
import com.stardust.pio.PFiles
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.launch
import org.autojs.autojs.Pref
import org.autojs.autojs.R
import org.autojs.autojs.build.ApkKeyStore
import org.autojs.autojs.build.ApkSigner
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder
import org.autojs.autojs.ui.filechooser.FileChooserDialogBuilder
import org.autojs.autojs.ui.shortcut.ShortcutIconSelectActivity
import org.autojs.autojs.ui.shortcut.ShortcutIconSelectActivity_
import java.io.File

@Composable
fun BuildPage(
    onKeyStoreChange: (ApkKeyStore?) -> Unit,
) {
    val model: BuildViewModel = viewModel()
    Scaffold(
        topBar = {
            TopBar()
        },
        floatingActionButton = {
            Fab()
        },
        floatingActionButtonPosition = FabPosition.End
    ) {
        Column(
            Modifier
                .padding(it)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            FileCard(model)
            ConfigCard(model)
            RunConfigCard(model)
            SignatureCard(onKeyStoreChange, model)
        }
    }
}

@Composable
private fun Fab() {
    FloatingActionButton(onClick = { /*TODO*/ }) {
        Icon(
            imageVector = Icons.Default.Done,
            contentDescription = stringResource(R.string.desc_done)
        )
    }
}

@Composable
private fun TopBar() {
    val context = LocalContext.current
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.text_build_apk)) },
        navigationIcon = {
            IconButton(onClick = { if (context is Activity) context.finish() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(id = R.string.desc_back)
                )
            }
        }
    )
}

@Composable
private fun SignatureCard(
    onKeyStoreChange: (ApkKeyStore?) -> Unit,
    model: BuildViewModel
) {
    val context = LocalContext.current

    Card(modifier = Modifier.padding(6.dp)) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(text = model.appSignKeyPath.ifEmpty { stringResource(id = R.string.text_default_signature) })
            TextButton(onClick = {
                chooseSign(
                    context = context,
                    onKeyStoreChange = onKeyStoreChange,
                    onAppSignKeyPathChange = { model.appSignKeyPath = it }
                )
            }) {
                Text(text = stringResource(id = R.string.text_sign_choose))
            }
        }
    }
}

@Composable
private fun RunConfigCard(model: BuildViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var icon by model::splashIconDrawable
    val selectIconLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.let { intent ->
                    scope.launch {
                        ShortcutIconSelectActivity.getBitmapFromIntent1(
                            context,
                            intent
                        )?.let { icon = it }
                    }
                }
            }
        }
    )

    Card(modifier = Modifier.padding(6.dp)) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(text = stringResource(id = R.string.text_run_config))
            NextActionTextField(
                value = model.mainScriptFile,
                onValueChange = { model.mainScriptFile = it },
                label = { Text(text = stringResource(id = R.string.text_main_file_name)) },
            )
            Row {
                Checkbox(
                    checked = model.isHideLauncher,
                    onCheckedChange = { model.isHideLauncher = it })
                Text(text = stringResource(id = R.string.text_hideLaucher))
            }
            Row {
                Checkbox(
                    checked = model.isStableMode,
                    onCheckedChange = { model.isStableMode = it })
                Text(text = stringResource(id = R.string.text_stable_mode))
            }
            Row {
                Checkbox(
                    checked = model.isHideLogs,
                    onCheckedChange = { model.isHideLogs = it })
                Text(text = stringResource(id = R.string.text_hideLogs))
            }
            NextActionTextField(
                value = model.splashText,
                onValueChange = { model.splashText = it },
                label = { Text(text = stringResource(id = R.string.text_splash_text)) }
            )
            Text(text = stringResource(id = R.string.text_splash_icon))

            AsyncImage(
                model = if (icon != null) icon else painterResource(id = R.drawable.ic_add_white_48dp),
                contentDescription = stringResource(R.string.apk_icon),
                modifier = Modifier
                    .size(64.dp)
                    .clickable {
                        selectIconLauncher.launch(
                            ShortcutIconSelectActivity_
                                .intent(context)
                                .get()
                        )
                    }
            )

            TextField(
                value = model.serviceDesc,
                onValueChange = { model.serviceDesc = it },
                label = { Text(text = stringResource(id = R.string.text_service_desc_text)) },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                singleLine = true
            )
        }
    }
}

@Composable
private fun ConfigCard(model: BuildViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var icon by model::iconDrawable
    val selectIconLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            it.data?.let { intent ->
                scope.launch {
                    ShortcutIconSelectActivity.getBitmapFromIntent1(
                        context,
                        intent
                    )?.let { icon = it }
                }
            }
        }
    )

    Card(modifier = Modifier.padding(6.dp)) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(text = stringResource(id = R.string.text_config))
            NextActionTextField(
                value = model.appName,
                onValueChange = { model.appName = it },
                label = { Text(text = stringResource(id = R.string.text_app_name)) }
            )
            NextActionTextField(
                value = model.packageName,
                onValueChange = { model.packageName = it },
                label = { Text(text = stringResource(id = R.string.text_package_name)) }
            )
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    NextActionTextField(
                        value = model.versionName,
                        onValueChange = { model.versionName = it },
                        label = { Text(text = stringResource(id = R.string.text_version_name)) }
                    )

                    val regex = remember {
                        Regex("^$|^[0-9]{0,20}$")
                    }
                    NextActionTextField(
                        value = model.versionCode,
                        onValueChange = { if (regex.matches(it)) model.versionCode = it },
                        label = { Text(text = stringResource(id = R.string.text_version_code)) }
                    )
                }
                Column {
                    Text(text = stringResource(id = R.string.text_icon))
                    Box {
                        AsyncImage(
                            model = if (icon != null) icon else painterResource(id = R.drawable.ic_add_white_48dp),
                            contentDescription = stringResource(R.string.apk_icon),
                            modifier = Modifier
                                .size(64.dp)
                                .align(alignment = Alignment.Center)
                                .clickable {
                                    selectIconLauncher.launch(
                                        ShortcutIconSelectActivity_
                                            .intent(context)
                                            .get()
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileCard(model: BuildViewModel) {
    val context = LocalContext.current
    Card(modifier = Modifier.padding(6.dp)) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(text = "文件")
            Row(Modifier.fillMaxWidth()) {
                NextActionTextField(
                    value = model.scriptPath,
                    onValueChange = { model.scriptPath = it },
                    label = { Text(text = stringResource(id = R.string.text_source_file_path)) },
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        selectSourceFilePath(
                            context = context,
                            scriptPath = model.scriptPath,
                            onResult = { model.scriptPath = it.absolutePath },
                        )
                    },
                ) {
                    Text(text = stringResource(id = R.string.text_select))
                }
            }
            Row(Modifier.fillMaxWidth()) {
                NextActionTextField(
                    value = model.outputPath,
                    onValueChange = { model.outputPath = it },
                    label = { Text(text = stringResource(id = R.string.text_output_apk_path)) },
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    selectOutputDirPath(
                        context = context,
                        outputPath = model.outputPath,
                        onResult = { model.outputPath = it.absolutePath },
                    )
                }) {
                    Text(text = stringResource(id = R.string.text_select))
                }
            }

        }
    }
}

@Composable
fun NextActionTextField(
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
    maxLines: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape =
        MaterialTheme.shapes.small.copy(bottomEnd = ZeroCornerSize, bottomStart = ZeroCornerSize),
    colors: TextFieldColors = TextFieldDefaults.textFieldColors()
) {
    val focusManager = LocalFocusManager.current
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions.copy(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = {
            focusManager.moveFocus(FocusDirection.Next)
        }),
        singleLine = true,
        maxLines = maxLines,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors
    )
}

@Composable
fun AddIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    Surface(color = Color(0xff727379), shape = RoundedCornerShape(4.dp)) {
        IconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            interactionSource = interactionSource
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add_white_48dp),
                contentDescription = stringResource(R.string.desc_add_icon)
            )
        }
    }
}

private fun selectSourceFilePath(context: Context, scriptPath: String, onResult: (File) -> Unit) {
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

private fun selectOutputDirPath(context: Context, outputPath: String, onResult: (File) -> Unit) {
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
    onKeyStoreChange: (ApkKeyStore?) -> Unit,
    onAppSignKeyPathChange: (String) -> Unit
) {
    val keyStoreList = ApkSigner.loadKeyStore()
    val selectedIndex = getSelectIndex(keyStore, keyStoreList)

    DialogUtils.showDialog(
        ThemeColorMaterialDialogBuilder(context)
            .title(R.string.text_sign_choose)
            .items(getSignItems(keyStoreList))
            .autoDismiss(false)
            .itemsCallbackSingleChoice(selectedIndex) { dialog: MaterialDialog?, itemView: View?, which: Int, text: CharSequence? ->
                if (which <= 0) {
                    onKeyStoreChange(null)
                    return@itemsCallbackSingleChoice true
                } else {
                    onKeyStoreChange(keyStoreList!![which - 1])
                    onAppSignKeyPathChange(keyStore.path)
                    if (keyStore.isVerified) {
                        return@itemsCallbackSingleChoice true
                    }
                    showPasswordInputDialog(context, keyStore, dialog,{})
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
                SignManageActivity_.intent(context).start()
            }
            .build())
}

private fun getSelectIndex(keyStore: ApkKeyStore, mKeyStoreList: List<ApkKeyStore>): Int {
    var index = 0
    val len = mKeyStoreList.size
    for (i in 1..len) {
        val item = mKeyStoreList[i - 1]
        Log.d(BuildActivity.LOG_TAG, "name: ->" + item.name)
        Log.d(BuildActivity.LOG_TAG, "name: =>" + keyStore.name)
        Log.d(BuildActivity.LOG_TAG, "contains: =>" + item.name.contains(keyStore.name))
        if (item.name.contains(PFiles.getName(keyStore.name))) {
            index = i
            Log.d(BuildActivity.LOG_TAG, "index: =>$index")
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
                Pref.setKeyStorePassWord(PFiles.getName(keyStore.path), password)
                dialog.dismiss()
                chooseDialog?.dismiss()
                keyStore.isVerified = true
                onComplete(password)
            } else {
                dialog.inputEditText!!.error = "验证失败"
            }
        }
        .onNeutral { dialog: MaterialDialog, which: DialogAction? -> dialog.dismiss() }
        .build())
}

