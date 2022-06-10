package org.autojs.autojs.ui.doc;

import android.webkit.WebView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.autojs.autojs.Pref;
import org.autojs.autojs.R;
import org.autojs.autojs.ui.BaseActivity;
import org.autojs.autojs.ui.widget.EWebView;

/**
 * Created by Stardust on 2017/10/24.
 */
@EActivity(R.layout.activity_documentation)
public class DocumentationActivity extends BaseActivity {

    public static final String EXTRA_URL = "url";

    @ViewById(R.id.eweb_view)
    EWebView mEWebView;

    WebView mWebView;
    com.tencent.smtt.sdk.WebView mWebViewTbs;

    @AfterViews
    void setUpViews() {
        setToolbarAsBack(getString(R.string.text_tutorial));
        if (mEWebView.getIsTbs()) {
            mWebViewTbs = mEWebView.getWebViewTbs();
        } else {
            mWebView = mEWebView.getWebView();
        }
        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null) {
            url = Pref.getDocumentationUrl() + "index.html";
        }
        if (mEWebView.getIsTbs()) {
            mWebViewTbs.loadUrl(url);
        } else {
            mWebView.loadUrl(url);
        }
    }

    @Override
    public void onBackPressed() {
        if (mEWebView.getIsTbs()) {
            if (mWebViewTbs.canGoBack()) {
                mWebViewTbs.goBack();
            } else {
                super.onBackPressed();
            }
        } else {
            if (mWebView.canGoBack()) {
                mWebView.goBack();
            } else {
                super.onBackPressed();
            }
        }
    }
}