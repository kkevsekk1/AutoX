package com.stardust.auojs.inrt.pluginclient;

import android.text.TextUtils;
import android.util.Log;

import com.fanjun.keeplive.config.KeepLiveService;
import com.stardust.auojs.inrt.Pref;

public class AutoXKeepLiveService implements KeepLiveService {
    public static String TAG = "inrt.AutoXKeepLiveService";

    @Override
    public void onWorking() {
        Log.d(TAG, "------------------onWorking: ---");
       String code = Pref.getCode("");
       String host = Pref.getHost("");
       String iemi =Pref.getImei("");
       if(!TextUtils.isEmpty(code)){
           Log.d(TAG, "onWorking: "+"链接");
           String params = "iemi="+iemi + "&usercode="+code;
           DevPluginService.getInstance().connectToServer(host, params).subscribe();
           }
    }



    @Override
    public void onStop() {
        Log.d(TAG, "onStop: ----");
    }
}
