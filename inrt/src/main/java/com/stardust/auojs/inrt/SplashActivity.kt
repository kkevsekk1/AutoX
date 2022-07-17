package com.stardust.auojs.inrt

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.Gson
import com.stardust.app.GlobalAppContext
import com.stardust.app.permission.BackgroundStartPermission
import com.stardust.app.permission.DrawOverlaysPermission
import com.stardust.app.permission.DrawOverlaysPermission.launchCanDrawOverlaysSettings
import com.stardust.app.permission.Permissions
import com.stardust.app.permission.PermissionsSettingsUtil.launchAppPermissionsSettings
import com.stardust.auojs.inrt.autojs.AccessibilityServiceTool
import com.stardust.auojs.inrt.autojs.AutoJs
import com.stardust.auojs.inrt.launch.GlobalProjectLauncher
import com.stardust.autojs.project.ProjectConfigKt
import com.stardust.util.IntentUtil

/**
 * Created by Stardust on 2018/2/2.
 * Modified by wilinz on 2022/5/23
 */

class SplashActivity : ComponentActivity() {
    var TAG = "SplashActivity"
    var step = 1 //打开悬浮权限，而打开权限，请求权限

    private val accessibilitySettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (AccessibilityServiceTool.isAccessibilityServiceEnabled(this)) {
                permissionsResult[Permissions.ACCESSIBILITY_SERVICES] = true
                Toast.makeText(this, "无障碍服务已开启", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this, "无障碍服务未开启", Toast.LENGTH_SHORT).show()
            }
            checkPermissions()
        }

    private val backgroundStartSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (BackgroundStartPermission.isBackgroundStartAllowed(this)) {
                permissionsResult[Permissions.BACKGROUND_START] = true
            }
            checkPermissions()
        }

    private val drawOverlaysSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (DrawOverlaysPermission.isCanDrawOverlays(this)) {
                permissionsResult[Permissions.DRAW_OVERLAY] = true
            }
            checkPermissions()
        }

    private val projectConfig by lazy {
        ProjectConfigKt.fromAssets(this, ProjectConfigKt.configFileOfDir("project"))!!
    }

    private val permissionsResult = mutableMapOf<String, Boolean>()

    private fun checkPermissions() {
        if (permissionsResult.all { it.value }) {
            runScript()
        } else {
            for (entry in permissionsResult) {
                if (!entry.value) {
                    when (entry.key) {
                        Permissions.ACCESSIBILITY_SERVICES -> {
                            requestAccessibilityService()
                        }
                        Permissions.BACKGROUND_START -> {
                            requestBackgroundStart()
                        }
                        Permissions.DRAW_OVERLAY -> {
                            requestDrawOverlays()
                        }
                    }
                    break
                }
            }
        }
    }


    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        val slug = findViewById<TextView>(R.id.slug)
        slug.typeface = Typeface.createFromAsset(assets, "roboto_medium.ttf")
        Log.d(TAG, "onCreate: ${Gson().toJson(projectConfig)}")
        if (Pref.getHost("d") == "d") { //非第一次运行
            Pref.setHost("112.74.161.35")
            projectConfig.launchConfig.let {
                Pref.setHideLogs(it.isHideLogs)
                Pref.setStableMode(it.isStableMode)
                Pref.setStopAllScriptsWhenVolumeUp(it.isVolumeUpControl)
                Pref.setDisplaySplash(it.displaySplash)
            }

        }
        if (!checkStoragePermissions()) {
            GlobalAppContext.toast("请开启权限后，再运行!")
        }
        projectConfig.launchConfig.let {
            it.permissions.forEach {
                when (it) {
                    Permissions.ACCESSIBILITY_SERVICES -> {
                        permissionsResult[it] =
                            AccessibilityServiceTool.isAccessibilityServiceEnabled(this)
                    }
                    Permissions.BACKGROUND_START -> {
                        permissionsResult[it] =
                            BackgroundStartPermission.isBackgroundStartAllowed(this)
                    }
                    Permissions.DRAW_OVERLAY -> {
                        permissionsResult[it] = DrawOverlaysPermission.isCanDrawOverlays(this)
                    }
                }
            }
        }
        Log.d(TAG, "onCreate: ${permissionsResult}")
        checkPermissions()
    }

    private fun requestDrawOverlays() {
        val dialog =
            MaterialDialog.Builder(this).title("需要悬浮窗权限").content("需要悬浮窗权限")//内容
                .positiveText("去打开") //肯定按键
                .negativeText("取消")
                .onPositive { dialog, which ->
                    dialog.dismiss()
                    drawOverlaysSettingsLauncher.launchCanDrawOverlaysSettings(packageName)
                }.onNegative { dialog, which ->
                    finish()
                }
                .canceledOnTouchOutside(false)
                .build();
        dialog.show();
    }

    private fun manageWriteSettings() {
        val dialog = MaterialDialog.Builder(this).title("继续进入权限设置")
            .content("请打开所有权限!\r\n 请打开所有权限 \r\n 请打开所有权限")//内容
            .positiveText("确定") //肯定按键
            .onPositive { _, _ ->
                IntentUtil.goToAppDetailSettings(this);
            }.canceledOnTouchOutside(false)
            .build();
        dialog.show();
    }

    private fun requestBackgroundStart() {
        val dialog = MaterialDialog.Builder(this)
            .title("需要后台打开界面权限")
            .content("需要后台打开界面权限才能运行，请前往设置打开")
            .positiveText("去打开") //肯定按键
            .negativeText("取消")
            .onPositive { dialog, which ->
                dialog.dismiss()
                backgroundStartSettingsLauncher.launchAppPermissionsSettings(packageName)
            }
            .onNegative { dialog, which ->
                finish()
            }
            .canceledOnTouchOutside(false)
            .build();
        dialog.show();
    }

    private fun requestAccessibilityService() {
        val dialog = MaterialDialog.Builder(this)
            .title(R.string.text_need_to_enable_accessibility_service)
            .content(R.string.explain_accessibility_permission, GlobalAppContext.appName)
            .positiveText("去打开") //肯定按键
            .negativeText("取消")
            .onPositive { dialog, which ->
                dialog.dismiss()
                accessibilitySettingsLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .onNegative { dialog, which ->
                finish()
            }
            .canceledOnTouchOutside(false)
            .build();
        dialog.show();
    }

    private fun runScript() {
        Thread {
            try {
                GlobalProjectLauncher.launch(this)
                this.finish();
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@SplashActivity, e.message, Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@SplashActivity, LogActivity::class.java))
                    AutoJs.instance.globalConsole.printAllStackTrace(e)
                }
            }
        }.start()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        @NonNull permissions: Array<String>,
        @NonNull grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionsResult: " + requestCode);
    }

    private fun checkPermission(vararg permissions: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val requestPermissions = getRequestPermissions(permissions)
            if (requestPermissions.isNotEmpty()) {
                requestPermissions(requestPermissions, PERMISSION_REQUEST_CODE)
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    private fun checkStoragePermissions(): Boolean {
        return checkPermission(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun getRequestPermissions(permissions: Array<out String>): Array<String> {
        val list = ArrayList<String>()
        for (permission in permissions) {
            if (checkSelfPermission(permission) == PERMISSION_DENIED) {
                list.add(permission)
            }
        }
        return list.toTypedArray()
    }

    companion object {

        private const val PERMISSION_REQUEST_CODE = 11186
        private const val INIT_TIMEOUT: Long = 2500
    }

    override fun onResume() {
        super.onResume()
    }

}

