package com.stardust.auojs.inrt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.afollestad.materialdialogs.MaterialDialog;
import com.linsh.utilseverywhere.ContextUtils;
import com.stardust.app.GlobalAppContext;
import com.stardust.auojs.inrt.autojs.AccessibilityServiceTool;
import com.stardust.auojs.inrt.launch.GlobalProjectLauncher;
import com.stardust.auojs.inrt.pluginclient.DevPluginService;
import com.stardust.auojs.inrt.util.UpdateUtil;

import java.security.SecureRandom;
import java.util.Random;

import io.reactivex.android.schedulers.AndroidSchedulers;

public class LoginActivity extends AppCompatActivity {
    TextView infoTv;

    Button settingBtn;
    private String code;
    private String imei;
    private String status;
    Button registBtn;
    boolean isOpen = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupViews();
        DevPluginService.getInstance().connectionState()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> {
                    if (state.getException() != null) {
                        status = state.getException().getMessage();
                        Pref.setStatus(status);
                        init();
                        setTvInfo();
                    }
                });
        checkVersion();
    }


    private void setupViews() {

        setContentView(R.layout.activity_main2);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(GlobalAppContext.getAppName());
        setSupportActionBar(toolbar);
        infoTv = findViewById(R.id.info);
        settingBtn = findViewById(R.id.regist);
        settingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                regist();
            }
        });
        registBtn = findViewById(R.id.setting);
        registBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isOpen) {
                    registBtn.setText("去开启无障碍服务");
                    AccessibilityServiceTool.INSTANCE.goToAccessibilitySetting();
                } else {
                    showMessage("已开启无障碍服务");
                    registBtn.setText("功能已开启");
                }
            }
        });
        init();
        setTvInfo();
    }

    private void runScript() {
        GlobalProjectLauncher.INSTANCE.launch(LoginActivity.this);
    }

    private void setTvInfo() {
        String format = "code:%s \r\n IMEI:%s  \r\n 状态:%s ";
        String info = String.format(format, code, imei, status);
        infoTv.setText(info);
    }


    private void init() {
        code = Pref.getCode("");
        imei = Pref.getImei("");
        if (TextUtils.isEmpty(imei)) {
            imei = getIMEI();
            Pref.setImei(imei);
        }
        status = Pref.getStatus("未知");
    }

    private void showMessage(CharSequence text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private void regist() {
        if (!checkPermission()) {
            GlobalAppContext.toast("你没有授权,请授权后重试");
            return;
        }
        String host = Pref.getHost("");
        String code = Pref.getCode("");
        MaterialDialog tmpDialog = new MaterialDialog.Builder(this).title("连接到服务器")
                .customView(R.layout.dialog_regist_user_code, false)
                .positiveText("确定")
                .onPositive((dialog, which) -> {
                    View customeView = dialog.getCustomView();
                    EditText userCodeInput = (EditText) customeView.findViewById(R.id.user_code);
                    EditText serverAddrInput = (EditText) customeView.findViewById(R.id.server_addr);
                    String code1 = userCodeInput.getText().toString().trim();
                    Pref.setCode(code1);
                    String host1 = serverAddrInput.getText().toString().trim();
                    Pref.setHost(host);
                    String params = "iemi=" + imei + "&usercode=" + code1;
                    DevPluginService.getInstance().connectToServer(host1, params)
                            .subscribe();
                    showMessage("正在连接...");
                }).show();
        View customeView = tmpDialog.getCustomView();
        EditText userCodeInput = (EditText) customeView.findViewById(R.id.user_code);
        EditText serverAddrInput = (EditText) customeView.findViewById(R.id.server_addr);
        userCodeInput.setText(code);
        serverAddrInput.setText(host);
    }


    @SuppressLint("MissingPermission")
    private String getIMEI() {
        if (checkPermission()) {
            return "错误数据";
        }
        String deviceId = null;
        TelephonyManager tm = (TelephonyManager) this.getApplication().getSystemService(TELEPHONY_SERVICE);
        deviceId = tm.getDeviceId();
        if (TextUtils.isEmpty(deviceId)) {
            deviceId = Settings.System.getString(
                    getApplication().getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        if (TextUtils.isEmpty(deviceId)) {
            deviceId = Pref.getImei("");
        }
        if (TextUtils.isEmpty(deviceId)) {
            deviceId = getGUID();
            Pref.setImei(deviceId);
        }
        return deviceId;
    }

    public static String getGUID() {
        StringBuilder uid = new StringBuilder();
        //产生16位的强随机数
        Random rd = new SecureRandom();
        for (int i = 0; i < 16; i++) {
            //产生0-2的3位随机数
            int type = rd.nextInt(3);
            switch (type) {
                case 0:
                    //0-9的随机数
                    uid.append(rd.nextInt(10));
                    break;
                case 1:
                    //ASCII在65-90之间为大写,获取大写随机
                    uid.append((char) (rd.nextInt(25) + 65));
                    break;
                case 2:
                    //ASCII在97-122之间为小写，获取小写随机
                    uid.append((char) (rd.nextInt(25) + 97));
                    break;
                default:
                    break;
            }
        }
        return uid.toString();
    }

    private boolean checkPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 123);
            return false;
        }
        return true;
    }


    private void checkVersion() {
        UpdateUtil updateUtil = new UpdateUtil(this);
        updateUtil.checkUpdate();
    }


}
