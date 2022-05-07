package org.autojs.autojs.build;

import android.content.Context;
import android.widget.Toast;

import org.autojs.autojs.R;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Stardust on 2017/11/29.
 */

public class ApkBuilderPluginHelper {
    private static final String TEMPLATE_APK_PATH = "template.apk";
    public static InputStream openTemplateApk(Context context) {
        try {
            return context.getAssets().open(TEMPLATE_APK_PATH);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.text_template_apk_not_found, Toast.LENGTH_SHORT).show();
        }
        return null;
    }
}


