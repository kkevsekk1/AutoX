package org.autojs.autojs.ui.doc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
@EFragment(R.layout.fragment_online_docs)
public class DocsFragment extends ViewPagerFragment implements BackPressedHandler, FloatingActionMenu.OnFloatingActionButtonClickListener {

    public static final String ARGUMENT_URL = "url";

    @ViewById(R.id.eweb_view)
    EWebView mEWebView;

    com.tencent.smtt.sdk.WebView mWebView;

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

    static class WebData {
        public String homepage = "";
        public String[] bookmarks = new String[]{
                "https://wht.im",
                "https://github.com/search?q=auto+js+%E8%84%9A%E6%9C%AC&type=repositories",
                "http://mk.autoxjs.com/pages/controlMine/controlMine",
                "http://www.autoxjs.com/"
        };
        public String[] bookmarkLabels = new String[]{
                "万花筒",
                "脚本搜索",
                "脚本市场",
                "交流社区"
        };
        public String[] searchEngines = new String[]{
                "https://cn.bing.com/search?q=",
                "https://yandex.eu/search/?text=",
                "https://www.baidu.com/baidu?word=",
                "https://www.sogou.com/sogou?query=",
                "https://www.zhihu.com/search?type=content&q=",
                "https://github.com/search?q=",
                "https://www.so.com/s?ie=utf-8&fr=so.com&src=home_so.com&q=",
                "https://www.google.com.hk/search?hl=zh-CN&q",
                "https://www.ggssl.com/search?hl=zh-cn&ie=utf-8&q="
        };
        public String[] searchEngineLabels = new String[]{
                "必应",
                "Yandex",
                "百度",
                "搜狗",
                "知乎",
                "GitHub",
                "360",
                "谷歌",
                "谷歌SSL"
        };
        public String[] userAgents = new String[]{
                "Mozilla/5.0 (Linux; U; Android 8.1.0; zh-CN; MI 5X Build/OPM1.171019.019) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/57.0.2987.108 UCBrowser/12.1.2.992 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 7.0; BLN-AL40 Build/HONORBLN-AL40; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/66.0.3359.126 MQQBrowser/6.2 TBS/045130 Mobile Safari/537.36 V1_AND_SQ_8.2.7_1334_YYB_D QQ/8.2.7.4410 NetType/4G WebP/0.3.0 Pixel/1080 StatusBarHeight/72 SimpleUISwitch/0",
                "Mozilla/5.0 (Linux; Android 10; CLT-AL00 Build/HUAWEICLT-AL00; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/76.0.3809.89 Mobile Safari/537.36 T7/11.20 SP-engine/2.16.0 baiduboxapp/11.20.0.14 (Baidu; P1 10) NABar/1.0",
                "Mozilla/5.0 (Linux; Android 9; HMA-AL00 Build/HUAWEIHMA-AL00; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/72.0.3626.121 Mobile Safari/537.36 MMWEBID/1247 MicroMessenger/7.0.10.1561(0x27000A41) Process/tools NetType/4G Language/zh_CN ABI/arm64 GPVersion/1",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 13_6_1 like Mac OS X), AppleWebKit/605.1.15 (KHTML, like Gecko), Version/14.0.3 Safari/605.1.15",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 13_6_1 like Mac OS X), AppleWebKit/605.1.15 (KHTML, like Gecko), Mobile/15E148 MicroMessenger/8.0.17(0x18001122), NetType/4G Language/zh_CN",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_6), AppleWebKit/605.1.15 (KHTML, like Gecko), Version/14.0.3 Safari/605.1.15",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_4), AppleWebKit/537.36 (KHTML, like Gecko), Chrome/99.0.4044.138 Safari/537.36 NetType/WIFI MicroMessenger/8.0.17(0x18001122), WindowsWechat(0x63030522),",
                "Mozilla/5.0 (Windows NT 10.0; WOW64), AppleWebKit/537.36 (KHTML, like Gecko), Chrome/99.0.3904.62 Safari/537.36"
        };
        public String[] userAgentLabels = new String[]{
                "小米5X/UC浏览器",
                "荣耀畅玩6X/QQ浏览器",
                "华为P20 Pro/手机百度",
                "华为Mate20/微信",
                "iPhone/Safari",
                "iPhone/微信",
                "Mac OS X/Safari",
                "Mac OS X/微信",
                "Windows 10/Chrome"
        };
    }

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

    private void loadUrl() {
        if (!Objects.equals(Pref.getWebData(), "")) {
            mWebData = gson.fromJson(Pref.getWebData(), WebData.class);
        } else {
            mWebData = new WebData();
            mWebData.homepage = getArguments().getString(ARGUMENT_URL, Pref.getDocumentationUrl() + "index.html");
        }
        mWebView.loadUrl(mWebData.homepage);
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
        initFloatingActionMenuIfNeeded(fab);
        int[] fabIcons = {
                R.drawable.ic_home_black_48dp,
                R.drawable.ic_star,
                R.drawable.ic_floating_action_menu_open,
                R.drawable.ic_pc_mode,
                R.drawable.ic_check_for_updates,
                R.drawable.ic_web,
                R.drawable.ic_code_black_48dp,
                R.drawable.ic_project};
        String[] fabLabs = {"主页", "收藏", "本地文档", "切换桌面模式", "切换UA", "网址/搜索", "网页源码", "问题反馈"};
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

    @Override
    public void onClick(FloatingActionButton button, int pos) {
        if (!Objects.equals(Pref.getWebData(), "")) {
            mWebData = gson.fromJson(Pref.getWebData(), WebData.class);
        } else {
            mWebData = new WebData();
            mWebData.homepage = getArguments().getString(ARGUMENT_URL, Pref.getDocumentationUrl() + "index.html");
        }
        switch (pos) {
            case 0:
                mWebView.loadUrl(mWebData.homepage);
                break;
            case 1:
                new MaterialDialog.Builder(requireContext())
                        .title("请选择书签：")
                        .positiveText("打开(单选)")
                        .neutralText("删除(多选)")
                        .negativeText("添加当前页")
                        .items(mWebData.bookmarkLabels)
                        .itemsCallbackMultiChoice(null, new MaterialDialog.ListCallbackMultiChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {
                                return true;
                            }
                        })
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
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
                            }
                        })
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(MaterialDialog dialog, DialogAction which) {
                                if (Objects.requireNonNull(dialog.getSelectedIndices()).length > 0) {
                                    mWebView.loadUrl(mWebData.bookmarks[dialog.getSelectedIndices()[0]]);
                                }
                            }
                        })
                        .onNeutral(new MaterialDialog.SingleButtonCallback() {
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
                            }
                        })
                        .show();
                break;
            case 2:
                HashMap<String, Integer> images = new HashMap<String, Integer>();
                // 下面几句设置各文件类型的图标， 需要你先把图标添加到资源文件夹
                images.put(sRoot, R.drawable.filedialog_root);    // 根目录图标
                images.put(sParent, R.drawable.filedialog_folder_up);    //返回上一层的图标
                images.put(sFolder, R.drawable.filedialog_folder);    //文件夹图标
                images.put(sEmpty, R.drawable.filedialog_file);
                mDialog = createDialog(getActivity(), "TBS内核安装完后将支持office、pdf、epub等格式文档查看", new CallbackBundle() {
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
                                }
                            }
                        },
                        "html;htm;xhtml;xml;mhtml;mht;doc;docx;ppt;pptx;xls;xlsx;pdf;txt;js;log;epub",
                        images);
                mDialog.show();
                break;
            case 3:
                mEWebView.switchRescale();
                if (mEWebView.getIsRescale()) {
                    mWebView.getSettings().setLoadWithOverviewMode(true);
                    mWebView.getSettings().setUserAgentString(mWebData.userAgents[6]);
                } else {
                    mWebView.getSettings().setLoadWithOverviewMode(false);
                    mWebView.getSettings().setUserAgentString(mWebData.userAgents[4]);
                }
                mWebView.reload();
                break;
            case 4:
                new MaterialDialog.Builder(requireContext())
                        .title("请选择要使用的User-Agent：")
                        .negativeText("取消")
                        .items(mWebData.userAgentLabels)
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                mWebView.getSettings().setUserAgentString(mWebData.userAgents[which]);
                                mWebView.reload();
                                Toast.makeText(
                                        getContext(),
                                        "UA: " + mWebData.userAgents[which],
                                        Toast.LENGTH_LONG
                                ).show();
                                dialog.dismiss();
                            }
                        })
                        .show();
                break;
            case 5:
                EditText et = new EditText(getContext());
                new MaterialDialog.Builder(requireContext())
                        .title(mWebView.getOriginalUrl())
                        .customView(et, false)
                        .positiveText("打开")
                        .negativeText("设为主页")
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
                                mWebData.homepage = mWebView.getOriginalUrl();
                                Pref.setWebData(gson.toJson(mWebData));
                                Toast.makeText(getContext(), "设置为主页：" + mWebView.getTitle(), Toast.LENGTH_LONG).show();
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
                mWebView.loadUrl("https://github.com/kkevsekk1/AutoX/issues");
                break;
            default:
                mWebView.loadUrl("http://www.autoxjs.com/");
                break;
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
