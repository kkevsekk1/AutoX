package org.autojs.autojs.ui.user;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stardust.app.OnActivityResultDelegate;
import com.tencent.smtt.export.external.TbsCoreSettings;
import com.tencent.smtt.sdk.QbSdk;

import org.autojs.autojs.R;
import org.autojs.autojs.ui.BaseActivity;
import org.autojs.autojs.ui.widget.EWebView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import java.util.HashMap;

/**
 * Created by Stardust on 2017/10/26.
 */
@EActivity(R.layout.activity_web)
public class WebActivity extends BaseActivity implements OnActivityResultDelegate.DelegateHost {

    public static final String EXTRA_URL = "url";

    private OnActivityResultDelegate.Mediator mMediator = new OnActivityResultDelegate.Mediator();

    @ViewById(R.id.eweb_view)
    EWebView mEWebView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //初始化X5内核
        HashMap map = new HashMap();
        map.put(TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER, true);
        map.put(TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE, true);
        QbSdk.initTbsSettings(map);
    }

    @AfterViews
    void setupViews() {
        setToolbarAsBack(getIntent().getStringExtra(Intent.EXTRA_TITLE));
        mEWebView.getWebView().loadUrl(getIntent().getStringExtra(EXTRA_URL));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mMediator.onActivityResult(requestCode, resultCode, data);
    }

    @NonNull
    @Override
    public OnActivityResultDelegate.Mediator getOnActivityResultDelegateMediator() {
        return mMediator;
    }
}
