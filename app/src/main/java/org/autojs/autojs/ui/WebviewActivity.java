package org.autojs.autojs.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.stardust.app.OnActivityResultDelegate;
import com.stardust.autojs.execution.ScriptExecution;
import com.stardust.autojs.script.StringScriptSource;
import com.tencent.smtt.export.external.TbsCoreSettings;
import com.tencent.smtt.export.external.interfaces.JsPromptResult;
import com.tencent.smtt.export.external.interfaces.JsResult;
import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.sdk.QbSdk;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.autojs.autojs.R;
import org.autojs.autojs.model.script.Scripts;
import org.autojs.autojs.ui.log.LogActivity_;

import java.io.InputStream;
import java.util.HashMap;

@EActivity(R.layout.activity_webview)
public class WebviewActivity extends BaseActivity implements OnActivityResultDelegate.DelegateHost {

    public static String EXTRA_URL = "https://wht.im";

    private OnActivityResultDelegate.Mediator mMediator = new OnActivityResultDelegate.Mediator();

    @ViewById(R.id.eweb_view)
    com.tencent.smtt.sdk.WebView mEWebView;

    @ViewById(R.id.swipeRefreshLayout)
    SwipeRefreshLayout mSwipeLayout;

    private MenuItem mSwitchMenuItem;
    private MenuItem mLogMenuItem;
    private MenuItem mScriptMenuItem;
    private MenuItem mAddressItem;
    private boolean mDocsSearchItemExpanded;
    private JSInterface mJSInterface = new JSInterface();
    String ua0 = "";
    final String ua1 = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36";
    final String ua2 = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:61.0) Gecko/20100101 Firefox/61.0";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //初始化X5内核
        HashMap map = new HashMap();
        map.put(TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER, true);
        map.put(TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE, true);
        QbSdk.initTbsSettings(map);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @AfterViews
    void setupViews() {
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mEWebView.reload();
                mSwipeLayout.setRefreshing(false);
            }
        });
        mEWebView.addJavascriptInterface(mJSInterface, "_a");
        WebSettings webSettings = mEWebView.getSettings();
        webSettings.setAllowFileAccess(true); //设置可以访问文件
        webSettings.setDefaultTextEncodingName("utf-8");//设置编码格式
        webSettings.setLoadsImagesAutomatically(true); //支持自动加载图片
        webSettings.setUseWideViewPort(true); //将图片调整到适合webview的大小
        webSettings.setLoadWithOverviewMode(true); // 缩放至屏幕的大小
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN); //自适应屏幕
        webSettings.setSupportZoom(true); //支持缩放，默认为true。是下面那个的前提。
        webSettings.setBuiltInZoomControls(true); //设置内置的缩放控件。若为false，则该WebView不可缩放
        webSettings.setDisplayZoomControls(true); //设置原生的缩放控件
        webSettings.setJavaScriptEnabled(true);  //设置支持Javascript交互
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true); //支持通过JS打开新窗口
        webSettings.setDomStorageEnabled(true); // 开启 DOM storage API 功能
        webSettings.setDatabaseEnabled(true);   //开启 database storage API 功能
        webSettings.setAppCacheEnabled(true);//开启 Application Caches 功能
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK); //使用缓存
        webSettings.setPluginsEnabled(true); //支持插件
        webSettings.setPluginState(WebSettings.PluginState.ON_DEMAND);
        mEWebView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setTitle("")
                        .setMessage(message)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm();
                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setTitle("")
                        .setMessage(message)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm();
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.cancel();
                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, final JsPromptResult result) {

                final EditText et = new EditText(view.getContext());
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setTitle("")
                        .setMessage(message)
                        .setView(et)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm(et.getText().toString());
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.cancel();
                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();
                return true;
            }
        });
        mEWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript("javascript:" + readAssetsTxt(getApplicationContext(), "vconsole.min.js") + "if(vConsole==null||vConsole==undefined) var vConsole = new window.VConsole();", null);
            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return shouldOverrideUrlLoading(view, request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://")) {
                    view.loadUrl(url);
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                }
                return true;
            }
        });
        ua0 = mEWebView.getSettings().getUserAgentString();
        if (getIntent().getStringExtra(EXTRA_URL) != null && getIntent().getStringExtra(EXTRA_URL).trim() != "") {
            EXTRA_URL = getIntent().getStringExtra(EXTRA_URL);
        }
        mEWebView.loadUrl(EXTRA_URL);
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mEWebView.canGoBack()) {
                mEWebView.goBack(); //goBack()表示返回WebView的上一页面
            } else {
                finish();
            }
            return true;
        } else return false;
    }

    class JSInterface {
        ScriptExecution execution;

        @android.webkit.JavascriptInterface
        public void runScript(String code, String name) {
            stopScript(execution);
            execution = Scripts.INSTANCE.run(new StringScriptSource(name, code));
        }

        @android.webkit.JavascriptInterface
        public void runScript(String code) {
            stopScript(execution);
            execution = Scripts.INSTANCE.run(new StringScriptSource("", code));
        }

        @android.webkit.JavascriptInterface
        public void stopScript(ScriptExecution execution) {
            if (execution != null) {
                execution.getEngine().forceStop();
            }
        }
    }

    public static String readAssetsTxt(Context context, String fileName) {
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String text = new String(buffer, "utf-8");
            return text;
        } catch (Exception e) {
            Log.e("readAssetsTxt", e.getMessage());
        }
        return "读取错误，请检查文件名";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_webview, menu);
        this.mAddressItem = menu.findItem(R.id.action_webSwitch);
        this.mSwitchMenuItem = menu.findItem(R.id.action_webSwitch);
        this.mLogMenuItem = menu.findItem(R.id.action_webLog);
        this.mScriptMenuItem = menu.findItem(R.id.action_webScript);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_webLog) {
            LogActivity_.intent(this).start();
            return true;
        } else if (item.getItemId() == R.id.action_webScript) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final EditText et = new EditText(this);
            et.setTextSize(12);
            et.setMinLines(4);
            et.setText("_a.runScript(\"toastLog('Hello!')\");");
            builder.setTitle("请输入JS脚本代码")
                    .setView(et)
                    .setPositiveButton("运行", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.dismiss();
                            mEWebView.evaluateJavascript("javascript:" + et.getText().toString(), null);
                        }
                    }).setNegativeButton("取消", null).show();
        } else if (item.getItemId() == R.id.action_webSwitch) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("请选择浏览器UA：");
            final String items[] = {ua0, ua1, ua2};
            builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mEWebView.getSettings().setUserAgentString(items[which]);
                    Toast.makeText(getApplicationContext(), "UA: " + items[which], Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                }
            });
            builder.show();
        } else if (item.getItemId() == R.id.action_webAddress) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final EditText et = new EditText(this);
            et.setText(mEWebView.getUrl());
            builder.setTitle("请输入网址")
                    .setIcon(android.R.drawable.sym_def_app_icon)
                    .setView(et)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            //按下确定键后的事件
                            mEWebView.loadUrl(et.getText().toString());
                            dialog.dismiss();
                        }
                    }).setNegativeButton("取消", null).show();
        }
        return super.onOptionsItemSelected(item);
    }
}
