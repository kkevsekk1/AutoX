package org.autojs.autojs.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.MaterialDialog.Builder
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.stardust.app.FragmentPagerAdapterBuilder
import com.stardust.app.FragmentPagerAdapterBuilder.StoredFragmentPagerAdapter
import com.stardust.app.OnActivityResultDelegate.DelegateHost
import com.stardust.app.OnActivityResultDelegate.Mediator
import com.stardust.autojs.core.permission.OnRequestPermissionsResultCallback
import com.stardust.autojs.core.permission.PermissionRequestProxyActivity
import com.stardust.autojs.core.permission.RequestPermissionCallbacks
import com.stardust.enhancedfloaty.FloatyService
import com.stardust.pio.PFiles
import com.stardust.theme.ThemeColorManager
import com.stardust.util.BackPressedHandler
import com.stardust.util.BackPressedHandler.DoublePressExit
import com.stardust.util.BackPressedHandler.HostActivity
import com.stardust.util.DrawerAutoClose
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.QbSdk.PreInitCallback
import org.androidannotations.annotations.AfterViews
import org.androidannotations.annotations.Click
import org.androidannotations.annotations.EActivity
import org.androidannotations.annotations.ViewById
import org.autojs.autojs.Pref
import org.autojs.autojs.R
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.external.foreground.ForegroundService
import org.autojs.autojs.model.explorer.Explorers
import org.autojs.autojs.timing.TimedTaskScheduler
import org.autojs.autojs.tool.AccessibilityServiceTool
import org.autojs.autojs.ui.BaseActivity
import org.autojs.autojs.ui.common.NotAskAgainDialog
import org.autojs.autojs.ui.doc.DocsFragment.LoadUrl
import org.autojs.autojs.ui.doc.DocsFragment_
import org.autojs.autojs.ui.floating.FloatyWindowManger
import org.autojs.autojs.ui.log.LogActivity_
//import org.autojs.autojs.ui.main.scripts.MyScriptListFragment_
import org.autojs.autojs.ui.main.task.TaskManagerFragment_
import org.autojs.autojs.ui.settings.SettingsActivity_
import org.autojs.autojs.ui.widget.CommonMarkdownView.DialogBuilder
import org.autojs.autojs.ui.widget.SearchViewItem
import org.autojs.autojs.ui.widget.WebData
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.*
import java.util.regex.Pattern

@Deprecated("Use MainActivityCompose")
@EActivity(R.layout.activity_main)
open class MainActivity : BaseActivity(), DelegateHost, HostActivity,
    PermissionRequestProxyActivity {
    object DrawerOpenEvent {
        var SINGLETON: DrawerOpenEvent = DrawerOpenEvent
    }

    @JvmField
    @ViewById(R.id.drawer_layout)
    var mDrawerLayout: DrawerLayout? = null

    @JvmField
    @ViewById(R.id.viewpager)
    var mViewPager: ViewPager? = null

    @JvmField
    @ViewById(R.id.fab)
    var mFab: FloatingActionButton? = null

    @JvmField
    @ViewById(R.id.navigation_view_right)
    var rightDrawer: NavigationView? = null
    private var mPagerAdapter: StoredFragmentPagerAdapter? = null
    private val mActivityResultMediator = Mediator()
    private val mRequestPermissionCallbacks = RequestPermissionCallbacks()

    //private VersionGuard mVersionGuard;
    private val mBackPressObserver = BackPressedHandler.Observer()
    private var mSearchViewItem: SearchViewItem? = null
    private var mLogMenuItem: MenuItem? = null
    private var mDocsSearchItemExpanded = false
    var gson = Gson()
    var mWebData: WebData? = null
    private var mTabLayout: TabLayout? = null
    private var mToolbar: Toolbar? = null
    private var mAppBarLayout: AppBarLayout? = null
    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
        showAccessibilitySettingPromptIfDisabled()
        //mVersionGuard = new VersionGuard(this);
        showAnnunciationIfNeeded()
        EventBus.getDefault().register(this)
        applyDayNightMode()
        if (Pref.getWebData().contains("isTbs")) {
            mWebData = gson.fromJson(Pref.getWebData(), WebData::class.java)
        } else {
            mWebData = WebData()
            Pref.setWebData(gson.toJson(mWebData))
        }
        if (mWebData!!.isTbs) {
            QbSdk.initX5Environment(this, object : PreInitCallback {
                override fun onCoreInitFinished() {
                    // 内核初始化完成，可能为系统内核，也可能为系统内核
                }

                /**
                 * 预初始化结束
                 * 由于X5内核体积较大，需要依赖网络动态下发，所以当内核不存在的时候，默认会回调false，此时将会使用系统内核代替
                 * @param isX5 是否使用X5内核
                 */
                override fun onViewInitFinished(isX5: Boolean) {}
            })
        }
    }

    @SuppressLint("RestrictedApi")
    @AfterViews
    fun setUpViews() {
        setUpToolbar()
        setUpTabViewPager()
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        registerBackPressHandlers()
        mAppBarLayout = findViewById(R.id.app_bar)
        ThemeColorManager.addViewBackground(mAppBarLayout)
        mDrawerLayout!!.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                EventBus.getDefault().post(DrawerOpenEvent.SINGLETON)
            }
        })
        rightDrawer!!.setNavigationItemSelectedListener { menuItem: MenuItem ->
            if (Pref.getWebData().contains("isTbs")) {
                mWebData = gson.fromJson(Pref.getWebData(), WebData::class.java)
            } else {
                mWebData = WebData()
                Pref.setWebData(gson.toJson(mWebData))
            }
            when (menuItem.itemId) {
                R.id.ali_exit -> exitCompletely()
                R.id.ali_settings -> startActivity(Intent(this, SettingsActivity_::class.java))
                R.id.file_manager_permission -> if (Build.VERSION.SDK_INT >= 30) {
                    Builder(this)
                        .title("所有文件访问权限")
                        .content("在Android 11+ 的系统中，读写非应用目录外文件需要授予“所有文件访问权限”（右侧侧滑菜单中设置），部分设备授予后可能出现文件读写异常，建议仅在无法读写文件时授予。请选择是否授予该权限：")
                        .positiveText("前往授权")
                        .negativeText("取消")
                        .onPositive { dialog: MaterialDialog, _: DialogAction? ->
                            dialog.dismiss()
                            val intent =
                                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            intent.data = Uri.parse("package:" + this.packageName)
                            startActivity(intent)
                            dialog.dismiss()
                        }
                        .show()
                } else {
                    Toast.makeText(this, "Android 10 及以下系统无需设置该项", Toast.LENGTH_LONG).show()
                }
                R.id.switch_fullscreen -> if (mAppBarLayout?.visibility != View.GONE) {
                    mTabLayout!!.visibility = View.GONE
                    mAppBarLayout?.visibility = View.GONE
                    mFab!!.hide()
                    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE)
                } else {
                    mTabLayout!!.visibility = View.VISIBLE
                    mAppBarLayout?.visibility = View.VISIBLE
                    mFab!!.show()
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
                R.id.switch_line_wrap -> {
                    Pref.setLineWrap(!Pref.getLineWrap())
                    if (Pref.getLineWrap()) {
                        Toast.makeText(this, "已打开编辑器自动换行，重启编辑器后生效！", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "已关闭编辑器自动换行，重启编辑器后生效！", Toast.LENGTH_LONG).show()
                    }
                }
                R.id.switch_task_manager -> {
                    val taskManagerList = arrayOf("WorkManager", "AndroidJob", "AlarmManager")
                    Builder(this)
                        .title("请选择定时任务调度器：")
                        .negativeText("取消")
                        .items(*taskManagerList)
                        .itemsCallback { dialog, _, which, _ ->
                            Pref.setTaskManager(which)
                            Toast.makeText(
                                applicationContext,
                                "定时任务调度器已切换为：" + taskManagerList[which] + "，重启APP后生效！",
                                Toast.LENGTH_LONG
                            ).show()
                            AutoJs.getInstance()
                                .debugInfo("切换任务调度模式为：" + taskManagerList[which] + "，重启APP后生效！")
                            dialog.dismiss()
                        }
                        .onPositive { dialog, _ ->
                            if (Objects.requireNonNull(dialog.selectedIndices).size >= mWebData!!.bookmarks.size) {
                                mWebData!!.bookmarks = arrayOf()
                                mWebData!!.bookmarkLabels = arrayOf()
                                Pref.setWebData(gson.toJson(mWebData))
                            } else if (Objects.requireNonNull(dialog.selectedIndices).isNotEmpty()) {
                                val strList = arrayOfNulls<String>(
                                    mWebData!!.bookmarks.size - dialog.selectedIndices!!.size
                                )
                                val strLabelList = arrayOfNulls<String>(
                                    mWebData!!.bookmarks.size - dialog.selectedIndices!!.size
                                )
                                var j = 0
                                var i = 0
                                while (i < mWebData!!.bookmarks.size) {
                                    var flag = true
                                    for (index: Int in dialog.selectedIndices!!) {
                                        if (i == index) {
                                            flag = false
                                            break
                                        }
                                    }
                                    if (flag) {
                                        strList[j] = mWebData!!.bookmarks[i]
                                        strLabelList[j] = mWebData!!.bookmarkLabels[i]
                                        j += 1
                                    }
                                    i++
                                }
                                mWebData!!.bookmarks = strList
                                mWebData!!.bookmarkLabels = strLabelList
                                Pref.setWebData(gson.toJson(mWebData))
                            }
                            dialog.dismiss()
                        }
                        .show()
                    TimedTaskScheduler.ensureCheckTaskWorks(this)
                }
                R.id.web_bookmarks -> Builder(this)
                    .title("请选择书签：")
                    .positiveText("删除(多选)")
                    .negativeText("取消")
                    .items(*mWebData!!.bookmarkLabels)
                    .itemsCallbackMultiChoice(null
                    ) { _, _, _ -> true }
                    .onPositive { dialog, _ ->
                        if (Objects.requireNonNull(dialog.selectedIndices).size >= mWebData!!.bookmarks.size) {
                            mWebData!!.bookmarks = arrayOf()
                            mWebData!!.bookmarkLabels = arrayOf()
                            Pref.setWebData(gson.toJson(mWebData))
                        } else if (Objects.requireNonNull(dialog.selectedIndices).isNotEmpty()) {
                            val strList = arrayOfNulls<String>(
                                mWebData!!.bookmarks.size - dialog.selectedIndices!!.size
                            )
                            val strLabelList = arrayOfNulls<String>(
                                mWebData!!.bookmarks.size - dialog.selectedIndices!!.size
                            )
                            var j = 0
                            var i = 0
                            while (i < mWebData!!.bookmarks.size) {
                                var flag = true
                                for (index: Int in dialog.selectedIndices!!) {
                                    if (i == index) {
                                        flag = false
                                        break
                                    }
                                }
                                if (flag) {
                                    strList[j] = mWebData!!.bookmarks[i]
                                    strLabelList[j] = mWebData!!.bookmarkLabels[i]
                                    j += 1
                                }
                                i++
                            }
                            mWebData!!.bookmarks = strList
                            mWebData!!.bookmarkLabels = strLabelList
                            Pref.setWebData(gson.toJson(mWebData))
                        }
                        dialog.dismiss()
                    }
                    .show()
                R.id.web_kernel -> {
                    mWebData!!.isTbs = !mWebData!!.isTbs
                    Pref.setWebData(gson.toJson(mWebData))
                    if (mWebData!!.isTbs) {
                        Toast.makeText(this, "默认Web内核已切换为：TBS WebView，重启APP后生效！", Toast.LENGTH_LONG)
                            .show()
                    } else {
                        Toast.makeText(this, "默认Web内核已切换为：系统 WebView，重启APP后生效！", Toast.LENGTH_LONG)
                            .show()
                    }
                }
                R.id.web_ua -> Builder(this)
                    .title("请选择默认的User-Agent：")
                    .negativeText("取消")
                    .items(*mWebData!!.userAgentLabels)
                    .itemsCallback { dialog, _, which, _ ->
                        mWebData!!.userAgent = mWebData!!.userAgents[which]
                        Pref.setWebData(gson.toJson(mWebData))
                        dialog.dismiss()
                    }
                    .show()
            }
            false
        }
    }

    private fun showAnnunciationIfNeeded() {
        if (!Pref.shouldShowAnnunciation()) {
            return
        }
        DialogBuilder(this)
            .padding(36, 0, 36, 0)
            .markdown(PFiles.read(resources.openRawResource(R.raw.annunciation)))
            .title(R.string.text_annunciation)
            .positiveText(R.string.ok)
            .canceledOnTouchOutside(false)
            .show()
    }

    private fun registerBackPressHandlers() {
        mBackPressObserver.registerHandler(DrawerAutoClose(mDrawerLayout, Gravity.START))
        mBackPressObserver.registerHandler(DoublePressExit(this, R.string.text_press_again_to_exit))
    }

    private fun checkPermissions() {
        // 检测存储权限
        checkPermission(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (Build.VERSION.SDK_INT >= 30) {
            if (Pref.getPermissionCheck() && !Environment.isExternalStorageManager()) {
                Builder(this)
                    .title("“所有文件访问权限”说明")
                    .content("在Android 11+ 的系统中，读写非应用目录外文件需要授予“所有文件访问权限”（右侧侧滑菜单中设置），部分设备授予后可能出现文件读写异常，建议仅在无法读写文件时授予。")
                    .positiveText("确定")
                    .neutralText("不再提示")
                    .onNeutral { _: MaterialDialog?, _: DialogAction? ->
                        Pref.setPermissionCheck(
                            false
                        )
                    }
                    .show()
            }
        }
    }

    private fun showAccessibilitySettingPromptIfDisabled() {
        if (AccessibilityServiceTool.isAccessibilityServiceEnabled(this)) {
            return
        }
        NotAskAgainDialog.Builder(this, "MainActivity.accessibility")
            .title(R.string.text_need_to_enable_accessibility_service)
            .content(R.string.explain_accessibility_permission)
            .positiveText(R.string.text_go_to_setting)
            .negativeText(R.string.text_cancel)
            .onPositive { _: MaterialDialog?, _: DialogAction? -> AccessibilityServiceTool.enableAccessibilityService() }
            .show()
    }

    private fun setUpToolbar() {
        mToolbar = `$`(R.id.toolbar)
        setSupportActionBar(mToolbar)
        mToolbar?.setTitle(R.string.app_name)
        val drawerToggle = ActionBarDrawerToggle(
            this, mDrawerLayout, mToolbar, R.string.text_drawer_open,
            R.string.text_drawer_close
        )
        drawerToggle.syncState()
        mDrawerLayout!!.addDrawerListener(drawerToggle)
    }

    private fun setUpTabViewPager() {
        mTabLayout = `$`(R.id.tab)
        if (Pref.getWebData().contains("isTbs")) {
            mWebData = gson.fromJson(Pref.getWebData(), WebData::class.java)
        } else {
            mWebData = WebData()
            Pref.setWebData(gson.toJson(mWebData))
        }
        mPagerAdapter = FragmentPagerAdapterBuilder(this)
//            .add(MyScriptListFragment_(), R.string.text_file)
            .add(TaskManagerFragment_(), R.string.text_manage)
            .add(DocsFragment_(), R.string.text_WebX)
            .build()
        mViewPager!!.adapter = mPagerAdapter
        mTabLayout?.setupWithViewPager(mViewPager)
        setUpViewPagerFragmentBehaviors()
    }

    private fun setUpViewPagerFragmentBehaviors() {
        mPagerAdapter!!.setOnFragmentInstantiateListener { pos: Int, fragment: Fragment ->
            (fragment as ViewPagerFragment).setFab(mFab)
            if (pos == mViewPager!!.currentItem) {
                fragment.onPageShow()
            }
        }
        mViewPager!!.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
            private var mPreviousFragment: ViewPagerFragment? = null
            override fun onPageSelected(position: Int) {
                val fragment = mPagerAdapter!!.getStoredFragment(position) ?: return
                if (mPreviousFragment != null) {
                    mPreviousFragment!!.onPageHide()
                }
                mPreviousFragment = fragment as ViewPagerFragment
                mPreviousFragment!!.onPageShow()
            }
        })
    }

    @Click(R.id.setting)
    fun startSettingActivity() {
        startActivity(Intent(this, SettingsActivity_::class.java))
    }

    @Click(R.id.exit)
    fun exitCompletely() {
        finish()
        FloatyWindowManger.hideCircularMenu()
        ForegroundService.stop(this)
        stopService(Intent(this, FloatyService::class.java))
        AutoJs.getInstance().scriptEngineService.stopAll()
    }

    override fun onPause() {
        super.onPause()
    }

    @SuppressLint("CheckResult")
    override fun onResume() {
        super.onResume()
        TimedTaskScheduler.ensureCheckTaskWorks(applicationContext)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mActivityResultMediator.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (mRequestPermissionCallbacks.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
            )
        ) {
            return
        }
        if (getGrantResult(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                permissions,
                grantResults
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Explorers.workspace().refreshAll()
            //AutoJs.getInstance().setLogFilePath(Pref.getScriptDirPath(), BuildConfig.DEBUG);
        }
    }

    private fun getGrantResult(
        permission: String,
        permissions: Array<String>,
        grantResults: IntArray
    ): Int {
        val i = listOf(*permissions).indexOf(permission)
        return if (i < 0) {
            2
        } else grantResults[i]
    }

    override fun onStart() {
        super.onStart()
        //if (!BuildConfig.DEBUG) {
        //    DeveloperUtils.verifyApk(this, signal, R.string.dex_crcs);
        //}
    }

    override fun getOnActivityResultDelegateMediator(): Mediator {
        return mActivityResultMediator
    }

    override fun onBackPressed() {
        val fragment = mPagerAdapter!!.getStoredFragment(
            mViewPager!!.currentItem
        )
        if (fragment is BackPressedHandler) {
            if ((fragment as BackPressedHandler).onBackPressed(this)) {
                return
            }
        }
        if (!mBackPressObserver.onBackPressed(this)) {
            super.onBackPressed()
        }
    }

    override fun addRequestPermissionsCallback(callback: OnRequestPermissionsResultCallback) {
        mRequestPermissionCallbacks.addCallback(callback)
    }

    override fun removeRequestPermissionsCallback(callback: OnRequestPermissionsResultCallback): Boolean {
        return mRequestPermissionCallbacks.removeCallback(callback)
    }

    override fun getBackPressedObserver(): BackPressedHandler.Observer {
        return mBackPressObserver
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchMenuItem = menu.findItem(R.id.action_search)
        mLogMenuItem = menu.findItem(R.id.action_log)
        setUpSearchMenuItem(searchMenuItem)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_log) {
            if (mDocsSearchItemExpanded) {
                submitForwardQuery()
            } else {
                LogActivity_.intent(this).start()
            }
            return true
        } else if (item.itemId == R.id.action_fullscreen) {
            if ((window.decorView.windowSystemUiVisibility and View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN == 0) or (window.decorView.windowSystemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0)) {
                mTabLayout!!.visibility = View.GONE
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LOW_PROFILE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        or View.SYSTEM_UI_FLAG_IMMERSIVE)
            } else {
                mTabLayout!!.visibility = View.VISIBLE
                mAppBarLayout!!.visibility = View.VISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        } else if (item.itemId == R.id.action_drawer_right) {
            mDrawerLayout!!.openDrawer(rightDrawer!!)
        }
        return super.onOptionsItemSelected(item)
    }

    @Subscribe
    fun onLoadUrl(loadUrl: LoadUrl?) {
        mDrawerLayout!!.closeDrawer(GravityCompat.START)
    }

    private fun setUpSearchMenuItem(searchMenuItem: MenuItem) {
        mSearchViewItem = object : SearchViewItem(this, searchMenuItem) {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                if (mViewPager!!.currentItem == 1) {
                    mDocsSearchItemExpanded = true
                    mLogMenuItem!!.setIcon(R.drawable.ic_ali_up)
                }
                return super.onMenuItemActionExpand(item)
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                if (mDocsSearchItemExpanded) {
                    mDocsSearchItemExpanded = false
                    mLogMenuItem!!.setIcon(R.drawable.ic_ali_log)
                }
                return super.onMenuItemActionCollapse(item)
            }
        }
        mSearchViewItem?.setQueryCallback { query: String? -> submitQuery(query) }
    }

    private fun submitQuery(query: String?) {
        if (query == null) {
            EventBus.getDefault().post(QueryEvent.CLEAR)
            return
        }
        val event = QueryEvent(query)
        EventBus.getDefault().post(event)
        if (event.shouldCollapseSearchView()) {
            mSearchViewItem!!.collapse()
        }
    }

    private fun submitForwardQuery() {
        val event = QueryEvent.FIND_FORWARD
        EventBus.getDefault().post(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    companion object {
        //private static final String  signal ="uyMt3t/FqNUjYvXE6KElfppO17L1Nzhm0mXlnsPBl1o=";
        private const val TAG = "MainActivity"
        private val SERVICE_PATTERN = Pattern.compile("^(((\\w+\\.)+\\w+)[/]?){2}$")
    }
}