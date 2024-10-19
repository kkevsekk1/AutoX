package com.aiselp.autox.ui.material3

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AppOpsManager
import android.content.Intent
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.preference.PreferenceManager
import coil.compose.rememberAsyncImagePainter
import com.aiselp.autox.ui.material3.components.AlertDialog
import com.aiselp.autox.ui.material3.components.BaseDialog
import com.aiselp.autox.ui.material3.components.DialogController
import com.aiselp.autox.ui.material3.components.SettingOptionSwitch
import com.aiselp.autox.ui.material3.components.Watch
import com.stardust.app.GlobalAppContext
import com.stardust.app.isOpPermissionGranted
import com.stardust.app.permission.DrawOverlaysPermission
import com.stardust.app.permission.DrawOverlaysPermission.launchCanDrawOverlaysSettings
import com.stardust.app.permission.PermissionsSettingsUtil
import com.stardust.autojs.IndependentScriptService
import com.stardust.autojs.core.pref.PrefKey
import com.stardust.notification.NotificationListenerService
import com.stardust.toast
import com.stardust.util.IntentUtil
import com.stardust.view.accessibility.AccessibilityService
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import io.noties.markwon.Markwon
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.autojs.autojs.Pref
import org.autojs.autojs.devplugin.DevPlugin
import org.autojs.autojs.tool.AccessibilityServiceTool
import org.autojs.autojs.tool.WifiTool
import org.autojs.autojs.ui.floating.FloatyWindowManger
import org.autojs.autojs.ui.main.drawer.DrawerViewModel
import org.autojs.autojs.ui.settings.SettingsActivity
import org.autojs.autoxjs.R
import org.joda.time.DateTimeZone
import org.joda.time.Instant

private const val TAG = "DrawerPage"
private const val URL_DEV_PLUGIN = "https://github.com/kkevsekk1/Auto.js-VSCode-Extension"
private const val PROJECT_ADDRESS = "https://github.com/kkevsekk1/AutoX"
private const val DOWNLOAD_ADDRESS = "https://github.com/kkevsekk1/AutoX/releases"
private const val FEEDBACK_ADDRESS = "https://github.com/kkevsekk1/AutoX/issues"


@Composable
fun DrawerPage() {
    ModalDrawerSheet(Modifier.width(500.dp)) {
        Column(Modifier.fillMaxSize()) {
            val textStyle = MaterialTheme.typography.titleMedium
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Image(
                        painter = rememberAsyncImagePainter(R.drawable.autojs_logo1),
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = stringResource(R.string.text_service), style = textStyle)
                AccessibilityServiceSwitch()
                StableModeSwitch()
                NotificationUsageRightSwitch()
                ForegroundServiceSwitch()
                UsageStatsPermissionSwitch()

                Text(text = stringResource(id = R.string.text_script_record), style = textStyle)
                FloatingWindowSwitch()
                VolumeDownControlSwitch()
                AutoBackupSwitch()

                Text(text = stringResource(id = R.string.text_others), style = textStyle)
                ConnectComputerSwitch()
                USBDebugSwitch()

                SwitchTimedTaskScheduler()
                ProjectAddress()
                DownloadLink()
                Feedback()
                CheckForUpdate()
                AppDetailsSettings()
            }
            HorizontalDivider()
            BottomButtons()
        }
    }
}

@Composable
private fun AccessibilityServiceSwitch() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dialog = remember { DialogController() }
    val isAccessibilityServiceEnabled = remember {
        mutableStateOf(AccessibilityServiceTool.isAccessibilityServiceEnabled(context))
    }
    val accessibilitySettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            isAccessibilityServiceEnabled.value =
                AccessibilityServiceTool.isAccessibilityServiceEnabled(context)
            if (!isAccessibilityServiceEnabled.value) {
                isAccessibilityServiceEnabled.value = false
                toast(context, R.string.text_accessibility_service_is_not_enable)
            }
        }
    val editor = remember { mutableStateOf(Pref.getEditor()) }
    Watch(editor) {
        Pref.setEditor(editor.value)
    }
    SettingOptionSwitch(
        icon = Icons.Default.Edit,
        title = "启用新编辑器",
        value = editor,
    )
    SettingOptionSwitch(
        icon = {
            Icon(imageVector = Icons.Default.Settings, contentDescription = null)
        },
        title = stringResource(id = R.string.text_accessibility_service),
        checked = isAccessibilityServiceEnabled.value,
        onCheckedChange = {
            if (!isAccessibilityServiceEnabled.value) {
                if (Pref.shouldEnableAccessibilityServiceByRoot()) {
                    scope.launch {
                        val enabled = withContext(Dispatchers.IO) {
                            AccessibilityServiceTool.enableAccessibilityServiceByRootAndWaitFor(2000)
                        }
                        if (enabled) isAccessibilityServiceEnabled.value = true
                        else dialog.show()
                    }
                } else scope.launch { dialog.show() }
            } else {
                isAccessibilityServiceEnabled.value = !AccessibilityService.disable()
            }
        }
    )
    dialog.BaseDialog(onDismissRequest = { scope.launch { dialog.dismiss() } },
        title = { Text(text = stringResource(R.string.text_need_to_enable_accessibility_service)) },
        positiveText = stringResource(id = R.string.text_go_to_open),
        onPositiveClick = {
            scope.launch { dialog.dismiss() }
            accessibilitySettingsLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        },
        negativeText = stringResource(id = R.string.text_cancel),
        onNegativeClick = { scope.launch { dialog.dismiss() } }
    ) {
        Text(
            text = stringResource(
                R.string.explain_accessibility_permission2,
                GlobalAppContext.appName
            )
        )
    }
}

@Composable
private fun StableModeSwitch() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dialog = remember { DialogController() }

    var isStableMode by remember {
        val default = Pref.isStableModeEnabled()
        mutableStateOf(default)
    }
    SettingOptionSwitch(
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_triangle),
                contentDescription = null
            )
        },
        title = stringResource(id = R.string.text_stable_mode),
        checked = isStableMode,
        onCheckedChange = {
            if (it) scope.launch { dialog.show() }
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(context.getString(R.string.key_stable_mode), it)
                .apply()
            isStableMode = it
        }
    )
    dialog.AlertDialog(
        title = stringResource(id = R.string.text_stable_mode),
        content = stringResource(R.string.description_stable_mode),
        positiveText = stringResource(id = R.string.ok)
    )
}

@Composable
private fun NotificationUsageRightSwitch() {
    fun notificationListenerEnable(): Boolean = NotificationListenerService.instance != null

    val isNotificationListenerEnable = remember {
        mutableStateOf(notificationListenerEnable())
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            isNotificationListenerEnable.value = notificationListenerEnable()
        }
    )
    Watch(isNotificationListenerEnable) {
        if (isNotificationListenerEnable.value)
            launcher.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    SettingOptionSwitch(
        icon = Icons.Default.Notifications,
        title = stringResource(id = R.string.text_notification_permission),
        value = isNotificationListenerEnable,
    )
}

@Composable
private fun ForegroundServiceSwitch() {
    val context = LocalContext.current
    val isOpenForegroundServices = remember {
        val default = com.stardust.autojs.core.pref.Pref.getDefault(context)
            .getBoolean(PrefKey.KEY_FOREGROUND_SERVIE, false)
        mutableStateOf(default)
    }
    Watch(isOpenForegroundServices) {
        Pref.def().edit(true) {
            putBoolean(PrefKey.KEY_FOREGROUND_SERVIE, isOpenForegroundServices.value)
        }
        if (isOpenForegroundServices.value) {
            IndependentScriptService.startForeground(context)
        } else IndependentScriptService.stopForeground(context)
    }
    SettingOptionSwitch(
        icon = Icons.Default.Settings,
        title = stringResource(id = R.string.text_foreground_service),
        value = isOpenForegroundServices,
    )
}

@Composable
private fun UsageStatsPermissionSwitch() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var enabled by remember {
        mutableStateOf(context.isOpPermissionGranted(AppOpsManager.OPSTR_GET_USAGE_STATS))
    }
    val dialog = remember { DialogController() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            enabled = context.isOpPermissionGranted(AppOpsManager.OPSTR_GET_USAGE_STATS)
        }
    )
    SettingOptionSwitch(
        icon = Icons.Default.Settings,
        title = stringResource(id = R.string.text_usage_stats_permission),
        checked = enabled,
        onCheckedChange = { scope.launch { if (it) dialog.show() } }
    )
    dialog.AlertDialog(
        title = stringResource(id = R.string.text_usage_stats_permission),
        content = stringResource(R.string.description_usage_stats_permission),
        positiveText = stringResource(id = R.string.text_go_to_setting),
        onPositiveClick = {
            scope.launch { dialog.dismiss() }
            launcher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        },
        negativeText = stringResource(id = R.string.text_cancel)
    )
}

@Composable
private fun FloatingWindowSwitch() {
    val context = LocalContext.current

    var isFloatingWindowShowing by remember {
        mutableStateOf(Pref.isFloatingMenuShown())
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            if (DrawOverlaysPermission.isCanDrawOverlays(context)) {
                FloatyWindowManger.showCircularMenu()
                isFloatingWindowShowing = true
            } else isFloatingWindowShowing = false
        }
    )
    SettingOptionSwitch(
        icon = {
            Icon(painterResource(id = R.drawable.ic_overlay), null)
        },
        title = stringResource(id = R.string.text_floating_window),
        checked = isFloatingWindowShowing,
        onCheckedChange = {
            if (isFloatingWindowShowing) {
                FloatyWindowManger.hideCircularMenu()
                isFloatingWindowShowing = false
                Pref.setFloatingMenuShown(false)
            } else {
                if (DrawOverlaysPermission.isCanDrawOverlays(context)) {
                    FloatyWindowManger.showCircularMenu()
                    isFloatingWindowShowing = true
                    Pref.setFloatingMenuShown(true)
                } else launcher.launchCanDrawOverlaysSettings(context.packageName)
            }
        }
    )
}


@Composable
private fun VolumeDownControlSwitch() {
    val context = LocalContext.current
    var enable by remember {
        val default = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(context.getString(R.string.key_use_volume_control_record), false)
        mutableStateOf(default)
    }
    SettingOptionSwitch(
        icon = {
            Icon(painterResource(id = R.drawable.ic_sound_waves), null)
        },
        title = stringResource(id = R.string.text_volume_down_control),
        checked = enable,
        onCheckedChange = {
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(context.getString(R.string.key_use_volume_control_record), it)
                .apply()
            enable = it
        }
    )
}

@Composable
private fun AutoBackupSwitch() {
    val context = LocalContext.current
    var enable by remember {
        val default = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(context.getString(R.string.key_auto_backup), false)
        mutableStateOf(default)
    }
    SettingOptionSwitch(
        icon = {
            Icon(
                painterResource(id = R.drawable.ic_backup),
                null
            )
        },
        title = stringResource(id = R.string.text_auto_backup),
        checked = enable,
        onCheckedChange = {
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(context.getString(R.string.key_auto_backup), it)
                .apply()
            enable = it
        }
    )
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
private fun ConnectComputerSwitch() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var enable by remember { mutableStateOf(DevPlugin.isActive) }


    val scanCodeLauncher =
        rememberLauncherForActivityResult(contract = ScanQRCode(), onResult = { result ->
            when (result) {
                is QRResult.QRSuccess -> {
                    val url = result.content.rawValue
                    if (url.matches(Regex("^(ws://|wss://).+$"))) {
                        Pref.saveServerAddress(url)
                        GlobalScope.launch { DevPlugin.connect(url) }
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.text_unsupported_qr_code),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                QRResult.QRUserCanceled -> {}
                QRResult.QRMissingPermission -> {}
                is QRResult.QRError -> {}
            }
        })
    val dialog = object : DialogController() {
        override fun onNeutralClick() {
            showState = false
            scanCodeLauncher.launch(null)
        }
    }
    dialog.ConnectComputerDialog()
    LaunchedEffect(Unit) {
        DevPlugin.connectState.collect {
            withContext(Dispatchers.Main) {
                when (it.state) {
                    DevPlugin.State.CONNECTED -> enable = true
                    DevPlugin.State.DISCONNECTED -> enable = false
                }
            }
        }
    }
    SettingOptionSwitch(
        icon = {
            Icon(painterResource(id = R.drawable.ic_debug), null)
        },
        title = stringResource(
            id = if (!enable) R.string.text_connect_computer
            else R.string.text_connected_to_computer
        ),
        checked = enable,
        onCheckedChange = {
            scope.launch {
                if (it) {
                    dialog.show()
                } else DevPlugin.close()
            }
        }
    )

}

@Composable
private fun DialogController.ConnectComputerDialog() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var host by remember {
        mutableStateOf(Pref.getServerAddressOrDefault(WifiTool.getRouterIp(context)))
    }
    BaseDialog(
        onDismissRequest = { scope.launch { dismiss() } },
        title = {
            Text(
                text = stringResource(id = R.string.text_server_address),
                style = MaterialTheme.typography.titleLarge
            )
        },
        positiveText = stringResource(id = R.string.ok),
        onPositiveClick = {
            scope.launch { dismiss() }
            Pref.saveServerAddress(host)
            connectServer(getUrl(host))
        },
        negativeText = stringResource(id = R.string.text_help),
        onNegativeClick = {
            scope.launch { dismiss() }
            IntentUtil.browse(context, URL_DEV_PLUGIN)
        },
        neutralText = stringResource(id = R.string.text_scan_qr),
    ) {
        TextField(value = host, onValueChange = { host = it })
    }
}

@Composable
fun USBDebugSwitch() {
    val context = LocalContext.current
    var enable by remember {
        mutableStateOf(DevPlugin.isUSBDebugServiceActive)
    }
    val scope = rememberCoroutineScope()
    SettingOptionSwitch(
        icon = {
            Icon(
                painterResource(id = R.drawable.ic_debug),
                contentDescription = null
            )
        },
        title = stringResource(id = R.string.text_open_usb_debug),
        checked = enable,
        onCheckedChange = {
            scope.launch {
                if (it) {
                    try {
                        DevPlugin.startUSBDebug()
                        enable = true
                    } catch (e: Exception) {
                        enable = false
                        e.printStackTrace()
                        context.getString(
                            R.string.text_start_service_failed,
                            e.localizedMessage
                        ).toast(context)
                    }
                } else {
                    DevPlugin.stopUSBDebug()
                    enable = false
                }
            }
        }
    )
}

@Composable
fun SwitchTimedTaskScheduler() {
    val dialog = DialogController()
    val scope = rememberCoroutineScope()
    TextButton(onClick = { scope.launch { dialog.show() } }) {
        Text(text = stringResource(id = R.string.text_switch_timed_task_scheduler))
    }
    dialog.TimedTaskSchedulerDialog()
}

@Composable
private fun DialogController.TimedTaskSchedulerDialog() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable {
        mutableIntStateOf(Pref.getTaskManager())
    }

    fun dismissDialog() {
        scope.launch { dismiss() }
    }
    BaseDialog(
        onDismissRequest = { dismissDialog() },
        positiveText = stringResource(R.string.ok),
        onPositiveClick = {
            dismissDialog()
            Pref.setTaskManager(selected)
            toast(context, R.string.text_set_successfully)
        },
        title = {
            Text(
                text = stringResource(id = R.string.text_switch_timed_task_scheduler),
                style = MaterialTheme.typography.titleLarge
            )
        },
    ) {
        Column {
            for (i in 0 until 2) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selected = i }) {
                    RadioButton(selected = selected == i, onClick = { selected = i })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (i) {
                            0 -> stringResource(id = R.string.text_work_manager)
                            else -> stringResource(id = R.string.text_alarm_manager)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectAddress() {
    val context = LocalContext.current
    TextButton(onClick = {
        IntentUtil.browse(context, PROJECT_ADDRESS)
    }) {
        Text(text = stringResource(R.string.text_project_link))
    }
}

@Composable
private fun DownloadLink() {
    val context = LocalContext.current
    TextButton(onClick = {
        IntentUtil.browse(context, DOWNLOAD_ADDRESS)
    }) {
        Text(text = stringResource(R.string.text_app_download_link))
    }
}

@Composable
private fun Feedback() {
    val context = LocalContext.current
    TextButton(onClick = {
        IntentUtil.browse(context, FEEDBACK_ADDRESS)
    }) {
        Text(text = stringResource(R.string.text_issue_report))
    }
}

@Composable
private fun CheckForUpdate(model: DrawerViewModel = viewModel()) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val dialog = DialogController()
    var enabled by rememberSaveable { mutableStateOf(true) }

    TextButton(
        enabled = enabled,
        onClick = {
            enabled = false
            model.checkUpdate(
                onUpdate = {
                    scope.launch { dialog.show() }
                },
                onComplete = { enabled = true },
            )
        }
    ) {
        Text(text = stringResource(R.string.text_check_for_updates))
    }

    dialog.BaseDialog(
        onDismissRequest = { scope.launch { dialog.dismiss() } },
        title = {
            Text(
                text = stringResource(
                    R.string.text_new_version2,
                    model.githubReleaseInfo!!.name
                ),
                style = MaterialTheme.typography.titleMedium
            )
        },
        positiveText = stringResource(id = R.string.text_download),
        onPositiveClick = {
            showDialog = false
            model.downloadApk()
        },
        negativeText = stringResource(id = R.string.cancel),
        onNegativeClick = { showDialog = false },
    ) {
        val date = rememberSaveable {
            Instant.parse(model.githubReleaseInfo!!.createdAt)
                .toDateTime(DateTimeZone.getDefault())
                .toString("yyyy-MM-dd HH:mm:ss")
        }
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                stringResource(id = R.string.text_release_date, date)
            )
            AndroidView(
                factory = { context ->
                    TextView(context).apply {
                        val content =
                            model.githubReleaseInfo!!.body.trim().replace("\r\n", "\n")
                                .replace("\n", "  \n")
                        val markdwon = Markwon.builder(context).build()
                        markdwon.setMarkdown(this, content)
                    }
                }
            )
        }
    }
}

@Composable
private fun AppDetailsSettings() {
    val context = LocalContext.current
    TextButton(onClick = {
        context.startActivity(PermissionsSettingsUtil.getAppDetailSettingIntent(context.packageName))
    }) {
        Text(text = stringResource(R.string.text_app_detail_settings))
    }
}

@Composable
private fun BottomButtons() {
    val context = LocalContext.current
    Row(modifier = Modifier.fillMaxWidth()) {
        TextButton(
            modifier = Modifier.weight(1f),
            onClick = {
                context.startActivity(
                    Intent(
                        context,
                        SettingsActivity::class.java
                    )
                )
            },
        ) {
            Icon(imageVector = Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(id = R.string.text_setting))
        }
        TextButton(
            modifier = Modifier.weight(1f), onClick = {
                context as Activity
                context.finish()
            }
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(id = R.string.text_exit))
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
@SuppressLint("HardwareIds")
fun connectServer(url: String) {
    GlobalScope.launch { DevPlugin.connect(url) }
}

fun getUrl(host: String): String {
    var url1 = host
    if (!url1.matches(Regex("^(ws|wss)://.*"))) {
        url1 = "ws://${url1}"
    }
    if (!url1.matches(Regex("^.+://.+?:.+$"))) {
        url1 += ":${DevPlugin.SERVER_PORT}"
    }
    return url1
}