package com.stardust.auojs.inrt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
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
import com.stardust.app.GlobalAppContext;
import com.stardust.auojs.inrt.autojs.AccessibilityServiceTool;
import com.stardust.auojs.inrt.launch.GlobalProjectLauncher;
import com.stardust.auojs.inrt.pluginclient.DevPluginService;
import com.stardust.auojs.inrt.util.UpdateUtil;

import java.security.SecureRandom;
import java.util.Random;

import io.reactivex.android.schedulers.AndroidSchedulers;


/** GPL-2.0
 * Copyright 2006 AARON  <CHENGSHULUN@LIVE.COM>
 */


public class LoginActivity extends AppCompatActivity {
    TextView infoTv;

    Button registBtn;
    Button settingBtn;
    Button reconnectBtn;
    Button runFeatureBtn;
    private String code;
    private String imei;
    private String status;
    boolean isOpen = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupViews();
        checkconnect();
        checkVersion();

    }


    @Override
    protected void onStart() {
        super.onStart();
        checkAccessibilityService();
    }

    private void checkconnect() {
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
    }


    private void setupViews() {

        setContentView(R.layout.activity_market_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(GlobalAppContext.getAppName());
        setSupportActionBar(toolbar);

        infoTv = findViewById(R.id.info);

        registBtn = findViewById(R.id.regist);
        registBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                regist();
            }
        });
        reconnectBtn = findViewById(R.id.reconnect);
        reconnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reconnect();
            }
        });
        runFeatureBtn = findViewById(R.id.ykapp);
        runFeatureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startFeature();
            }
        });
        settingBtn = findViewById(R.id.setting);
        settingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAccessibilityService();
                if (isOpen) {
                    showMessage("无障碍服务--已开启");
                }  AccessibilityServiceTool.INSTANCE.goToAccessibilitySetting();
            }
        });
        init();
        setTvInfo();
    }
    private void startFeature(){
        Intent intent = new Intent(this, FeatureActivity.class);
        startActivity(intent);
    }


    private void checkAccessibilityService() {
        this.isOpen = AccessibilityServiceTool.INSTANCE.isAccessibilityServiceEnabled(this);
        String msg = "";
        if (isOpen) {
            msg = "无障碍服务--已开启";
        } else {
            msg = "无障碍服务--未打开";
        }
        settingBtn.setText(msg);
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

    private void reconnect() {
        String host = Pref.getHost("");
        String code = Pref.getCode("");
        if (TextUtils.isEmpty(host) || TextUtils.isEmpty(code)) {
            showMessage("链接地址或用户码未设置");
        }
        DevPluginService.getInstance().sayHelloToServer(Integer.parseInt(code));//重启
        showMessage("重连中...");
    }


    private void regist() {
        if (!checkPermission()) {
            GlobalAppContext.toast("权限不够,请打开必要的权限");
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
                    String newCode = userCodeInput.getText().toString().trim();
                    Pref.setCode(newCode);
                    String newHost = serverAddrInput.getText().toString().trim();
                    Pref.setHost(newHost);
                    String params = "iemi=" + imei + "&usercode=" + newCode;
                    if (!host.equals(newHost)) {
                        DevPluginService.getInstance().connectToServer(newHost, params).subscribe();
                    } else {
                        DevPluginService.getInstance().sayHelloToServer(Integer.parseInt(newCode));//重启
                    }
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
