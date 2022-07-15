package org.autojs.autojs.ui.doc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.stardust.util.BackPressedHandler;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import org.autojs.autojs.Pref;
import org.autojs.autojs.R;
import org.autojs.autojs.tool.SimpleObserver;
import org.autojs.autojs.ui.main.FloatingActionMenu;
import org.autojs.autojs.ui.main.QueryEvent;
import org.autojs.autojs.ui.main.ViewPagerFragment;
import org.autojs.autojs.ui.widget.CallbackBundle;
import org.autojs.autojs.ui.widget.EWebView;
import org.autojs.autojs.ui.widget.WebData;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Created by Stardust on 2017/8/22.
 */
@Deprecated
@EFragment(R.layout.fragment_online_docs)
public class DocsFragment extends ViewPagerFragment implements BackPressedHandler, FloatingActionMenu.OnFloatingActionButtonClickListener {

    public static class LoadUrl {
        public final String url;

        public LoadUrl(String url) {
            this.url = url;
        }

    }

    public static final String ARGUMENT_URL = "url";

    @ViewById(R.id.eweb_view_docs)
    EWebView mEWebView;

    com.tencent.smtt.sdk.WebView mWebViewTbs;
    android.webkit.WebView mWebView;

    private String mPreviousQuery;
    private FloatingActionMenu mFloatingActionMenu;
    static private Dialog mDialog;
    public static String tag = "OpenFileDialog";
    static public String sRoot = Environment.getExternalStorageDirectory().getAbsolutePath();
    static final public String sParent = "..";
    static final public String sFolder = ".";
    static final public String sEmpty = "";
    static final private String sOnErrorMsg = "No rights to access!";
    Gson gson = new Gson();
    WebData mWebData;


    public DocsFragment() {
        super(ROTATION_GONE);
        setArguments(new Bundle());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @AfterViews
    void setUpViews() {
        if (mEWebView.getIsTbs()) {
            mWebViewTbs = mEWebView.getWebViewTbs();
            mEWebView.getSwipeRefreshLayout().setOnRefreshListener(() -> {
                if (TextUtils.equals(mWebViewTbs.getUrl(), mWebData.homepage)) {
                    loadUrl();
                } else {
                    mEWebView.onRefresh();
                }
            });
            Bundle savedWebViewState = getArguments().getBundle("savedWebViewState");
            if (savedWebViewState != null) {
                mWebViewTbs.restoreState(savedWebViewState);
            } else {
                loadUrl();
            }
        } else {
            mWebView = mEWebView.getWebView();
            mEWebView.getSwipeRefreshLayout().setOnRefreshListener(() -> {
                if (TextUtils.equals(mWebView.getUrl(), mWebData.homepage)) {
                    loadUrl();
                } else {
                    mEWebView.onRefresh();
                }
            });
            Bundle savedWebViewState = getArguments().getBundle("savedWebViewState");
            if (savedWebViewState != null) {
                mWebView.restoreState(savedWebViewState);
            } else {
                loadUrl();
            }
        }
    }

    private void loadUrl() {
        if (Pref.getWebData().contains("isTbs")) {
            mWebData = gson.fromJson(Pref.getWebData(), WebData.class);
        } else {
            mWebData = new WebData();
            Pref.setWebData(gson.toJson(mWebData));
        }
        if (mEWebView.getIsTbs()) {
            mWebViewTbs.loadUrl(mWebData.homepage);
        } else {
            mWebView.loadUrl(mWebData.homepage);
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        Bundle savedWebViewState = new Bundle();
        if (mEWebView.getIsTbs()) {
            mWebViewTbs.saveState(savedWebViewState);
        } else {
            mWebView.saveState(savedWebViewState);
        }
        getArguments().putBundle("savedWebViewState", savedWebViewState);
    }

    @Override
    public boolean onBackPressed(Activity activity) {
        if (mEWebView.getIsTbs()) {
            if (mWebViewTbs.canGoBack()) {
                mWebViewTbs.goBack();
                return true;
            }

        } else {
            if (mWebView.canGoBack()) {
                mWebView.goBack();
                return true;
            }

        }
        return false;
    }

    @Override
    protected void onFabClick(FloatingActionButton fab) {
        initFloatingActionMenuIfNeeded(fab);
        int[] fabIcons = {
                R.drawable.ic_home_black_48dp,
                R.drawable.ic_homepage,
                R.drawable.ic_star,
                R.drawable.ic_pc_mode,
                R.drawable.ic_console,
                R.drawable.ic_web,
                R.drawable.ic_code_black_48dp,
                R.drawable.ic_floating_action_menu_open
        };
        String[] fabLabs = {"主页1", "主页2", "收藏", "切换桌面模式", "开关控制台", "网址/搜索", "网页源码", "本地文档"};
        mFloatingActionMenu.buildFabs(fabIcons, fabLabs);
        mFloatingActionMenu.setOnFloatingActionButtonClickListener(this);
        if (mFloatingActionMenu.isExpanded()) {
            mFloatingActionMenu.collapse();
        } else {
            mFloatingActionMenu.expand();
        }
    }

    private void initFloatingActionMenuIfNeeded(final FloatingActionButton fab) {
        if (mFloatingActionMenu != null)
            return;
        mFloatingActionMenu = getActivity().findViewById(R.id.floating_action_menu);
        mFloatingActionMenu.getState()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<Boolean>() {
                    @Override
                    public void onNext(@io.reactivex.annotations.NonNull Boolean expanding) {
                        fab.animate()
                                .rotation(expanding ? 45 : 0)
                                .setDuration(300)
                                .start();
                    }
                });
        mFloatingActionMenu.setOnFloatingActionButtonClickListener(this);
    }


    @Subscribe
    public void onQuerySummit(QueryEvent event) {
        if (!isShown()) {
            return;
        }
        if (mEWebView.getIsTbs()) {
            if (event == QueryEvent.CLEAR) {
                mWebViewTbs.clearMatches();
                mPreviousQuery = null;
                return;
            }
            if (event.isFindForward()) {
                mWebViewTbs.findNext(false);
                return;
            }
            if (event.getQuery().equals(mPreviousQuery)) {
                mWebViewTbs.findNext(true);
                return;
            }
            mWebViewTbs.findAllAsync(event.getQuery());
        } else {
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
        }
        mPreviousQuery = event.getQuery();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onClick(FloatingActionButton button, int pos) {
        if (Pref.getWebData().contains("isTbs")) {
            mWebData = gson.fromJson(Pref.getWebData(), WebData.class);
        } else {
            mWebData = new WebData();
            Pref.setWebData(gson.toJson(mWebData));
        }
        if (mEWebView.getIsTbs()) {
            switch (pos) {
                case 0:
                    mWebViewTbs.loadUrl(mWebData.homepage);
                    break;
                case 1:
                    mWebViewTbs.loadUrl(mWebData.homepage2);
                    break;
                case 2:
                    new MaterialDialog.Builder(requireContext())
                            .title("当前页：" + mWebViewTbs.getTitle() + "（设置当前页面或点击打开书签：）")
                            .positiveText("添加收藏")
                            .negativeText("设为主页1")
                            .neutralText("设为主页2")
                            .items(mWebData.bookmarkLabels)
                            .itemsCallback(new MaterialDialog.ListCallback() {
                                @Override
                                public void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                                    mWebViewTbs.loadUrl(mWebData.bookmarks[which]);
                                    dialog.dismiss();
                                }
                            })
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(MaterialDialog dialog, DialogAction which) {
                                    String[] strList = new String[mWebData.bookmarks.length + 1];
                                    String[] strLabelList = new String[mWebData.bookmarks.length + 1];
                                    int j = 0;
                                    for (int i = 0; i < mWebData.bookmarks.length; i++) {
                                        strList[j] = mWebData.bookmarks[i];
                                        strLabelList[j] = mWebData.bookmarkLabels[i];
                                        j += 1;
                                    }
                                    strList[j] = mWebViewTbs.getOriginalUrl();
                                    strLabelList[j] = mWebViewTbs.getTitle();
                                    mWebData.bookmarks = strList;
                                    mWebData.bookmarkLabels = strLabelList;
                                    Pref.setWebData(gson.toJson(mWebData));
                                    dialog.dismiss();
                                }
                            })
                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(MaterialDialog dialog, DialogAction which) {
                                    mWebData.homepage = mWebViewTbs.getOriginalUrl();
                                    Pref.setWebData(gson.toJson(mWebData));
                                    Toast.makeText(getContext(), "设置为主页1：" + mWebViewTbs.getTitle(), Toast.LENGTH_LONG).show();
                                    dialog.dismiss();
                                }
                            })
                            .onNeutral(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(MaterialDialog dialog, DialogAction which) {
                                    mWebData.homepage2 = mWebViewTbs.getOriginalUrl();
                                    Pref.setWebData(gson.toJson(mWebData));
                                    Toast.makeText(getContext(), "设置为主页2：" + mWebViewTbs.getTitle(), Toast.LENGTH_LONG).show();
                                    dialog.dismiss();
                                }
                            })
                            .show();
                    break;
                case 3:
                    mEWebView.switchRescale();
                    if (mEWebView.getIsRescale()) {
                        mWebViewTbs.getSettings().setLoadWithOverviewMode(true);
                        mWebViewTbs.getSettings().setUserAgentString(mWebData.userAgents[6]);
                    } else {
                        mWebViewTbs.getSettings().setLoadWithOverviewMode(false);
                        mWebViewTbs.getSettings().setUserAgentString(mWebData.userAgents[1]);
                    }
                    mWebViewTbs.reload();
                    break;
                case 4:
                    mEWebView.switchConsole();
                    if (mEWebView.getIsConsole()) {
                        com.tencent.smtt.sdk.WebView.setWebContentsDebuggingEnabled(true);
                    } else {
                        com.tencent.smtt.sdk.WebView.setWebContentsDebuggingEnabled(false);
                    }
                    mWebViewTbs.reload();
                    break;
                case 5:
                    EditText et = new EditText(getContext());
                    new MaterialDialog.Builder(requireContext())
                            .title(mWebViewTbs.getOriginalUrl())
                            .customView(et, false)
                            .positiveText("打开")
                            .negativeText("复制网址")
                            .neutralText("搜索")
                            .onNeutral(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(MaterialDialog dialog, DialogAction which) {
                                    new MaterialDialog.Builder(requireContext())
                                            .title("选择搜索引擎：")
                                            .negativeText("取消")
                                            .items(mWebData.searchEngineLabels)
                                            .itemsCallback(new MaterialDialog.ListCallback() {
                                                @Override
                                                public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                                    mWebViewTbs.loadUrl(mWebData.searchEngines[which] + et.getText().toString());
                                                    dialog.dismiss();
                                                }
                                            })
                                            .show();
                                    dialog.dismiss();
                                }
                            })
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    mWebViewTbs.loadUrl(et.getText().toString());
                                    dialog.dismiss();
                                }
                            })
                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    ClipboardManager mClipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                                    ClipData mClipData = ClipData.newPlainText("Label", mWebViewTbs.getOriginalUrl());
                                    mClipboardManager.setPrimaryClip(mClipData);
                                    Toast.makeText(getContext(), "当前网址已复制到剪贴板！", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                }
                            })
                            .show();
                    break;
                case 6:
                    mWebViewTbs.loadUrl(
                            "file://" + requireContext().getExternalFilesDir(
                                    null
                            ).getPath() + File.separator + "html_source.txt"
                    );
                    break;
                case 7:
                    HashMap<String, Integer> images = new HashMap<String, Integer>();
                    // 下面几句设置各文件类型的图标， 需要你先把图标添加到资源文件夹
                    images.put(sRoot, R.drawable.filedialog_root);    // 根目录图标
                    images.put(sParent, R.drawable.filedialog_folder_up);    //返回上一层的图标
                    images.put(sFolder, R.drawable.filedialog_folder);    //文件夹图标
                    images.put(sEmpty, R.drawable.filedialog_file);
                    mDialog = createDialog(getActivity(), "打开本地文档", new CallbackBundle() {
                                @Override
                                public void callback(Bundle bundle) {
                                    String filepath = bundle.getString("filepath").toLowerCase();
                                    if (filepath.endsWith(".html") || filepath.endsWith(".htm") || filepath.endsWith(
                                            ".xhtml"
                                    ) || filepath.endsWith(
                                            ".xml"
                                    ) || filepath.endsWith(
                                            ".mhtml"
                                    ) || filepath.endsWith(
                                            ".mht"
                                    ) || filepath.endsWith(
                                            ".txt"
                                    ) || filepath.endsWith(
                                            ".js"
                                    ) || filepath.endsWith(
                                            ".log"
                                    )
                                    ) {
                                        mWebViewTbs.loadUrl("file://" + filepath);
                                    } else {
                                        if (mEWebView.getIsTbs()) {
                                            HashMap<String, String> extraParams = new HashMap<String, String>(); //define empty hashmap
                                            extraParams.put("style", "1");
                                            extraParams.put("local", "true");
                                            com.tencent.smtt.sdk.QbSdk.openFileReader(
                                                    getContext(),
                                                    filepath,
                                                    extraParams,
                                                    new com.tencent.smtt.sdk.ValueCallback<String>() {
                                                        @Override
                                                        public void onReceiveValue(String it) {
                                                            Log.i("TAG", "OpenFile Callback: $it");
                                                            if ("openFileReader open in QB" == it
                                                                    || "filepath error" == it
                                                                    || "TbsReaderDialogClosed" == it
                                                                    || "fileReaderClosed" == it
                                                            ) {
                                                                com.tencent.smtt.sdk.QbSdk.closeFileReader(
                                                                        getActivity()
                                                                );
                                                            }
                                                        }
                                                    });
                                        } else {
                                            Toast.makeText(getContext(), "系统Web内核不支持查看该格式文档！", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                }
                            },
                            "html;htm;xhtml;xml;mhtml;mht;doc;docx;ppt;pptx;xls;xlsx;pdf;txt;js;log;epub",
                            images);
                    mDialog.show();
                    break;
                default:
                    new MaterialDialog.Builder(requireContext())
                            .title("请选择书签：")
                            .positiveText("删除(多选)")
                            .negativeText("取消")
                            .items(mWebData.bookmarkLabels)
                            .itemsCallbackMultiChoice(null, new MaterialDialog.ListCallbackMultiChoice() {
                                @Override
                                public boolean onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {
                                    return true;
                                }
                            })
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(MaterialDialog dialog, DialogAction which) {
                                    if (Objects.requireNonNull(dialog.getSelectedIndices()).length >= mWebData.bookmarks.length) {
                                        mWebData.bookmarks = new String[]{};
                                        mWebData.bookmarkLabels = new String[]{};
                                        Pref.setWebData(gson.toJson(mWebData));
                                    } else if (Objects.requireNonNull(dialog.getSelectedIndices()).length > 0) {
                                        String[] strList = new String[mWebData.bookmarks.length - dialog.getSelectedIndices().length];
                                        String[] strLabelList = new String[mWebData.bookmarks.length - dialog.getSelectedIndices().length];
                                        int j = 0;
                                        for (int i = 0; i < mWebData.bookmarks.length; i++) {
                                            boolean flag = true;
                                            for (Integer index : dialog.getSelectedIndices()) {
                                                if (i == index) {
                                                    flag = false;
                                                    break;
                                                }
                                            }
                                            if (flag) {
                                                strList[j] = mWebData.bookmarks[i];
                                                strLabelList[j] = mWebData.bookmarkLabels[i];
                                                j += 1;
                                            }
                                        }
                                        mWebData.bookmarks = strList;
                                        mWebData.bookmarkLabels = strLabelList;
                                        Pref.setWebData(gson.toJson(mWebData));
                                    }
                                    dialog.dismiss();
                                }
                            })
                            .show();
                    break;
            }
        } else {
            switch (pos) {
                case 0:
                    mWebView.loadUrl(mWebData.homepage);
                    break;
                case 1:
                    mWebView.loadUrl(mWebData.homepage2);
                    break;
                case 2:
                    new MaterialDialog.Builder(requireContext())
                            .title("当前页：" + mWebView.getTitle() + "（设置当前页面或点击打开书签：）")
                            .positiveText("添加收藏")
                            .negativeText("设为主页1")
                            .neutralText("设为主页2")
                            .items(mWebData.bookmarkLabels)
                            .itemsCallback(new MaterialDialog.ListCallback() {
                                @Override
                                public void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                                    mWebView.loadUrl(mWebData.bookmarks[which]);
                                    dialog.dismiss();
                                }
                            })
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(MaterialDialog dialog, DialogAction which) {
                                    String[] strList = new String[mWebData.bookmarks.length + 1];
                                    String[] strLabelList = new String[mWebData.bookmarks.length + 1];
                                    int j = 0;
                                    for (int i = 0; i < mWebData.bookmarks.length; i++) {
                                        strList[j] = mWebData.bookmarks[i];
                                        strLabelList[j] = mWebData.bookmarkLabels[i];
                                        j += 1;
                                    }
                                    strList[j] = mWebView.getOriginalUrl();
                                    strLabelList[j] = mWebView.getTitle();
                                    mWebData.bookmarks = strList;
                                    mWebData.bookmarkLabels = strLabelList;
                                    Pref.setWebData(gson.toJson(mWebData));
                                    dialog.dismiss();
                                }
                            })
                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(MaterialDialog dialog, DialogAction which) {
                                    mWebData.homepage = mWebView.getOriginalUrl();
                                    Pref.setWebData(gson.toJson(mWebData));
                                    Toast.makeText(getContext(), "设置为主页1：" + mWebView.getTitle(), Toast.LENGTH_LONG).show();
                                    dialog.dismiss();
                                }
                            })
                            .onNeutral(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(MaterialDialog dialog, DialogAction which) {
                                    mWebData.homepage2 = mWebView.getOriginalUrl();
                                    Pref.setWebData(gson.toJson(mWebData));
                                    Toast.makeText(getContext(), "设置为主页2：" + mWebView.getTitle(), Toast.LENGTH_LONG).show();
                                    dialog.dismiss();
                                }
                            })
                            .show();
                    break;
                case 3:
                    mEWebView.switchRescale();
                    if (mEWebView.getIsRescale()) {
                        mWebView.getSettings().setLoadWithOverviewMode(true);
                        mWebView.getSettings().setUserAgentString(mWebData.userAgents[6]);
                    } else {
                        mWebView.getSettings().setLoadWithOverviewMode(false);
                        mWebView.getSettings().setUserAgentString(mWebData.userAgents[1]);
                    }
                    mWebView.reload();
                    break;
                case 4:
                    mEWebView.switchConsole();
                    if (mEWebView.getIsConsole()) {
                        android.webkit.WebView.setWebContentsDebuggingEnabled(true);
                    } else {
                        android.webkit.WebView.setWebContentsDebuggingEnabled(false);
                    }
                    mWebView.reload();
                    break;
                case 5:
                    EditText et = new EditText(getContext());
                    new MaterialDialog.Builder(requireContext())
                            .title(mWebView.getOriginalUrl())
                            .customView(et, false)
                            .positiveText("打开")
                            .negativeText("复制网址")
                            .neutralText("搜索")
                            .onNeutral(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(MaterialDialog dialog, DialogAction which) {
                                    new MaterialDialog.Builder(requireContext())
                                            .title("选择搜索引擎：")
                                            .negativeText("取消")
                                            .items(mWebData.searchEngineLabels)
                                            .itemsCallback(new MaterialDialog.ListCallback() {
                                                @Override
                                                public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                                    mWebView.loadUrl(mWebData.searchEngines[which] + et.getText().toString());
                                                    dialog.dismiss();
                                                }
                                            })
                                            .show();
                                    dialog.dismiss();
                                }
                            })
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    mWebView.loadUrl(et.getText().toString());
                                    dialog.dismiss();
                                }
                            })
                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    ClipboardManager mClipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                                    ClipData mClipData = ClipData.newPlainText("Label", mWebView.getOriginalUrl());
                                    mClipboardManager.setPrimaryClip(mClipData);
                                    Toast.makeText(getContext(), "当前网址已复制到剪贴板！", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                }
                            })
                            .show();
                    break;
                case 6:
                    mWebView.loadUrl(
                            "file://" + requireContext().getExternalFilesDir(
                                    null
                            ).getPath() + File.separator + "html_source.txt"
                    );
                    break;
                case 7:
                    HashMap<String, Integer> images = new HashMap<String, Integer>();
                    // 下面几句设置各文件类型的图标， 需要你先把图标添加到资源文件夹
                    images.put(sRoot, R.drawable.filedialog_root);    // 根目录图标
                    images.put(sParent, R.drawable.filedialog_folder_up);    //返回上一层的图标
                    images.put(sFolder, R.drawable.filedialog_folder);    //文件夹图标
                    images.put(sEmpty, R.drawable.filedialog_file);
                    mDialog = createDialog(getActivity(), "打开本地文档", new CallbackBundle() {
                                @Override
                                public void callback(Bundle bundle) {
                                    String filepath = bundle.getString("filepath").toLowerCase();
                                    if (filepath.endsWith(".html") || filepath.endsWith(".htm") || filepath.endsWith(
                                            ".xhtml"
                                    ) || filepath.endsWith(
                                            ".xml"
                                    ) || filepath.endsWith(
                                            ".mhtml"
                                    ) || filepath.endsWith(
                                            ".mht"
                                    ) || filepath.endsWith(
                                            ".txt"
                                    ) || filepath.endsWith(
                                            ".js"
                                    ) || filepath.endsWith(
                                            ".log"
                                    )
                                    ) {
                                        mWebView.loadUrl("file://" + filepath);
                                    } else {
                                        if (mEWebView.getIsTbs()) {
                                            HashMap<String, String> extraParams = new HashMap<String, String>(); //define empty hashmap
                                            extraParams.put("style", "1");
                                            extraParams.put("local", "true");
                                            com.tencent.smtt.sdk.QbSdk.openFileReader(
                                                    getContext(),
                                                    filepath,
                                                    extraParams,
                                                    new com.tencent.smtt.sdk.ValueCallback<String>() {
                                                        @Override
                                                        public void onReceiveValue(String it) {
                                                            Log.i("TAG", "OpenFile Callback: $it");
                                                            if ("openFileReader open in QB" == it
                                                                    || "filepath error" == it
                                                                    || "TbsReaderDialogClosed" == it
                                                                    || "fileReaderClosed" == it
                                                            ) {
                                                                com.tencent.smtt.sdk.QbSdk.closeFileReader(
                                                                        getActivity()
                                                                );
                                                            }
                                                        }
                                                    });
                                        } else {
                                            Toast.makeText(getContext(), "系统Web内核不支持查看该格式文档！", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                }
                            },
                            "html;htm;xhtml;xml;mhtml;mht;doc;docx;ppt;pptx;xls;xlsx;pdf;txt;js;log;epub",
                            images);
                    mDialog.show();
                    break;
                default:
                    new MaterialDialog.Builder(requireContext())
                            .title("请选择书签：")
                            .positiveText("删除(多选)")
                            .negativeText("取消")
                            .items(mWebData.bookmarkLabels)
                            .itemsCallbackMultiChoice(null, new MaterialDialog.ListCallbackMultiChoice() {
                                @Override
                                public boolean onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {
                                    return true;
                                }
                            })
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(MaterialDialog dialog, DialogAction which) {
                                    if (Objects.requireNonNull(dialog.getSelectedIndices()).length >= mWebData.bookmarks.length) {
                                        mWebData.bookmarks = new String[]{};
                                        mWebData.bookmarkLabels = new String[]{};
                                        Pref.setWebData(gson.toJson(mWebData));
                                    } else if (Objects.requireNonNull(dialog.getSelectedIndices()).length > 0) {
                                        String[] strList = new String[mWebData.bookmarks.length - dialog.getSelectedIndices().length];
                                        String[] strLabelList = new String[mWebData.bookmarks.length - dialog.getSelectedIndices().length];
                                        int j = 0;
                                        for (int i = 0; i < mWebData.bookmarks.length; i++) {
                                            boolean flag = true;
                                            for (Integer index : dialog.getSelectedIndices()) {
                                                if (i == index) {
                                                    flag = false;
                                                    break;
                                                }
                                            }
                                            if (flag) {
                                                strList[j] = mWebData.bookmarks[i];
                                                strLabelList[j] = mWebData.bookmarkLabels[i];
                                                j += 1;
                                            }
                                        }
                                        mWebData.bookmarks = strList;
                                        mWebData.bookmarkLabels = strLabelList;
                                        Pref.setWebData(gson.toJson(mWebData));
                                    }
                                    dialog.dismiss();
                                }
                            })
                            .show();
                    break;
            }
        }
    }

    public Dialog createDialog(Context context, String title, CallbackBundle callback, String suffix, Map<String, Integer> images, String rootDir) {
        if (!rootDir.isEmpty() && (new File(rootDir)).isDirectory()) {
            sRoot = rootDir;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(new FileSelectView(context, callback, suffix, images));
        mDialog = builder.create();
        mDialog.setTitle(title);
        return mDialog;
    }

    public Dialog createDialog(Context context, String title, CallbackBundle callback, String suffix, Map<String, Integer> images) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(new FileSelectView(context, callback, suffix, images));
        mDialog = builder.create();
        //dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setTitle(title);
        return mDialog;
    }

    static class FileSelectView extends ListView implements AdapterView.OnItemClickListener {
        private CallbackBundle callback = null;
        private String path = sRoot;
        private List<Map<String, Object>> list = null;
        private String suffix = null;

        private Map<String, Integer> imageMap = null;

        public FileSelectView(Context context, CallbackBundle callback, String suffix, Map<String, Integer> images) {
            super(context);
            this.imageMap = images;
            this.suffix = suffix == null ? "" : suffix.toLowerCase();
            this.callback = callback;
            this.setOnItemClickListener(this);
            refreshFileList();
        }

        private String getSuffix(String filename) {
            int dix = filename.lastIndexOf('.');
            if (dix < 0) {
                return "";
            } else {
                return filename.substring(dix);
            }
        }

        private int getImageId(String s) {
            if (imageMap == null) {
                return 0;
            } else if (imageMap.containsKey(s)) {
                return imageMap.get(s);
            } else if (imageMap.containsKey(sEmpty)) {
                return imageMap.get(sEmpty);
            } else {
                return 0;
            }
        }

        private int refreshFileList() {
            // 刷新文件列表
            File[] files = null;
            try {
                files = new File(path).listFiles();
            } catch (Exception e) {
                files = null;
            }
            if (files == null) {
                // 访问出错
                Toast.makeText(getContext(), sOnErrorMsg, Toast.LENGTH_SHORT).show();
                return -1;
            }
            if (list != null) {
                list.clear();
            } else {
                list = new ArrayList<Map<String, Object>>(files.length);
            }

            // 用来先保存文件夹和文件夹的两个列表
            ArrayList<Map<String, Object>> lfolders = new ArrayList<Map<String, Object>>();
            ArrayList<Map<String, Object>> lfiles = new ArrayList<Map<String, Object>>();

            if (!this.path.equals(sRoot)) {
                // 添加根目录 和 上一层目录
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("name", sRoot);
                map.put("path", sRoot);
                map.put("img", getImageId(sRoot));
                list.add(map);

                map = new HashMap<String, Object>();
                map.put("name", sParent);
                map.put("path", path);
                map.put("img", getImageId(sParent));
                list.add(map);
            }

            for (File file : files) {
                if (file.isDirectory() && file.listFiles() != null) {
                    // 添加文件夹
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("name", file.getName());
                    map.put("path", file.getPath());
                    map.put("img", getImageId(sFolder));
                    lfolders.add(map);
                } else if (file.isFile()) {
                    // 添加文件
                    String sf = getSuffix(file.getName()).toLowerCase();
//                    Toast.makeText(getContext(), sf, Toast.LENGTH_SHORT).show();
                    String[] suffixList = suffix.split(";");
                    boolean hasSuffix = false;
                    for (String item : suffixList) {
                        if (sf.contains("." + item)) {
                            hasSuffix = true;
                            break;
                        }
                    }
                    if (suffix == null || suffix.length() == 0 || (sf.length() > 0 && hasSuffix)) {
                        Map<String, Object> map = new HashMap<String, Object>();
                        map.put("name", file.getName());
                        map.put("path", file.getPath());
                        map.put("img", getImageId(suffixList[0]));
                        lfiles.add(map);
                    }
                }
            }
            list.addAll(lfolders); // 先添加文件夹，确保文件夹显示在上面
            list.addAll(lfiles);    //再添加文件
            SimpleAdapter adapter = new SimpleAdapter(getContext(), list, R.layout.filedialogitem, new String[]{"img", "name", "path"}, new int[]{R.id.filedialogitem_img, R.id.filedialogitem_name, R.id.filedialogitem_path});
            this.setAdapter(adapter);
            return files.length;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            // 条目选择
            String pt = (String) list.get(position).get("path");
            String fn = (String) list.get(position).get("name");
            if (fn.equals(sRoot) || fn.equals(sParent)) {
                // 如果是更目录或者上一层
                File fl = new File(pt);
                String ppt = fl.getParent();
                if (ppt != null) {
                    // 返回上一层
                    path = ppt;
                } else {
                    // 返回更目录
                    path = sRoot;
                }
            } else {
                File fl = new File(pt);
                if (fl.isFile()) {
                    // 如果是文件
                    mDialog.dismiss(); // 让文件夹对话框消失

                    // 设置回调的返回值
                    Bundle bundle = new Bundle();
                    bundle.putString("filepath", pt);
                    bundle.putString("filename", fn);
                    // 调用事先设置的回调函数
                    this.callback.callback(bundle);
                    return;
                } else if (fl.isDirectory()) {
                    // 如果是文件夹
                    // 那么进入选中的文件夹
                    path = pt;
                }
            }
            this.refreshFileList();
        }
    }
}
