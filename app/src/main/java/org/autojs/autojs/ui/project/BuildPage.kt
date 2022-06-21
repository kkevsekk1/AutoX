package org.autojs.autojs.ui.project

import android.app.Activity
import android.content.Context
import android.os.Environment
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.stardust.app.DialogUtils
import com.stardust.pio.PFile
import com.stardust.pio.PFiles
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

/**
 * @author wilinz
 * @date 2022/5/23
 */
@Composable
fun BuildPage() {
    val model: BuildViewModel = viewModel()
    Scaffold(
        topBar = {
            TopBar()
        },
        floatingActionButton = {
            Fab(model)
        },
        floatingActionButtonPosition = FabPosition.End
    ) {
        Column(
            Modifier
                .padding(it)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FileCard(model)
            ConfigCard(model)
            CompileOptionCard(model)
            RunConfigCard(model)
            SignatureCard(model)
        }
    }
}

@Composable
private fun Fab(model: BuildViewModel) {
    val context = LocalContext.current
    FloatingActionButton(onClick = {
        if (context is BuildActivity) {
            Toast.makeText(context, "开始构建", Toast.LENGTH_SHORT).show()
            context.buildApk(model.keyStore)
        }
    }) {
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
private fun CompileOptionCard(
    model: BuildViewModel
) {
    val context = LocalContext.current
    Card() {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(text = stringResource(R.string.compile_options))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = model.isRequiredOpenCv,
                    onCheckedChange = { model.isRequiredOpenCv = it })
                Text(text = stringResource(id = R.string.text_required_opencv))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = model.isRequiredOCR,
                    onCheckedChange = { model.isRequiredOCR = it })
                Text(text = stringResource(id = R.string.text_required_ocr))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = model.isRequired7Zip,
                    onCheckedChange = { model.isRequired7Zip = it })
                Text(text = stringResource(id = R.string.text_required_7zip))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = model.isRequiredDefaultOcrModel,
                    onCheckedChange = { model.isRequiredDefaultOcrModel = it })
                Text(text = stringResource(id = R.string.text_required_default_ocr_model))
            }
            //目前必须为true
            /*Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = model.isRequiredTerminalEmulator,
                    onCheckedChange = { model.isRequiredTerminalEmulator = it })
                Text(text = stringResource(id = R.string.text_required_terminal_emulator))
            }*/
        }
    }
}

@Composable
private fun SignatureCard(
    model: BuildViewModel
) {
    val context = LocalContext.current

    Card() {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = model.appSignKeyPath ?: stringResource(id = R.string.text_default_signature)
            )
            TextButton(onClick = {
                chooseSign(
                    context = context,
                    currentKeyStore = model.keyStore,
                    onKeyStoreChange = { model.keyStore = it;model.appSignKeyPath = it?.path }
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
                        ShortcutIconSelectActivity.getBitmapFromIntent2(
                            context,
                            intent
                        )?.let { icon = it }
                    }
                }
            }
        }
    )

    Card() {
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
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = model.isHideLauncher,
                    onCheckedChange = { model.isHideLauncher = it })
                Text(text = stringResource(id = R.string.text_hideLaucher))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = model.isStableMode,
                    onCheckedChange = { model.isStableMode = it })
                Text(text = stringResource(id = R.string.text_stable_mode))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = model.isHideLogs,
                    onCheckedChange = { model.isHideLogs = it })
                Text(text = stringResource(id = R.string.text_hideLogs))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = model.isVolumeUpControl,
                    onCheckedChange = { model.isVolumeUpControl = it })
                Text(text = stringResource(id = R.string.text_volumeUpcontrol))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = model.isRequiredAccessibilityServices,
                    onCheckedChange = { model.isRequiredAccessibilityServices = it })
                Text(text = stringResource(id = R.string.text_required_accessibility_service))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = model.isRequiredBackgroundStart,
                    onCheckedChange = { model.isRequiredBackgroundStart = it })
                Text(text = stringResource(id = R.string.text_required_background_start))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = model.isRequiredDrawOverlay,
                    onCheckedChange = { model.isRequiredDrawOverlay = it })
                Text(text = stringResource(id = R.string.text_required_draw_overlay))
            }
            MyTextField(
                value = model.splashText,
                onValueChange = { model.splashText = it },
                label = { Text(text = stringResource(id = R.string.text_splash_text)) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(id = R.string.text_splash_icon))

            val modifier = Modifier
                .size(64.dp)
                .clickable {
                    selectIconLauncher.launch(
                        ShortcutIconSelectActivity_
                            .intent(context)
                            .get()
                    )
                }

            if (icon == null) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add_white_48dp),
                    contentDescription = stringResource(R.string.apk_icon),
                    modifier = modifier,
                    tint = MaterialTheme.colors.primary
                )
            } else {
                Image(
                    painter = rememberAsyncImagePainter(model = icon),
                    contentDescription = stringResource(R.string.apk_icon),
                    modifier = modifier,
                )
            }


            MyTextField(
                value = model.serviceDesc,
                onValueChange = { model.serviceDesc = it },
                label = { Text(text = stringResource(id = R.string.text_service_desc_text)) },
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
                    ShortcutIconSelectActivity.getBitmapFromIntent2(
                        context,
                        intent
                    )?.let { icon = it }
                }
            }
        }
    )

    Card() {
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
                Column(Modifier.padding(top = 8.dp)) {
                    Text(text = stringResource(id = R.string.text_icon))
                    Box {
                        val modifier = Modifier
                            .size(64.dp)
                            .align(Alignment.Center)
                            .clickable {
                                selectIconLauncher.launch(
                                    ShortcutIconSelectActivity_
                                        .intent(context)
                                        .get()
                                )
                            }

                        if (icon == null) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_add_white_48dp),
                                contentDescription = stringResource(R.string.apk_icon),
                                modifier = modifier,
                                tint = MaterialTheme.colors.primary
                            )
                        } else {
                            Image(
                                painter = rememberAsyncImagePainter(model = icon),
                                contentDescription = stringResource(R.string.apk_icon),
                                modifier = modifier,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileCard(model: BuildViewModel) {
    val context = LocalContext.current
    Card() {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(text = stringResource(id = R.string.text_file))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MyTextField(
                    value = model.sourcePath,
                    onValueChange = { model.sourcePath = it },
                    label = { Text(text = stringResource(id = R.string.text_source_file_path)) },
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        selectSourceFilePath(
                            context = context,
                            scriptPath = model.sourcePath,
                            onResult = { model.sourcePath = it.absolutePath },
                        )
                    },
                ) {
                    Text(text = stringResource(id = R.string.text_select))
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MyTextField(
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

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MyTextField(
                    value = model.customOcrModelPath,
                    onValueChange = { model.customOcrModelPath = it },
                    label = { Text(text = stringResource(id = R.string.text_custom_ocr_model_path)) },
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    selectCustomOcrModelPath(
                        context = context,
                        scriptPath = model.sourcePath,
                        onResult = { model.customOcrModelPath = it.absolutePath },
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
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent)
) {
    val focusManager = LocalFocusManager.current
    MyTextField(
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

private fun selectCustomOcrModelPath(context: Context, scriptPath: String, onResult: (File) -> Unit) {
    val initialDir = File(scriptPath).parent
    FileChooserDialogBuilder(context)
        .title(R.string.text_custom_ocr_model_path)
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

        Log.d(BuildActivity.TAG, "name: ->" + item.name)
        Log.d(BuildActivity.TAG, "name: =>" + keyStore.name)
        Log.d(BuildActivity.TAG, "contains: =>" + keyStore.name?.let { item.name!!.contains(it) })

        if (item.name != null && item.name!!.contains(PFiles.getName(keyStore.name))) {
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
                Pref.setKeyStorePassWord(PFiles.getName(keyStore.path), password)
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

