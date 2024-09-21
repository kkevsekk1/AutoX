package org.autojs.autojs.external.tasker;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;

import org.autojs.autojs.external.ScriptIntents;
import org.autojs.autojs.external.open.RunIntentActivity;

import com.twofortyfouram.locale.sdk.client.receiver.AbstractPluginSettingReceiver;

import java.io.File;

/**
 * Created by Stardust on 2017/3/27.
 */

public class FireSettingReceiver extends AbstractPluginSettingReceiver {

    private static final String TAG = "FireSettingReceiver";

    @Override
    protected boolean isBundleValid(@NonNull Bundle bundle) {
        return ScriptIntents.isTaskerBundleValid(bundle);
    }

    @Override
    protected boolean isAsync() {
        return true;
    }

    @Override
    protected void firePluginSetting(@NonNull Context context, @NonNull Bundle bundle) {
        String path = bundle.getString(ScriptIntents.EXTRA_KEY_PATH);
        String script = bundle.getString(ScriptIntents.EXTRA_KEY_PRE_EXECUTE_SCRIPT);
        Intent intent = new Intent(context, RunIntentActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (path != null) {
            intent.setData(Uri.fromFile(new File(path)));
        }
        context.startActivity(intent);
    }

}
