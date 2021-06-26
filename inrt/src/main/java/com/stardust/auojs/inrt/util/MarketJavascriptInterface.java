package com.stardust.auojs.inrt.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.stardust.auojs.inrt.Pref;
import com.stardust.auojs.inrt.autojs.AutoJs;
import com.stardust.autojs.execution.ExecutionConfig;
import com.stardust.autojs.execution.ScriptExecution;
import com.stardust.autojs.script.StringScriptSource;


import static android.content.Context.TELEPHONY_SERVICE;

public class MarketJavascriptInterface {

    private Context context;
    private ScriptExecution execution;

    @android.webkit.JavascriptInterface
    public void runScript(String code,String name) {
        stopScript(execution);
        StringScriptSource scriptSource = new StringScriptSource(name,code);
        ExecutionConfig config = new ExecutionConfig();
        config.setWorkingDirectory(Pref.getScriptDirPath());
        execution=   AutoJs.Companion.getInstance().getScriptEngineService().execute(scriptSource, new ExecutionConfig());
    }


    public MarketJavascriptInterface(Context context){
        this.context =context;
    }

    @android.webkit.JavascriptInterface
    public String getUid() {
        return getIMEI();
    }

    @SuppressLint("MissingPermission")
    private String getIMEI() {
        String deviceId = null;
        TelephonyManager tm = (TelephonyManager) this.context.getSystemService(TELEPHONY_SERVICE);
        deviceId = tm.getDeviceId();
        if (TextUtils.isEmpty(deviceId)) {
            deviceId = Settings.System.getString(this.context.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        return deviceId;
    }

    private void stopScript(ScriptExecution execution) {
        if (execution != null) {
            execution.getEngine().forceStop();
        }
    }
}
