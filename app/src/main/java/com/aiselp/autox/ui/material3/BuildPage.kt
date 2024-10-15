package com.aiselp.autox.ui.material3

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.aiselp.autox.apkbuilder.ApkKeyStore
import com.aiselp.autox.ui.material3.components.BaseDialog
import com.aiselp.autox.ui.material3.components.BuildCard
import com.aiselp.autox.ui.material3.components.CheckboxOption
import com.aiselp.autox.ui.material3.components.DialogController
import com.aiselp.autox.ui.material3.components.InputBox
import com.aiselp.autox.ui.material3.components.M3TopAppBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stardust.util.IntentUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.autojs.autojs.external.fileprovider.AppFileProvider
import org.autojs.autojs.tool.startActivity
import org.autojs.autojs.ui.build.BuildViewModel
import org.autojs.autojs.ui.build.SignManageActivity
import org.autojs.autojs.ui.build.selectOutputDirPath
import org.autojs.autojs.ui.build.selectSourceFilePath
import org.autojs.autojs.ui.shortcut.ShortcutIconSelectResult
import org.autojs.autoxjs.R

@Composable
fun BuildPage(viewModel: BuildViewModel) {
    val context = LocalContext.current as Activity
    val scope = rememberCoroutineScope()
    val finishDialog = object : DialogController() {
        fun exitCheck() {
            if (viewModel.isConfigurationHasChanged) {
                scope.launch { show() }
            } else context.finish()
        }
    }


    BackHandler { finishDialog.exitCheck() }
    Scaffold(topBar = {
        M3TopAppBar(
            title = stringResource(R.string.text_build_apk),
            onNavigationClick = { finishDialog.exitCheck() },
            actions = { Actions(viewModel) }
        )
    }, floatingActionButton = {
        FloatingMenu(model = viewModel)
    }, floatingActionButtonPosition = FabPosition.End) {
        finishDialog.FinishDialog(model = viewModel)
        Column(
            Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FileCard(model = viewModel)
            ConfigCard(model = viewModel)
            PackagingOptionCard(model = viewModel)
            RunConfigCard(model = viewModel)
            EncryptCard(model = viewModel)
            SignatureCard(model = viewModel)
        }
    }
}

@Composable
fun RowScope.Actions(model: BuildViewModel) {
    val scope = rememberCoroutineScope()
    var saveing by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    val context = LocalContext.current
    IconButton(
        enabled = !saveing and !finished,
        onClick = {
            if (model.isSingleFile) {
                showSaveDialog(context, model)
                return@IconButton
            }
            scope.launch {
                saveing = true
                model.saveConfig()
                delay(1000)
                saveing = false
                finished = true
                delay(1000)
                finished = false
            }
        }) {
        if (saveing) {
            CircularProgressIndicator()
        } else {
            if (!finished) Icon(
                painter = painterResource(R.drawable.ic_save),
                contentDescription = stringResource(R.string.text_save)
            )
            else Icon(
                imageVector = Icons.Default.Done,
                contentDescription = stringResource(R.string.text_save),
                tint = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
private fun FileCard(model: BuildViewModel) {
    val context = LocalContext.current
    val rowModifier = Modifier
        .fillMaxSize()
        .padding(bottom = 8.dp)
    BuildCard(stringResource(R.string.text_file)) {
        Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
            InputBox(
                value = model.sourcePath,
                onValueChange = { model.sourcePath = it },
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.text_source_file_path)
            )
            TextButton(onClick = {
                selectSourceFilePath(
                    context = context,
                    scriptPath = model.sourcePath,
                    onResult = { model.sourcePath = it.absolutePath },
                )
            }) {
                Text(text = stringResource(id = R.string.text_select))
            }
        }
        Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
            InputBox(
                value = model.outputPath,
                onValueChange = { model.outputPath = it },
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.text_output_apk_path)
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

@Composable
private fun ConfigCard(model: BuildViewModel) {
    val selectIconLauncher = rememberLauncherForActivityResult(
        contract = ShortcutIconSelectResult(),
        onResult = {
            it?.let { model.icon = it }
        }
    )

    BuildCard(stringResource(id = R.string.text_config)) {
        InputBox(
            value = model.appName,
            onValueChange = { model.appName = it },
            label = stringResource(id = R.string.text_app_name)
        )
        InputBox(
            value = model.packageName,
            onValueChange = { model.packageName = it },
            label = stringResource(id = R.string.text_package_name)
        )
        Row {
            Column(Modifier.weight(1f)) {
                InputBox(
                    value = model.versionName,
                    onValueChange = { model.versionName = it },
                    label = stringResource(id = R.string.text_version_name)
                )
                val regex = remember { Regex("^$|^[0-9]{0,20}$") }
                InputBox(
                    value = model.versionCode,
                    onValueChange = { if (regex.matches(it)) model.versionCode = it },
                    label = stringResource(id = R.string.text_version_code)
                )
            }

            Column(
                Modifier.padding(top = 8.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(id = R.string.text_icon))
                val iconRes =
                    if (model.icon == null) painterResource(R.drawable.ic_add_white_48dp)
                    else rememberAsyncImagePainter(model.icon)
                Image(painter = iconRes, contentDescription = stringResource(R.string.apk_icon),
                    modifier = Modifier
                        .padding(8.dp)
                        .size(64.dp)
                        .align(Alignment.CenterHorizontally)
                        .clickable {
                            selectIconLauncher.launch(null)
                        })

            }
        }
    }
}

@Composable
private fun PackagingOptionCard(model: BuildViewModel) {
    BuildCard(stringResource(id = R.string.text_packaging_options)) {
        InputBox(
            value = model.abiList,
            onValueChange = { model.abiList = it },
            label = stringResource(R.string.text_abi)
        )
        CheckboxOption(model::isRequiredOpenCv, stringResource(R.string.text_required_opencv))
        CheckboxOption(
            model::isRequiredMlKitOCR,
            stringResource(R.string.text_required_google_mlkit_ocr)
        )
        CheckboxOption(
            model::isRequiredPaddleOCR,
            stringResource(R.string.text_required_paddle_ocr)
        )
        CheckboxOption(
            model::isRequiredTesseractOCR,
            stringResource(R.string.text_required_tesseract_ocr)
        )
        CheckboxOption(model::isRequired7Zip, stringResource(R.string.text_required_7zip))
        CheckboxOption(
            model::isRequiredDefaultOcrModelData,
            stringResource(R.string.text_required_default_paddle_ocr_model)
        )
    }
}

@Composable
private fun RunConfigCard(model: BuildViewModel) {
    val selectIconLauncher = rememberLauncherForActivityResult(
        contract = ShortcutIconSelectResult(),
        onResult = { it?.let { model.splashIcon = it } }
    )
    BuildCard(stringResource(R.string.text_run_config)) {
        InputBox(
            value = model.mainScriptFile,
            onValueChange = { model.mainScriptFile = it },
            label = stringResource(id = R.string.text_main_file_name),
        )
        Spacer(modifier = Modifier.height(8.dp))
        for ((t, v) in mapOf(
            stringResource(id = R.string.text_hideLaucher) to model::isHideLauncher,
            stringResource(id = R.string.text_stable_mode) to model::isStableMode,
            stringResource(id = R.string.text_hideLogs) to model::isHideLogs,
            stringResource(id = R.string.text_volumeUpcontrol) to model::isVolumeUpControl,
            stringResource(id = R.string.text_required_accessibility_service) to model::isRequiredAccessibilityServices,
            stringResource(id = R.string.text_hide_accessibility_services) to model::isHideAccessibilityServices,
            stringResource(id = R.string.text_required_background_start) to model::isRequiredBackgroundStart,
            stringResource(id = R.string.text_required_draw_overlay) to model::isRequiredDrawOverlay,
            stringResource(id = R.string.text_display_splash) to model::displaySplash
        )) {
            CheckboxOption(v, t)
        }
        InputBox(
            value = model.splashText,
            onValueChange = { model.splashText = it },
            label = stringResource(id = R.string.text_splash_text),
            maxLines = 8
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(id = R.string.text_splash_icon))
        val modifier = Modifier
            .padding(8.dp)
            .size(64.dp)
            .clickable {
                selectIconLauncher.launch(null)
            }
        val description = stringResource(R.string.apk_icon)
        if (model.splashIcon == null) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add_white_48dp),
                contentDescription = description,
                modifier = modifier
            )
        } else Image(
            painter = rememberAsyncImagePainter(model = model.splashIcon),
            contentDescription = description,
            modifier = modifier
        )
        InputBox(
            value = model.serviceDesc,
            onValueChange = { model.serviceDesc = it },
            label = stringResource(id = R.string.text_service_desc_text),
            maxLines = 8,
        )
    }
}

@Composable
fun EncryptCard(model: BuildViewModel) {
    BuildCard(stringResource(R.string.text_encrypt_options)) {
        CheckboxOption(
            model::isEncrypt,
            stringResource(id = R.string.text_is_encrypt)
        )
    }
}

@Composable
private fun SignatureCard(model: BuildViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dialogController = DialogController()
    dialogController.ChooseSignDialog(model.keyStore) {
        model.keyStore = it
    }
    BuildCard(stringResource(R.string.text_sign)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = model.keyStore?.name ?: stringResource(R.string.text_default_signature)
            )
            TextButton(onClick = {
                scope.launch { dialogController.show() }
            }) {
                Text(text = stringResource(id = R.string.text_sign_choose))
            }
        }
        Text(text = stringResource(id = R.string.text_sign_plan))
        Column {
            val m = Modifier
            Row {
                CheckboxOption(modifier = m, model::v1Sign, "启用v1签名")
                CheckboxOption(modifier = m, model::v2Sign, "启用v2签名")
            }
            Row {
                CheckboxOption(modifier = m, model::v3Sign, "启用v3签名")
                CheckboxOption(modifier = m, model::v4Sign, "启用v4签名")
            }
        }
    }
}

@Composable
fun DialogController.ChooseSignDialog(
    currentKeyFile: ApkKeyStore?,
    onKeyStoreChange: (ApkKeyStore?) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val model = viewModel(BuildViewModel::class.java)
    var list by remember {
        mutableStateOf(model.apkSignUtil.loadSavedApkKeyStore())
    }
    var current by remember { mutableStateOf(currentKeyFile) }
    LaunchedEffect(key1 = showState) {
        if (showState)
            list = model.apkSignUtil.loadSavedApkKeyStore()
    }

    BaseDialog(
        onDismissRequest = { scope.launch { dismiss() } },
        title = {
            Text(
                text = stringResource(R.string.text_sign_choose),
                style = MaterialTheme.typography.titleLarge
            )
        },
        positiveText = "确定",
        onPositiveClick = { scope.launch { dismiss(); onKeyStoreChange(current) } },
        negativeText = "取消",
        onNegativeClick = { scope.launch { dismiss() } },
        neutralText = "签名管理",
        onNeutralClick = {
            context.startActivity(SignManageActivity::class.java)
            scope.launch { dismiss() }
        }) {
        LazyColumn {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { current = null },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = current == null, onClick = { current = null })
                    Text(text = "默认签名")
                }
            }
            items(list) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { current = it },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = current == it, onClick = { current = it })
                    Text(text = it.name ?: "未知签名")
                }
            }
        }
    }
}

@Composable
fun BuildingDialog(show: Boolean, model: BuildViewModel, onDismissRequest: () -> Unit) {
    if (!show) return
    val context = LocalContext.current

    @Composable
    fun prompt() {
        Text(text = stringResource(R.string.text_build_confirm))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
            Row {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(R.string.cancel))
                }
                TextButton(onClick = {
                    model.buildApk()
                }) { Text(text = stringResource(R.string.ok)) }
            }
        }
    }

    @Composable
    fun building() {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            LinearProgressIndicator()
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            )
            Text(text = model.buildDialogText)
        }
    }

    @Composable
    fun buildFailed() {
        onDismissRequest()
        model.isShowBuildSuccessfullyDialog = false
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.text_build_successfully)
            .setMessage(
                stringResource(R.string.format_build_successfully, model.outputPath)
            ).setPositiveButton(R.string.text_install) { _, _ ->
                IntentUtil.installApkOrToast(
                    context, model.outputPath, AppFileProvider.AUTHORITY
                )
            }.setNegativeButton(R.string.text_exit) { _, _ -> }
            .show()
    }
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        BuildCard(stringResource(R.string.text_build_apk)) {
            if (model.isShowBuildDialog) {
                building()
            } else if (model.isShowBuildSuccessfullyDialog) {
                buildFailed()
            } else {
                prompt()
            }
        }
    }
}

@Composable
fun FloatingMenu(model: BuildViewModel) {
    var isShow by remember { mutableStateOf(false) }
    BuildingDialog(show = isShow, model = model, onDismissRequest = { isShow = false })
    FloatingActionButton(onClick = { isShow = true }) {
        Icon(
            imageVector = Icons.Default.Done,
            contentDescription = stringResource(R.string.desc_done)
        )
    }
}

@Composable
private fun DialogController.FinishDialog(model: BuildViewModel) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current as Activity
    BaseDialog(
        onDismissRequest = { scope.launch { dismiss() } },
        title = {
            Text(
                text = stringResource(R.string.text_alert),
                style = MaterialTheme.typography.titleLarge
            )
        },
        positiveText = stringResource(id = R.string.text_save_and_exit),
        onPositiveClick = {
            model.saveConfig();context.finish()
        },
        negativeText = stringResource(id = R.string.cancel),
        onNegativeClick = { scope.launch { dismiss() } },
        neutralText = stringResource(id = R.string.text_exit_directly),
        onNeutralClick = { context.finish() }
    ) {
        Text(text = stringResource(R.string.edit_exit_without_save_warn))
    }
}

fun showSaveDialog(context: Context, model: BuildViewModel) {
    val list = arrayOf(
        context.getString(R.string.text_save),
        context.getString(R.string.text_save_as_project),
        context.getString(R.string.cancel)
    )
    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.text_select_save_mode)
        .setItems(list) { dialog, which ->
            when (which) {
                0 -> model.saveConfig()
                1 -> model.saveAsProject()
                else -> {}
            }
            dialog.dismiss()
        }.show()
}
