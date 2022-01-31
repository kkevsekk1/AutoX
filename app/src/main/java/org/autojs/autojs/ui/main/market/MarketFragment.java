package org.autojs.autojs.ui.main.market;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.tencent.smtt.sdk.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.stardust.app.GlobalAppContext;
import com.stardust.util.BackPressedHandler;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import org.autojs.autojs.R;
import org.autojs.autojs.ui.main.QueryEvent;
import org.autojs.autojs.ui.main.ViewPagerFragment;
import org.autojs.autojs.ui.widget.EWebView;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * Created by Stardust on 2017/8/22.
 */
@EFragment(R.layout.fragment_market)
public class MarketFragment extends ViewPagerFragment implements BackPressedHandler {

    @ViewById(R.id.eweb_view_market)
    EWebView mEWebView;
    WebView mWebView;

    MarketJavascriptInterface javascriptInterface;

    private String mIndexUrl = "http://mk.autoxjs.com/pages/controlMine/controlMine";
    private String mPreviousQuery;


    public MarketFragment() {
        super(ROTATION_GONE);
        setArguments(new Bundle());
        javascriptInterface = new MarketJavascriptInterface(GlobalAppContext.get());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @AfterViews
    void setUpViews() {
        mWebView = mEWebView.getWebView();
        mEWebView.getSwipeRefreshLayout().setOnRefreshListener(() -> {
            loadUrl();
            mWebView.addJavascriptInterface(javascriptInterface, "android");
        });
        Bundle savedWebViewState = getArguments().getBundle("savedWebViewState");
        if (savedWebViewState != null) {
            mWebView.restoreState(savedWebViewState);
        } else {
            loadUrl();
        }
        mWebView.addJavascriptInterface(javascriptInterface, "android");
    }

    private void loadUrl() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setTitle("请选择共享脚本来源：");
        final String items[] = {"AutoX官方市场（年久失修……）", "Gitee脚本搜索（推荐）", "Github脚本搜索（推荐）"};
        builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String item = items[which];
                if (which == 0) {
                    mIndexUrl = "http://mk.autoxjs.com/pages/controlMine/controlMine";
                } else if (which == 1) {
                    mIndexUrl = "https://search.gitee.com/?skin=rec&type=repository&q=autojs";
                } else {
                    mIndexUrl = "https://github.com/search?q=autojs";
                }
                mWebView.loadUrl(mIndexUrl);
                dialog.dismiss();
            }
        });
        builder.show();
    }

    @Override
    public void onPause() {
        super.onPause();
        Bundle savedWebViewState = new Bundle();
        mWebView.saveState(savedWebViewState);
        getArguments().putBundle("savedWebViewState", savedWebViewState);
    }

    @Override
    public boolean onBackPressed(Activity activity) {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return false;
    }

    @Override
    protected void onFabClick(FloatingActionButton fab) {

    }

    @Subscribe
    public void onQuerySummit(QueryEvent event) {
        if (!isShown()) {
            return;
        }
        if (event == QueryEvent.CLEAR) {
            mWebView.clearMatches();
            mPreviousQuery = null;
            return;
        }
        if (event.isFindForward()) {
            mWebView.findNext(false);
            return;
        }
        if (event.getQuery().equals(mPreviousQuery)) {
            mWebView.findNext(true);
            return;
        }
        mWebView.findAllAsync(event.getQuery());
        mPreviousQuery = event.getQuery();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
