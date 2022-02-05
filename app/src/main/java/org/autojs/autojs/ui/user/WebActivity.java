package org.autojs.autojs.ui.user;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.stardust.app.FragmentPagerAdapterBuilder;
import com.stardust.app.OnActivityResultDelegate;
import com.tencent.smtt.export.external.TbsCoreSettings;
import com.tencent.smtt.sdk.QbSdk;

import org.autojs.autojs.R;
import org.autojs.autojs.ui.BaseActivity;
import org.autojs.autojs.ui.WebviewActivity_;
import org.autojs.autojs.ui.doc.DocsFragment_;
import org.autojs.autojs.ui.log.LogActivity_;
import org.autojs.autojs.ui.main.ViewPagerFragment;
import org.autojs.autojs.ui.main.community.CommunityFragment_;
import org.autojs.autojs.ui.main.market.MarketFragment_;
import org.autojs.autojs.ui.main.scripts.MyScriptListFragment_;
import org.autojs.autojs.ui.main.task.TaskManagerFragment_;
import org.autojs.autojs.ui.widget.EWebView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.autojs.autojs.ui.widget.SearchViewItem;

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
