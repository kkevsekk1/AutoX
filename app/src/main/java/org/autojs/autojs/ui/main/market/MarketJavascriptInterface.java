package org.autojs.autojs.ui.main.market;

import com.stardust.autojs.script.StringScriptSource;

import org.autojs.autojs.model.script.Scripts;

public class MarketJavascriptInterface {


    @android.webkit.JavascriptInterface
    public void runScript(String code,String params,String name) {
        System.out.println(code);
        System.out.println(params);
        Scripts.INSTANCE.run(new StringScriptSource("[remote]" + name, code));
    }
}
