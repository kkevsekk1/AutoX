package org.autojs.autojs.ui.floating

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.makeramen.roundedimageview.RoundedImageView
import com.stardust.app.DialogUtils
import com.stardust.autojs.core.record.Recorder
import com.stardust.enhancedfloaty.FloatyService
import com.stardust.enhancedfloaty.FloatyWindow
import com.stardust.util.ClipboardUtil
import com.stardust.view.accessibility.AccessibilityService.Companion.instance
import com.stardust.view.accessibility.LayoutInspector.CaptureAvailableListener
import com.stardust.view.accessibility.NodeInfo
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.channels.trySendBlocking
import org.autojs.autojs.Pref
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.autojs.record.GlobalActionRecorder
import org.autojs.autojs.model.explorer.ExplorerDirPage
import org.autojs.autojs.model.explorer.Explorers
import org.autojs.autojs.model.script.Scripts.run
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder
import org.autojs.autojs.tool.AccessibilityServiceTool
import org.autojs.autojs.tool.RootTool
import org.autojs.autojs.ui.common.NotAskAgainDialog
import org.autojs.autojs.ui.common.OperationDialogBuilder
import org.autojs.autojs.ui.explorer.ExplorerViewKt
import org.autojs.autojs.ui.floating.layoutinspector.LayoutBoundsFloatyWindow
import org.autojs.autojs.ui.floating.layoutinspector.LayoutHierarchyFloatyWindow
import org.autojs.autojs.ui.main.MainActivity
import org.autojs.autoxjs.R
import org.greenrobot.eventbus.EventBus

/**
 * Created by Stardust on 2017/10/18.
 */
class CircularMenu(context: Context) : Recorder.OnStateChangedListener, CaptureAvailableListener {
    class StateChangeEvent(val currentState: Int, val previousState: Int)

    private var mWindow: CircularMenuWindow? = null
    private var mState = 0
    private var mActionViewIcon: RoundedImageView? = null
    private val mContext: Context = ContextThemeWrapper(context, R.style.AppTheme)
    private val mRecorder: GlobalActionRecorder
    private var mSettingsDialog: MaterialDialog? = null
    private var mLayoutInspectDialog: MaterialDialog? = null
    private var mRunningPackage: String? = null
    private var mRunningActivity: String? = null

    private val captureChannel = Channel<NodeInfo>()
    private fun setupListeners() {
        mWindow?.setOnActionViewClickListener {
            if (mState == STATE_RECORDING) {
                stopRecord()
            } else if (mWindow?.isExpanded == true) {
                mWindow?.collapse()
            } else {
                AutoJs.getInstance().layoutInspector.captureCurrentWindow()
                mWindow?.expand()
            }
        }
    }

    private fun initFloaty() {
        mWindow = CircularMenuWindow(
            mContext,
            object : CircularMenuFloaty {
                override fun inflateActionView(
                    service: FloatyService,
                    window: CircularMenuWindow
                ): View {
                    val actionView = View.inflate(service, R.layout.circular_action_view, null)
                    mActionViewIcon = actionView.findViewById(R.id.icon)
                    return actionView
                }

                override fun inflateMenuItems(
                    service: FloatyService,
                    window: CircularMenuWindow
                ): CircularActionMenu {
                    val menu = View.inflate(
                        ContextThemeWrapper(service, R.style.AppTheme),
                        R.layout.circular_action_menu,
                        null
                    ) as CircularActionMenu
                    menu.findViewById<View>(R.id.script_list)
                        ?.setOnClickListener { showScriptList() }
                    menu.findViewById<View>(R.id.record)?.setOnClickListener { startRecord() }

                    menu.findViewById<View>(R.id.layout_inspect)
                        ?.setOnClickListener { inspectLayout() }
                    menu.findViewById<View>(R.id.settings)?.setOnClickListener { settings() }
                    menu.findViewById<View>(R.id.stop_all_scripts)
                        ?.setOnClickListener { stopAllScripts() }

                    return menu
                }
            }
        )
        mWindow?.setKeepToSideHiddenWidthRadio(0.25f)
        FloatyService.addWindow(mWindow)
    }

    fun showScriptList() {
        mWindow?.collapse()
        val explorerView = ExplorerViewKt(mContext)
        explorerView.setExplorer(
            Explorers.workspace(),
            ExplorerDirPage.createRoot(Pref.getScriptDirPath())
        )
        explorerView.setDirectorySpanSize(2)
        val dialog = ThemeColorMaterialDialogBuilder(mContext)
            .title(R.string.text_run_script)
            .customView(explorerView, false)
            .positiveText(R.string.cancel)
            .build()
        explorerView.setOnItemOperatedListener {
            dialog.dismiss()
        }
        explorerView.setOnItemClickListener { _, item ->
            item?.let { run(item.toScriptFile()) }
        }
        DialogUtils.showDialog(dialog)
    }

    fun startRecord() {
        mWindow?.collapse()
        if (!RootTool.isRootAvailable()) {
            DialogUtils.showDialog(
                NotAskAgainDialog.Builder(mContext, "CircularMenu.root")
                    .title(R.string.text_device_not_rooted)
                    .content(R.string.prompt_device_not_rooted)
                    .neutralText(R.string.text_device_rooted)
                    .positiveText(R.string.ok)
                    .onNeutral { _, _ -> mRecorder.start() }
                    .build()
            )
        } else {
            mRecorder.start()
        }
    }

    private fun setState(state: Int) {
        val previousState = mState
        mState = state
        mActionViewIcon?.setImageResource(
            if (mState == STATE_RECORDING) R.drawable.ic_ali_record
            else R.drawable.ic_android_eat_js
        )
        //        Color.WHITE);
        mActionViewIcon?.setBackgroundResource(
            if (mState == STATE_RECORDING) R.drawable.circle_red else R.drawable.circle_white
        )
        val padding =
            mContext.resources.getDimension(
                if (mState == STATE_RECORDING) R.dimen.padding_circular_menu_recording
                else R.dimen.padding_circular_menu_normal
            )
                .toInt()
        mActionViewIcon?.setPadding(padding, padding, padding, padding)
        EventBus.getDefault().post(StateChangeEvent(mState, previousState))
    }

    private fun stopRecord() {
        mRecorder.stop()
    }

    fun inspectLayout() {
        mWindow?.collapse()
        mLayoutInspectDialog = OperationDialogBuilder(mContext)
            .item(
                R.id.layout_bounds,
                R.drawable.ic_circular_menu_bounds,
                R.string.text_inspect_layout_bounds
            )
            .item(
                R.id.layout_hierarchy,
                R.drawable.ic_layout_hierarchy,
                R.string.text_inspect_layout_hierarchy
            )
            .bindItemClick(::showLayoutBounds, ::showLayoutHierarchy)
            .title(R.string.text_inspect_layout)
            .build()
        DialogUtils.showDialog(mLayoutInspectDialog)
    }

    fun showLayoutBounds() {
        inspectLayout { rootNode -> rootNode?.let { LayoutBoundsFloatyWindow(it) } }
    }

    fun showLayoutHierarchy() {
        inspectLayout { mRootNode -> mRootNode?.let { LayoutHierarchyFloatyWindow(it) } }
    }

    private fun inspectLayout(windowCreator: (NodeInfo?) -> FloatyWindow?) {
        mLayoutInspectDialog?.dismiss()
        mLayoutInspectDialog = null
        if (instance == null) {
            Toast.makeText(
                mContext,
                R.string.text_no_accessibility_permission_to_capture,
                Toast.LENGTH_SHORT
            ).show()
            AccessibilityServiceTool.goToAccessibilitySetting()
            return
        }
        val progress = DialogUtils.showDialog(
            ThemeColorMaterialDialogBuilder(mContext)
                .content(R.string.text_layout_inspector_is_dumping)
                .canceledOnTouchOutside(false)
                .progress(true, 0)
                .build()
        )
        captureChannel.tryReceive().onSuccess {
            if (!progress.isCancelled) {
                progress.dismiss()
                windowCreator.invoke(it)?.let { FloatyService.addWindow(it) }
            }
        }.onFailure { progress.dismiss() }
    }

    fun stopAllScripts() {
        mWindow?.collapse()
        AutoJs.getInstance().scriptEngineService.stopAllAndToast()
    }

    override fun onCaptureAvailable(capture: NodeInfo) {
        captureChannel.trySendBlocking(capture)
    }

    fun settings() {
        mWindow?.collapse()
        mRunningPackage = AutoJs.getInstance().infoProvider.getLatestPackageByUsageStatsIfGranted()
        mRunningActivity = AutoJs.getInstance().infoProvider.latestActivity
        mSettingsDialog = OperationDialogBuilder(mContext)
            .item(
                R.id.accessibility_service,
                R.drawable.ic_settings,
                R.string.text_accessibility_settings
            )
            .item(
                R.id.package_name,
                R.drawable.ic_android_fill,
                mContext.getString(R.string.text_current_package) + mRunningPackage
            )
            .item(
                R.id.class_name,
                R.drawable.ic_window,
                mContext.getString(R.string.text_current_activity) + mRunningActivity
            )
            .item(
                R.id.open_launcher,
                R.drawable.ic_home_light,
                R.string.text_open_main_activity
            )
            .item(
                R.id.pointer_location,
                R.drawable.ic_coordinate,
                R.string.text_pointer_location
            )
            .item(R.id.exit, R.drawable.ic_close, R.string.text_exit_floating_window)
            .bindItemClick(
                ::enableAccessibilityService,
                ::copyPackageName,
                ::copyActivityName,
                ::openLauncher,
                ::togglePointerLocation,
                ::close,
            )
            .title(R.string.text_more)
            .build()

        DialogUtils.showDialog(mSettingsDialog)
    }

    fun enableAccessibilityService() {
        dismissSettingsDialog()
        AccessibilityServiceTool.enableAccessibilityService()
    }

    private fun dismissSettingsDialog() {
        mSettingsDialog?.dismiss()
        mSettingsDialog = null
    }

    fun copyPackageName() {
        dismissSettingsDialog()
        if (TextUtils.isEmpty(mRunningPackage)) return
        ClipboardUtil.setClip(mContext, mRunningPackage)
        Toast.makeText(mContext, R.string.text_already_copy_to_clip, Toast.LENGTH_SHORT).show()
    }

    fun copyActivityName() {
        dismissSettingsDialog()
        if (TextUtils.isEmpty(mRunningActivity)) return
        ClipboardUtil.setClip(mContext, mRunningActivity)
        Toast.makeText(mContext, R.string.text_already_copy_to_clip, Toast.LENGTH_SHORT).show()
    }

    fun openLauncher() {
        dismissSettingsDialog()
        val intent = Intent(mContext, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        mContext.startActivity(intent)
    }

    fun togglePointerLocation() {
        dismissSettingsDialog()
        RootTool.togglePointerLocation()
    }

    fun close() {
        dismissSettingsDialog()
        try {
            mWindow?.close()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } finally {
            EventBus.getDefault().post(StateChangeEvent(STATE_CLOSED, mState))
            mState = STATE_CLOSED
        }
        mRecorder.removeOnStateChangedListener(this)
        AutoJs.getInstance().layoutInspector.removeCaptureAvailableListener(this)
    }

    override fun onStart() {
        setState(STATE_RECORDING)
    }

    override fun onStop() {
        setState(STATE_NORMAL)
    }

    override fun onPause() = Unit
    override fun onResume() = Unit

    companion object {
        const val STATE_CLOSED = -1
        const val STATE_NORMAL = 0
        const val STATE_RECORDING = 1
    }

    init {
        initFloaty()
        setupListeners()
        mRecorder = GlobalActionRecorder.getSingleton(context)
        mRecorder.addOnStateChangedListener(this)
        AutoJs.getInstance().layoutInspector.addCaptureAvailableListener(this)
    }
}
