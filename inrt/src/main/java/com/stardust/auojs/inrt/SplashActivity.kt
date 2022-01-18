package com.stardust.auojs.inrt

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.linsh.utilseverywhere.IntentUtils
import com.stardust.app.GlobalAppContext
import com.stardust.auojs.inrt.autojs.AutoJs
import com.stardust.auojs.inrt.launch.GlobalProjectLauncher
import com.stardust.auojs.inrt.util.UpdateUtil
import com.stardust.autojs.project.ProjectConfig
import com.stardust.util.IntentUtil
import ezy.assist.compat.SettingsCompat
import java.util.*

/**
 * Created by Stardust on 2018/2/2.
 */

class SplashActivity : AppCompatActivity() {
    var TAG = "SplashActivity";
    var step = 1; //打开悬浮权限，而打开权限，请求权限

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        val slug = findViewById<TextView>(R.id.slug)
        slug.typeface = Typeface.createFromAsset(assets, "roboto_medium.ttf")
        if(Pref.getHost("d")=="d"){ //非第一次运行
            Pref.setHost("112.74.161.35")
            val mProjectConfig: ProjectConfig = ProjectConfig.fromAssets(this, ProjectConfig.configFileOfDir("project"))
            Pref.setHideLogs(mProjectConfig.getLaunchConfig().shouldHideLogs())
            Pref.setStableMode(mProjectConfig.getLaunchConfig().isStableMode())
            Pref.setStopAllScriptsWhenVolumeUp(mProjectConfig.getLaunchConfig().isVolumeUpcontrol())
            Pref.setDisplaySplash(mProjectConfig.getLaunchConfig().isDisplaySplash())
        }
        if (!BuildConfig.isMarket) {
            if(Pref.istDisplaySplash()){
                main()
            }else{
                Handler().postDelayed({ this@SplashActivity.main() }, INIT_TIMEOUT)
            }
        }else{
           main()
        }

    }


    private fun main() {
        if(checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE)){
            runScript();
        }else{
            GlobalAppContext.toast("请开启权限后，再运行!")
        }
    }

    private fun manageDrawOverlays() {
        var dialog = MaterialDialog.Builder(this).title("提示").content("请打开所有的权限，\r\n 省电策略选【不限制】")//内容
                .positiveText("确定") //肯定按键
                .onPositive { _, _ ->
                    SettingsCompat.manageDrawOverlays(this);
                }.canceledOnTouchOutside(false)
                .build();
        dialog.show();
    }

    private fun manageWriteSettings() {
        var dialog = MaterialDialog.Builder(this).title("继续进入权限设置").content("请打开所有权限!\r\n 请打开所有权限 \r\n 请打开所有权限")//内容
                .positiveText("确定") //肯定按键
                .onPositive { _, _ ->
                    IntentUtil.goToAppDetailSettings(this);
                }.canceledOnTouchOutside(false)
                .build();
        dialog.show();
    }

    private fun AccessibilitySetting() {
        var dialog = MaterialDialog.Builder(this).title("提示").content("请打开无障碍服务")//内容
                .positiveText("确定") //肯定按键
                .onPositive { dialog, which ->
                    IntentUtils.gotoAccessibilitySetting();
                }.canceledOnTouchOutside(false)
                .build();
        dialog.show();
    }

    private fun runScript() {
        if (BuildConfig.isMarket) {
            var intent: Intent = Intent(this@SplashActivity, LoginActivity::class.java);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent)
            this@SplashActivity.finish();
            return
        }
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

    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray) {
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
        if (BuildConfig.isMarket) {
            if (Pref.isFirstUsing()) { //已经不是第一次了
                if (step == 1) {
                    manageDrawOverlays();
                }
                if (step == 2) {
                    manageWriteSettings();
                }
                if (step == 3) {
                    AccessibilitySetting();
                }
                if (step == 4) {
                    Pref.setNotFirstUsingEnd()
                    main();
                }
                step++;
            }
        }
        super.onResume()
    }

}

