package org.autojs.autojs.ui.main;

import static android.content.pm.PackageManager.PERMISSION_DENIED;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.internal.NavigationMenuItemView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.stardust.app.FragmentPagerAdapterBuilder;
import com.stardust.app.OnActivityResultDelegate;
import com.stardust.autojs.core.permission.OnRequestPermissionsResultCallback;
import com.stardust.autojs.core.permission.PermissionRequestProxyActivity;
import com.stardust.autojs.core.permission.RequestPermissionCallbacks;
import com.stardust.enhancedfloaty.FloatyService;
import com.stardust.pio.PFiles;
import com.stardust.theme.ThemeColorManager;
import com.stardust.util.BackPressedHandler;
import com.stardust.util.DrawerAutoClose;
import com.tencent.smtt.sdk.QbSdk;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.autojs.autojs.Pref;
import org.autojs.autojs.R;
import org.autojs.autojs.autojs.AutoJs;
import org.autojs.autojs.external.foreground.ForegroundService;
import org.autojs.autojs.model.explorer.Explorers;
import org.autojs.autojs.tool.AccessibilityServiceTool;
import org.autojs.autojs.ui.BaseActivity;
import org.autojs.autojs.ui.common.NotAskAgainDialog;
import org.autojs.autojs.ui.doc.DocsFragment_;
import org.autojs.autojs.ui.doc.DocsFragment_TBS;
import org.autojs.autojs.ui.doc.DocsFragment_TBS_;
import org.autojs.autojs.ui.floating.FloatyWindowManger;
import org.autojs.autojs.ui.log.LogActivity_;
import org.autojs.autojs.ui.main.community.CommunityFragment;
import org.autojs.autojs.ui.main.scripts.MyScriptListFragment_;
import org.autojs.autojs.ui.main.task.TaskManagerFragment_;
import org.autojs.autojs.ui.settings.SettingsActivity;
import org.autojs.autojs.ui.settings.SettingsActivity_;
import org.autojs.autojs.ui.widget.CommonMarkdownView;
import org.autojs.autojs.ui.widget.SearchViewItem;
import org.autojs.autojs.ui.widget.WebData;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

@EActivity(R.layout.activity_main)
public class MainActivity extends BaseActivity implements OnActivityResultDelegate.DelegateHost, BackPressedHandler.HostActivity, PermissionRequestProxyActivity {

    public static class DrawerOpenEvent {
        static DrawerOpenEvent SINGLETON = new DrawerOpenEvent();
    }

    //private static final String  signal ="uyMt3t/FqNUjYvXE6KElfppO17L1Nzhm0mXlnsPBl1o=";
    private static final String LOG_TAG = "MainActivity";

    @ViewById(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @ViewById(R.id.viewpager)
    ViewPager mViewPager;

    @ViewById(R.id.fab)
    FloatingActionButton mFab;

    @ViewById(R.id.navigation_view_right)
    NavigationView rightDrawer;

    private static final Pattern SERVICE_PATTERN = Pattern.compile("^(((\\w+\\.)+\\w+)[/]?){2}$");

    private FragmentPagerAdapterBuilder.StoredFragmentPagerAdapter mPagerAdapter;
    private OnActivityResultDelegate.Mediator mActivityResultMediator = new OnActivityResultDelegate.Mediator();
    private RequestPermissionCallbacks mRequestPermissionCallbacks = new RequestPermissionCallbacks();
    //private VersionGuard mVersionGuard;
    private BackPressedHandler.Observer mBackPressObserver = new BackPressedHandler.Observer();
    private SearchViewItem mSearchViewItem;
    private MenuItem mLogMenuItem;
    private boolean mDocsSearchItemExpanded;
    Gson gson = new Gson();
    WebData mWebData;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
        showAccessibilitySettingPromptIfDisabled();
        //mVersionGuard = new VersionGuard(this);
        showAnnunciationIfNeeded();
        EventBus.getDefault().register(this);
        applyDayNightMode();
        if (Pref.getWebData().contains("isTbs")) {
            mWebData = gson.fromJson(Pref.getWebData(), WebData.class);
        } else {
            mWebData = new WebData();
            Pref.setWebData(gson.toJson(mWebData));
        }
        if (mWebData.isTbs) {
            QbSdk.initX5Environment(this, new QbSdk.PreInitCallback() {
                @Override
                public void onCoreInitFinished() {
                    // 内核初始化完成，可能为系统内核，也可能为系统内核
                }

                /**
                 * 预初始化结束
                 * 由于X5内核体积较大，需要依赖网络动态下发，所以当内核不存在的时候，默认会回调false，此时将会使用系统内核代替
                 * @param isX5 是否使用X5内核
                 */
                @Override
                public void onViewInitFinished(boolean isX5) {
                }
            });
        }
    }

    @SuppressLint("RestrictedApi")
    @AfterViews
    void setUpViews() {
        setUpToolbar();
        setUpTabViewPager();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        registerBackPressHandlers();
        ThemeColorManager.addViewBackground(findViewById(R.id.app_bar));
        mDrawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                EventBus.getDefault().post(DrawerOpenEvent.SINGLETON);
            }
        });
        rightDrawer.setNavigationItemSelectedListener(menuItem -> {
            if (Pref.getWebData().contains("isTbs")) {
                mWebData = gson.fromJson(Pref.getWebData(), WebData.class);
            } else {
                mWebData = new WebData();
                Pref.setWebData(gson.toJson(mWebData));
            }
            switch (menuItem.getItemId()) {
                case R.id.ali_exit:
                    exitCompletely();
                    break;
                case R.id.ali_settings:
                    startActivity(new Intent(this, SettingsActivity_.class));
                    break;
                case R.id.file_manager_permission:
                    if (Build.VERSION.SDK_INT >= 30) {
                        new MaterialDialog.Builder(this)
                                .title("所有文件访问权限")
                                .content("在Android 11+ 的系统中，读写非应用目录外文件需要授予“所有文件访问权限”（右侧侧滑菜单中设置），部分设备授予后可能出现文件读写异常，建议仅在无法读写文件时授予。请选择是否授予该权限：")
                                .positiveText("前往授权")
                                .negativeText("取消")
                                .onPositive((dialog, which) -> {
                                    dialog.dismiss();
                                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                                    intent.setData(Uri.parse("package:" + this.getPackageName()));
                                    startActivity(intent);
                                    dialog.dismiss();
                                })
                                .show();
                    } else {
                        Toast.makeText(this, "Android 10 及以下系统无需设置该项", Toast.LENGTH_LONG).show();
                    }
                    break;
                case R.id.theme_color:
                    SettingsActivity.selectThemeColor(this);
                    break;
                case R.id.web_bookmarks:
                    new MaterialDialog.Builder(this)
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
                case R.id.web_kernel:
                    mWebData.isTbs = !mWebData.isTbs;
                    Pref.setWebData(gson.toJson(mWebData));
                    if (mWebData.isTbs) {
                        Toast.makeText(this, "默认Web内核已切换为：TBS WebView，重启APP后生效！", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "默认Web内核已切换为：系统 WebView，重启APP后生效！", Toast.LENGTH_LONG).show();
                    }
                    break;
                case R.id.web_ua:
                    new MaterialDialog.Builder(this)
                            .title("请选择默认的User-Agent：")
                            .negativeText("取消")
                            .items(mWebData.userAgentLabels)
                            .itemsCallback(new MaterialDialog.ListCallback() {
                                @Override
                                public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                    mWebData.userAgent = mWebData.userAgents[which];
                                    Pref.setWebData(gson.toJson(mWebData));
                                    dialog.dismiss();
                                }
                            })
                            .show();
                    break;
            }
            return false;
        });
    }

    private void showAnnunciationIfNeeded() {
        if (!Pref.shouldShowAnnunciation()) {
            return;
        }
        new CommonMarkdownView.DialogBuilder(this)
                .padding(36, 0, 36, 0)
                .markdown(PFiles.read(getResources().openRawResource(R.raw.annunciation)))
                .title(R.string.text_annunciation)
                .positiveText(R.string.ok)
                .canceledOnTouchOutside(false)
                .show();
    }


    private void registerBackPressHandlers() {
        mBackPressObserver.registerHandler(new DrawerAutoClose(mDrawerLayout, Gravity.START));
        mBackPressObserver.registerHandler(new BackPressedHandler.DoublePressExit(this, R.string.text_press_again_to_exit));
    }


    private void checkPermissions() {
        // 检测存储权限
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (Build.VERSION.SDK_INT >= 30) {
            if (Pref.getPermissionCheck() && !Environment.isExternalStorageManager()) {
                new MaterialDialog.Builder(this)
                        .title("“所有文件访问权限”说明")
                        .content("在Android 11+ 的系统中，读写非应用目录外文件需要授予“所有文件访问权限”（右侧侧滑菜单中设置），部分设备授予后可能出现文件读写异常，建议仅在无法读写文件时授予。")
                        .positiveText("确定")
                        .neutralText("不再提示")
                        .onNeutral((dialog, which) -> {
                            Pref.setPermissionCheck(false);
                        })
                        .show();
            }
        }
    }

    private void showAccessibilitySettingPromptIfDisabled() {
        if (AccessibilityServiceTool.isAccessibilityServiceEnabled(this)) {
            return;
        }
        new NotAskAgainDialog.Builder(this, "MainActivity.accessibility")
                .title(R.string.text_need_to_enable_accessibility_service)
                .content(R.string.explain_accessibility_permission)
                .positiveText(R.string.text_go_to_setting)
                .negativeText(R.string.text_cancel)
                .onPositive((dialog, which) ->
                        AccessibilityServiceTool.enableAccessibilityService()
                ).show();
    }

    private void setUpToolbar() {
        Toolbar toolbar = $(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.app_name);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.text_drawer_open,
                R.string.text_drawer_close);
        drawerToggle.syncState();
        mDrawerLayout.addDrawerListener(drawerToggle);
    }

    private void setUpTabViewPager() {
        TabLayout tabLayout = $(R.id.tab);
        if (Pref.getWebData().contains("isTbs")) {
            mWebData = gson.fromJson(Pref.getWebData(), WebData.class);
        } else {
            mWebData = new WebData();
            Pref.setWebData(gson.toJson(mWebData));
        }
        if (mWebData.isTbs) {
            mPagerAdapter = new FragmentPagerAdapterBuilder(this)
                    .add(new MyScriptListFragment_(), R.string.text_file)
                    .add(new TaskManagerFragment_(), R.string.text_manage)
                    .add(new DocsFragment_TBS_(), R.string.text_WebX)
//                .add(new CommunityFragment_(), R.string.text_community)
//                .add(new MarketFragment_(), R.string.text_market)
                    .build();
        } else {
            mPagerAdapter = new FragmentPagerAdapterBuilder(this)
                    .add(new MyScriptListFragment_(), R.string.text_file)
                    .add(new TaskManagerFragment_(), R.string.text_manage)
                    .add(new DocsFragment_(), R.string.text_WebX)
//                .add(new CommunityFragment_(), R.string.text_community)
//                .add(new MarketFragment_(), R.string.text_market)
                    .build();
        }
        mViewPager.setAdapter(mPagerAdapter);
        tabLayout.setupWithViewPager(mViewPager);
        setUpViewPagerFragmentBehaviors();
    }

    private void setUpViewPagerFragmentBehaviors() {
        mPagerAdapter.setOnFragmentInstantiateListener((pos, fragment) -> {
            ((ViewPagerFragment) fragment).setFab(mFab);
            if (pos == mViewPager.getCurrentItem()) {
                ((ViewPagerFragment) fragment).onPageShow();
            }
        });
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            private ViewPagerFragment mPreviousFragment;

            @Override
            public void onPageSelected(int position) {
                Fragment fragment = mPagerAdapter.getStoredFragment(position);
                if (fragment == null)
                    return;
                if (mPreviousFragment != null) {
                    mPreviousFragment.onPageHide();
                }
                mPreviousFragment = (ViewPagerFragment) fragment;
                mPreviousFragment.onPageShow();
            }
        });
    }


    @Click(R.id.setting)
    void startSettingActivity() {
        startActivity(new Intent(this, SettingsActivity_.class));
    }

    @Click(R.id.exit)
    public void exitCompletely() {
        finish();
        FloatyWindowManger.hideCircularMenu();
        ForegroundService.stop(this);
        stopService(new Intent(this, FloatyService.class));
        AutoJs.getInstance().getScriptEngineService().stopAll();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    @SuppressLint("CheckResult")
    protected void onResume() {
        super.onResume();

        //TimedTaskScheduler.ensureCheckTaskWorks(getApplicationContext());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mActivityResultMediator.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (mRequestPermissionCallbacks.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            return;
        }
        if (getGrantResult(Manifest.permission.READ_EXTERNAL_STORAGE, permissions, grantResults) == PackageManager.PERMISSION_GRANTED) {
            Explorers.workspace().refreshAll();
            //AutoJs.getInstance().setLogFilePath(Pref.getScriptDirPath(), BuildConfig.DEBUG);
        }
    }

    private int getGrantResult(String permission, String[] permissions, int[] grantResults) {
        int i = Arrays.asList(permissions).indexOf(permission);
        if (i < 0) {
            return 2;
        }
        return grantResults[i];
    }

    @Override
    protected void onStart() {
        super.onStart();
        //if (!BuildConfig.DEBUG) {
        //    DeveloperUtils.verifyApk(this, signal, R.string.dex_crcs);
        //}
    }


    @NonNull
    @Override
    public OnActivityResultDelegate.Mediator getOnActivityResultDelegateMediator() {
        return mActivityResultMediator;
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = mPagerAdapter.getStoredFragment(mViewPager.getCurrentItem());
        if (fragment instanceof BackPressedHandler) {
            if (((BackPressedHandler) fragment).onBackPressed(this)) {
                return;
            }
        }
        if (!mBackPressObserver.onBackPressed(this)) {
            super.onBackPressed();
        }
    }

    @Override
    public void addRequestPermissionsCallback(OnRequestPermissionsResultCallback callback) {
        mRequestPermissionCallbacks.addCallback(callback);
    }

    @Override
    public boolean removeRequestPermissionsCallback(OnRequestPermissionsResultCallback callback) {
        return mRequestPermissionCallbacks.removeCallback(callback);
    }


    @Override
    public BackPressedHandler.Observer getBackPressedObserver() {
        return mBackPressObserver;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        mLogMenuItem = menu.findItem(R.id.action_log);
        setUpSearchMenuItem(searchMenuItem);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_log) {
            if (mDocsSearchItemExpanded) {
                submitForwardQuery();
            } else {
                LogActivity_.intent(this).start();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Subscribe
    public void onLoadUrl(CommunityFragment.LoadUrl loadUrl) {
        mDrawerLayout.closeDrawer(GravityCompat.START);
    }


    private void setUpSearchMenuItem(MenuItem searchMenuItem) {
        mSearchViewItem = new SearchViewItem(this, searchMenuItem) {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                if (mViewPager.getCurrentItem() == 1) {
                    mDocsSearchItemExpanded = true;
                    mLogMenuItem.setIcon(R.drawable.ic_ali_up);
                }
                return super.onMenuItemActionExpand(item);
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                if (mDocsSearchItemExpanded) {
                    mDocsSearchItemExpanded = false;
                    mLogMenuItem.setIcon(R.drawable.ic_ali_log);
                }
                return super.onMenuItemActionCollapse(item);
            }
        };
        mSearchViewItem.setQueryCallback(this::submitQuery);
    }

    private void submitQuery(String query) {
        if (query == null) {
            EventBus.getDefault().post(QueryEvent.CLEAR);
            return;
        }
        QueryEvent event = new QueryEvent(query);
        EventBus.getDefault().post(event);
        if (event.shouldCollapseSearchView()) {
            mSearchViewItem.collapse();
        }
    }

    private void submitForwardQuery() {
        QueryEvent event = QueryEvent.FIND_FORWARD;
        EventBus.getDefault().post(event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
