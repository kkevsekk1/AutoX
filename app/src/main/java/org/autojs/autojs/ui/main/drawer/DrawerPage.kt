package org.autojs.autojs.ui.main.drawer

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.preference.PreferenceManager
import coil.compose.rememberAsyncImagePainter
import com.stardust.app.GlobalAppContext
import com.stardust.app.isOpPermissionGranted
import com.stardust.app.permission.DrawOverlaysPermission
import com.stardust.app.permission.DrawOverlaysPermission.launchCanDrawOverlaysSettings
import com.stardust.app.permission.PermissionsSettingsUtil
import com.stardust.enhancedfloaty.FloatyService
import com.stardust.notification.NotificationListenerService
import com.stardust.toast
import com.stardust.util.IntentUtil
import com.stardust.view.accessibility.AccessibilityService
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import io.noties.markwon.Markwon
import kotlinx.coroutines.*
import org.autojs.autojs.Pref
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.devplugin.DevPlugin
import org.autojs.autojs.external.foreground.ForegroundService
import org.autojs.autojs.tool.AccessibilityServiceTool
import org.autojs.autojs.tool.WifiTool
import org.autojs.autojs.ui.build.MyTextField
import org.autojs.autojs.ui.compose.theme.AutoXJsTheme
import org.autojs.autojs.ui.compose.widget.MyAlertDialog1
import org.autojs.autojs.ui.compose.widget.MyIcon
import org.autojs.autojs.ui.compose.widget.MySwitch
import org.autojs.autojs.ui.floating.FloatyWindowManger
import org.autojs.autojs.ui.settings.SettingsActivity
import org.autojs.autoxjs.R
import org.joda.time.DateTimeZone
import org.joda.time.Instant
import org.autojs.autojs.core.network.socket.State

private const val TAG = "DrawerPage"
private const val URL_DEV_PLUGIN = "https://github.com/kkevsekk1/Auto.js-VSCode-Extension"
private const val PROJECT_ADDRESS = "https://github.com/kkevsekk1/AutoX"
private const val DOWNLOAD_ADDRESS = "https://github.com/kkevsekk1/AutoX/releases"
private const val FEEDBACK_ADDRESS = "https://github.com/kkevsekk1/AutoX/issues"

@Composable
fun DrawerPage() {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
    ) {
        Spacer(
            modifier = Modifier
                .windowInsetsTopHeight(WindowInsets.statusBars)
        )
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
            Text(text = stringResource(id = R.string.text_service))
            AccessibilityServiceSwitch()
            StableModeSwitch()
            NotificationUsageRightSwitch()
            ForegroundServiceSwitch()
            UsageStatsPermissionSwitch()

            Text(text = stringResource(id = R.string.text_script_record))
            FloatingWindowSwitch()
            VolumeDownControlSwitch()
            AutoBackupSwitch()

            Text(text = stringResource(id = R.string.text_others))
            ConnectComputerSwitch()
            USBDebugSwitch()

            SwitchTimedTaskScheduler()
            ProjectAddress()
            DownloadLink()
            Feedback()
            CheckForUpdate()
            AppDetailsSettings()
        }
        Spacer(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(AutoXJsTheme.colors.divider)
        )
        BottomButtons()
        Spacer(
            modifier = Modifier
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
        )
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
private fun Feedback() {
    val context = LocalContext.current
    TextButton(onClick = { IntentUtil.browse(context, FEEDBACK_ADDRESS) }) {
        Text(text = stringResource(R.string.text_issue_report))
    }
}

@Composable
private fun DownloadLink() {
    val context = LocalContext.current
    TextButton(onClick = { IntentUtil.browse(context, DOWNLOAD_ADDRESS) }) {
        Text(text = stringResource(R.string.text_app_download_link))
    }
}

@Composable
private fun ProjectAddress() {
    val context = LocalContext.current
    TextButton(onClick = { IntentUtil.browse(context, PROJECT_ADDRESS) }) {
        Text(text = stringResource(R.string.text_project_link))
    }
}

@Composable
private fun CheckForUpdate(model: DrawerViewModel = viewModel()) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var enabled by rememberSaveable { mutableStateOf(true) }

    TextButton(
        enabled = enabled,
        onClick = {
            enabled = false
            model.checkUpdate(
                onUpdate = {
                    showDialog = true
                },
                onComplete = {
                    enabled = true
                },
            )
        }
    ) {
        Text(text = stringResource(R.string.text_check_for_updates))
    }
    if (showDialog && model.githubReleaseInfo != null) {
        AlertDialog(onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = stringResource(
                        id = R.string.text_new_version2,
                        model.githubReleaseInfo!!.name
                    )
                )
            },
            text = {
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
                    Text(text = stringResource(id = R.string.text_release_date, date))
                    AndroidView(
                        factory = { context ->
                            TextView(context).apply {
                                val content =
                                    model.githubReleaseInfo!!.body.trim().replace("\r\n", "\n")
                                        .replace("\n", "  \n")
                                val markdwon = Markwon.builder(context).build()
                                markdwon.setMarkdown(this, content)
                            }
                        },
                        update = {

                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                }) {
                    Text(text = stringResource(id = R.string.text_cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    model.downloadApk()
                }) {
                    Text(text = stringResource(id = R.string.text_download))
                }
            })
    }
}

@Composable
private fun BottomButtons() {
    val context = LocalContext.current
    var lastBackPressedTime = remember {
        0L
    }
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
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.onBackground)
        ) {
            MyIcon(imageVector = Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(id = R.string.text_setting))
        }
        TextButton(
            modifier = Modifier.weight(1f), onClick = {
                val currentTime = System.currentTimeMillis()
                val interval = currentTime - lastBackPressedTime
                if (interval > 2000) {
                    lastBackPressedTime = currentTime
                    Toast.makeText(
                        context,
                        context.getString(R.string.text_press_again_to_exit),
                        Toast.LENGTH_SHORT
                    ).show()
                } else exitCompletely(context)
            },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.onBackground)
        ) {
            MyIcon(imageVector = Icons.Default.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(id = R.string.text_exit))
        }
    }
}

fun exitCompletely(context: Context) {
    if (context is Activity) context.finish()
    FloatyWindowManger.hideCircularMenu()
    ForegroundService.stop(context)
    context.stopService(Intent(context, FloatyService::class.java))
    AutoJs.getInstance().scriptEngineService.stopAll()
}

@Composable
fun USBDebugSwitch() {
    val context = LocalContext.current
    var enable by remember {
        mutableStateOf(DevPlugin.isUSBDebugServiceActive)
    }
    val scope = rememberCoroutineScope()
    SwitchItem(
        icon = {
            MyIcon(
                painterResource(id = R.drawable.ic_debug),
                contentDescription = null
            )
        },
        text = { Text(text = stringResource(id = R.string.text_open_usb_debug)) },
        checked = enable,
        onCheckedChange = {
            if (it) {
                scope.launch {
                    try {
                        DevPlugin.startUSBDebug()
                        enable = true
                    } catch (e: Exception) {
                        enable = false
                        e.printStackTrace()
                        Toast.makeText(
                            context,
                            context.getString(
                                R.string.text_start_service_failed,
                                e.localizedMessage
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                scope.launch {
                    DevPlugin.stopUSBDebug()
                    enable = false
                }
            }
        }
    )
}

@Composable
private fun ConnectComputerSwitch() {
    val context = LocalContext.current
    var enable by remember {
        mutableStateOf(DevPlugin.isActive)
    }
    var showDialog by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()
    val scanCodeLauncher =
        rememberLauncherForActivityResult(contract = ScanQRCode(), onResult = { result ->
            when (result) {
                is QRResult.QRSuccess -> {
                    val url = result.content.rawValue
                    if (url.matches(Regex("^(ws://|wss://).+$"))) {
                        Pref.saveServerAddress(url)
                        connectServer(url)
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
    LaunchedEffect(key1 = Unit, block = {
        DevPlugin.connectState.collect {
            withContext(Dispatchers.Main) {
                when (it.state) {
                    State.CONNECTED -> enable = true
                    State.DISCONNECTED -> enable = false
                }
            }
        }
    })
    SwitchItem(
        icon = {
            MyIcon(
                painterResource(id = R.drawable.ic_debug),
                null
            )
        },
        text = {
            Text(
                text = stringResource(
                    id = if (!enable) R.string.text_connect_computer
                    else R.string.text_connected_to_computer
                )
            )
        },
        checked = enable,
        onCheckedChange = {
            if (it) {
                showDialog = true
            } else {
                scope.launch { DevPlugin.close() }
            }
        }
    )
    if (showDialog) {
        ConnectComputerDialog(
            onDismissRequest = { showDialog = false },
            onScanCode = { scanCodeLauncher.launch(null) }
        )
    }

}

@Composable
private fun ConnectComputerDialog(
    onDismissRequest: () -> Unit,
    onScanCode: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = { onDismissRequest() }) {
        var host by remember {
            mutableStateOf(Pref.getServerAddressOrDefault(WifiTool.getRouterIp(context)))
        }
        Surface(shape = RoundedCornerShape(4.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(text = stringResource(id = R.string.text_server_address))
                MyTextField(
                    value = host,
                    onValueChange = { host = it },
                    modifier = Modifier.padding(vertical = 16.dp),
                    placeholder = {
                        Text(text = host)
                    }
                )
                Row(Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = {
                            onDismissRequest()
                            IntentUtil.browse(context, URL_DEV_PLUGIN)
                        }
                    ) {
                        Text(text = stringResource(id = R.string.text_help))
                    }
                    TextButton(
                        onClick = {
                            onDismissRequest()
                            onScanCode()
                        }
                    ) {
                        Text(text = stringResource(id = R.string.text_scan_qr))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        onDismissRequest()
                        Pref.saveServerAddress(host)
                        connectServer(getUrl(host))
                    }) {
                        Text(text = stringResource(id = R.string.ok))
                    }
                }
            }
        }

    }
}

@OptIn(DelicateCoroutinesApi::class)
@SuppressLint("HardwareIds")
private fun connectServer(
    url: String,
) {
    GlobalScope.launch { DevPlugin.connect(url) }
}

private fun getUrl(host: String): String {
    var url1 = host
    if (!url1.matches(Regex("^(ws|wss)://.*"))) {
        url1 = "ws://${url1}"
    }
    if (!url1.matches(Regex("^.+://.+?:.+$"))) {
        url1 += ":${DevPlugin.SERVER_PORT}"
    }
    return url1
}

@Composable
private fun AutoBackupSwitch() {
    val context = LocalContext.current
    var enable by remember {
        val default = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(context.getString(R.string.key_auto_backup), false)
        mutableStateOf(default)
    }
    SwitchItem(
        icon = {
            MyIcon(
                painterResource(id = R.drawable.ic_backup),
                null
            )
        },
        text = { Text(text = stringResource(id = R.string.text_auto_backup)) },
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

@Composable
private fun VolumeDownControlSwitch() {
    val context = LocalContext.current
    var enable by remember {
        val default = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(context.getString(R.string.key_use_volume_control_record), false)
        mutableStateOf(default)
    }
    SwitchItem(
        icon = {
            MyIcon(
                painterResource(id = R.drawable.ic_sound_waves),
                null
            )
        },
        text = { Text(text = stringResource(id = R.string.text_volume_down_control)) },
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
private fun FloatingWindowSwitch() {
    val context = LocalContext.current

    var isFloatingWindowShowing by remember {
        mutableStateOf(FloatyWindowManger.isCircularMenuShowing())
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            if (DrawOverlaysPermission.isCanDrawOverlays(context)) FloatyWindowManger.showCircularMenu()
            isFloatingWindowShowing = FloatyWindowManger.isCircularMenuShowing()
        }
    )
    SwitchItem(
        icon = {
            MyIcon(
                painterResource(id = R.drawable.ic_overlay),
                null
            )
        },
        text = { Text(text = stringResource(id = R.string.text_floating_window)) },
        checked = isFloatingWindowShowing,
        onCheckedChange = {
            if (isFloatingWindowShowing) {
                FloatyWindowManger.hideCircularMenu()
            } else {
                if (DrawOverlaysPermission.isCanDrawOverlays(context)) FloatyWindowManger.showCircularMenu()
                else launcher.launchCanDrawOverlaysSettings(context.packageName)
            }
            isFloatingWindowShowing = FloatyWindowManger.isCircularMenuShowing()
            Pref.setFloatingMenuShown(isFloatingWindowShowing)
        }
    )
}

@Composable
private fun UsageStatsPermissionSwitch() {
    val context = LocalContext.current
    var enabled by remember {
        mutableStateOf(context.isOpPermissionGranted(AppOpsManager.OPSTR_GET_USAGE_STATS))
    }
    var showDialog by remember {
        mutableStateOf(false)
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            enabled = context.isOpPermissionGranted(AppOpsManager.OPSTR_GET_USAGE_STATS)
        }
    )
    SwitchItem(
        icon = {
            MyIcon(
                Icons.Default.Settings,
                null
            )
        },
        text = { Text(text = stringResource(id = R.string.text_usage_stats_permission)) },
        checked = enabled,
        onCheckedChange = {
            showDialog = true
        }
    )
    if (showDialog) {
        AlertDialog(
            title = { Text(text = stringResource(id = R.string.text_usage_stats_permission)) },
            onDismissRequest = { showDialog = false },
            text = {
                Text(
                    text = stringResource(
                        R.string.description_usage_stats_permission
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    launcher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }) {
                    Text(text = stringResource(id = R.string.text_go_to_setting))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(text = stringResource(id = R.string.text_cancel))
                }
            },
        )
    }
}

@Composable
private fun ForegroundServiceSwitch() {
    val context = LocalContext.current
    var isOpenForegroundServices by remember {
        val default = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(context.getString(R.string.key_foreground_servie), false)
        mutableStateOf(default)
    }
    SwitchItem(
        icon = {
            MyIcon(
                Icons.Default.Settings,
                contentDescription = null
            )
        },
        text = { Text(text = stringResource(id = R.string.text_foreground_service)) },
        checked = isOpenForegroundServices,
        onCheckedChange = {
            if (it) {
                ForegroundService.start(context)
            } else {
                ForegroundService.stop(context)
            }
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(context.getString(R.string.key_foreground_servie), it)
                .apply()
            isOpenForegroundServices = it
        }
    )
}

@Composable
private fun NotificationUsageRightSwitch() {
    LocalContext.current
    var isNotificationListenerEnable by remember {
        mutableStateOf(notificationListenerEnable())
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            isNotificationListenerEnable = notificationListenerEnable()
        }
    )
    SwitchItem(
        icon = {
            MyIcon(
                Icons.Default.Notifications,
                null
            )
        },
        text = { Text(text = stringResource(id = R.string.text_notification_permission)) },
        checked = isNotificationListenerEnable,
        onCheckedChange = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                launcher.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            } else isNotificationListenerEnable = it
        }
    )
}

private fun notificationListenerEnable(): Boolean = NotificationListenerService.instance != null


@Composable
private fun StableModeSwitch() {
    val context = LocalContext.current
    var showDialog by remember {
        mutableStateOf(false)
    }
    var isStableMode by remember {
        val default = Pref.isStableModeEnabled()
        mutableStateOf(default)
    }
    SwitchItem(
        icon = {
            MyIcon(
                painter = painterResource(id = R.drawable.ic_triangle),
                contentDescription = null
            )
        },
        text = { Text(text = stringResource(id = R.string.text_stable_mode)) },
        checked = isStableMode,
        onCheckedChange = {
            if (it) showDialog = true
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(context.getString(R.string.key_stable_mode), it)
                .apply()
            isStableMode = it
        }
    )
    if (showDialog) {
        AlertDialog(
            title = { Text(text = stringResource(id = R.string.text_stable_mode)) },
            onDismissRequest = { showDialog = false },
            text = {
                Text(
                    text = stringResource(
                        R.string.description_stable_mode
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(text = stringResource(id = R.string.ok))
                }
            }
        )
    }
}

@Composable
private fun AccessibilityServiceSwitch() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDialog by remember {
        mutableStateOf(false)
    }
    var isAccessibilityServiceEnabled by remember {
        mutableStateOf(AccessibilityServiceTool.isAccessibilityServiceEnabled(context))
    }
    val accessibilitySettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (AccessibilityServiceTool.isAccessibilityServiceEnabled(context)) {
                isAccessibilityServiceEnabled = true
            } else {
                isAccessibilityServiceEnabled = false
                Toast.makeText(
                    context,
                    R.string.text_accessibility_service_is_not_enable,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    var editor by remember { mutableStateOf(Pref.getEditor()) }
    SwitchItem(
        icon = {
            MyIcon(
                Icons.Default.Edit,
                contentDescription = null,
            )
        },
        text = { Text(text = "启用新编辑器") },
        checked = editor,
        onCheckedChange = { isChecked ->
            editor = isChecked
            Pref.setEditor(isChecked)
        }
    )
    SwitchItem(
        icon = {
            MyIcon(
                Icons.Default.Settings,
                contentDescription = null,
            )
        },
        text = { Text(text = stringResource(id = R.string.text_accessibility_service)) },
        checked = isAccessibilityServiceEnabled,
        onCheckedChange = {
            if (!isAccessibilityServiceEnabled) {
                if (Pref.shouldEnableAccessibilityServiceByRoot()) {
                    scope.launch {
                        val enabled = withContext(Dispatchers.IO) {
                            AccessibilityServiceTool.enableAccessibilityServiceByRootAndWaitFor(2000)
                        }
                        if (enabled) isAccessibilityServiceEnabled = true
                        else showDialog = true
                    }
                } else showDialog = true
            } else {
                isAccessibilityServiceEnabled = !AccessibilityService.disable()
            }
        }
    )

    if (showDialog) {
        AlertDialog(
            title = { Text(text = stringResource(id = R.string.text_need_to_enable_accessibility_service)) },
            onDismissRequest = { showDialog = false },
            text = {
                Text(
                    text = stringResource(
                        R.string.explain_accessibility_permission2,
                        GlobalAppContext.appName
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    accessibilitySettingsLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }) {
                    Text(text = stringResource(id = R.string.text_go_to_open))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(text = stringResource(id = R.string.text_cancel))
                }
            },
        )
    }
}

@Composable
fun SwitchItem(
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.padding(8.dp)) {
            icon()
        }
        Box(modifier = Modifier.weight(1f)) {
            text()
        }
        MySwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SwitchTimedTaskScheduler() {
    var isShowDialog by rememberSaveable {
        mutableStateOf(false)
    }
    TextButton(onClick = { isShowDialog = true }) {
        Text(text = stringResource(id = R.string.text_switch_timed_task_scheduler))
    }
    if (isShowDialog) {
        TimedTaskSchedulerDialog(onDismissRequest = { isShowDialog = false })
    }
}

@Composable
fun TimedTaskSchedulerDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var selected by rememberSaveable {
        mutableStateOf(Pref.getTaskManager())
    }
    MyAlertDialog1(
        onDismissRequest = onDismissRequest,
        onConfirmClick = {
            onDismissRequest()
            Pref.setTaskManager(selected)
            toast(context, R.string.text_set_successfully)
        },
        title = { Text(text = stringResource(id = R.string.text_switch_timed_task_scheduler)) },
        text = {
            Column {
                Spacer(modifier = Modifier.size(16.dp))
                Column() {
                    for (i in 0 until 3) {
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
                                    1 -> stringResource(id = R.string.text_android_job)
                                    else -> stringResource(id = R.string.text_alarm_manager)
                                }
                            )
                        }
                    }
                }
            }

        }
    )
}