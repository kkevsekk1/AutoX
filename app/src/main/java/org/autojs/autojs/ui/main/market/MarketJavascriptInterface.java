package org.autojs.autojs.ui.main.market;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.stardust.autojs.script.StringScriptSource;

import org.autojs.autojs.model.script.Scripts;

import static android.content.Context.TELEPHONY_SERVICE;

public class MarketJavascriptInterface {

    Context context;

    @android.webkit.JavascriptInterface
    public void runScript(String code,String name) {
        System.out.println(code);
        Scripts.INSTANCE.run(new StringScriptSource( name, code));
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
}
