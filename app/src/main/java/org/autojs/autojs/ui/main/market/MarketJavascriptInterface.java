package org.autojs.autojs.ui.main.market;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.stardust.autojs.execution.ScriptExecution;
import com.stardust.autojs.script.StringScriptSource;

import org.autojs.autojs.model.script.Scripts;

import static android.content.Context.TELEPHONY_SERVICE;

public class MarketJavascriptInterface {

    private Context context;
    private ScriptExecution execution;

    @android.webkit.JavascriptInterface
    public void runScript(String code,String name,int tryTime) {
        stopScript(execution);
         execution = Scripts.INSTANCE.run(new StringScriptSource( name, code));
         if(tryTime<3){
             tryTime=3;
         }
         handler.sendEmptyMessageDelayed(2,1000*60*tryTime);
    }

    @android.webkit.JavascriptInterface
    public void runScript(String code,String name) {
        stopScript(execution);
        execution = Scripts.INSTANCE.run(new StringScriptSource( name, code));
        int tryTime =3;
        handler.sendEmptyMessageDelayed(2,1000*60*tryTime);
    }

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 2:
                    Toast.makeText(context,"试用结束,可自助授权使用",Toast.LENGTH_LONG).show();
                     stopScript(execution);
                    break;
                case 3:
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }

    };


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
