package org.autojs.autojs.ui.floating.layoutinspector

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import com.stardust.app.DialogUtils
import com.stardust.enhancedfloaty.FloatyService
import com.stardust.view.accessibility.LayoutInspector
import com.stardust.view.accessibility.LayoutInspector.CaptureAvailableListener
import com.stardust.view.accessibility.NodeInfo
import org.autojs.autoxjs.R
import org.autojs.autojs.ui.codegeneration.CodeGenerateDialog
import org.autojs.autojs.ui.floating.FloatyWindowManger
import org.autojs.autojs.ui.floating.FullScreenFloatyWindow
import org.autojs.autojs.ui.widget.BubblePopupMenu
import java.util.*

/**
 * Created by Stardust on 2017/3/12.
 */
open class LayoutBoundsFloatyWindow(private val mRootNode: NodeInfo?) : FullScreenFloatyWindow() {
    private var mLayoutBoundsView: LayoutBoundsView? = null
    private var mNodeInfoDialog: MaterialDialog? = null
    private var mBubblePopMenu: BubblePopupMenu? = null
    private var mNodeInfoView: NodeInfoView? = null
    private var mSelectedNode: NodeInfo? = null
    private var mContext: Context? = null
    override fun onCreateView(floatyService: FloatyService): View {
        mContext = ContextThemeWrapper(floatyService, R.style.AppTheme)
        mLayoutBoundsView = object : LayoutBoundsView(mContext) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    close()
                    return true
                }
                return super.dispatchKeyEvent(event)
            }
        }
        return mLayoutBoundsView!!
    }

    override fun onViewCreated(v: View) {
        mLayoutBoundsView!!.setOnNodeInfoSelectListener { info: NodeInfo ->
            mSelectedNode = info
            ensureOperationPopMenu()
            if (mBubblePopMenu!!.contentView.measuredWidth <= 0) mBubblePopMenu!!.preMeasure()
            mBubblePopMenu!!.showAsDropDownAtLocation(
                mLayoutBoundsView,
                info.boundsInScreen.height(),
                info.boundsInScreen.centerX() - mBubblePopMenu!!.contentView.measuredWidth / 2,
                info.boundsInScreen.bottom - mLayoutBoundsView!!.statusBarHeight
            )
        }
        mLayoutBoundsView!!.boundsPaint.strokeWidth = 2f
        mLayoutBoundsView!!.setRootNode(mRootNode)
        if (mSelectedNode != null) mLayoutBoundsView!!.setSelectedNode(mSelectedNode)
    }

    private fun showNodeInfo() {
        ensureDialog()
        mNodeInfoView!!.setNodeInfo(mSelectedNode!!)
        mNodeInfoDialog!!.show()
    }

    private fun ensureOperationPopMenu() {
        if (mBubblePopMenu != null) return
        mBubblePopMenu = BubblePopupMenu(
            mContext, listOf(
                mContext!!.getString(R.string.text_show_widget_infomation),
                mContext!!.getString(R.string.text_show_layout_hierarchy),
                mContext!!.getString(R.string.text_generate_code),
                mContext!!.getString(R.string.text_exit_floating_window)
            )
        )
        mBubblePopMenu!!.setOnItemClickListener { _, position ->
            mBubblePopMenu!!.dismiss()
            when (position) {
                0 -> {
                    showNodeInfo()
                }
                1 -> {
                    showLayoutHierarchy()
                }
                2 -> {
                    generateCode()
                }
                3 -> {
                    close()
                }
            }
        }
        mBubblePopMenu!!.width = ViewGroup.LayoutParams.WRAP_CONTENT
        mBubblePopMenu!!.height = ViewGroup.LayoutParams.WRAP_CONTENT
    }

    private fun generateCode() {
        DialogUtils.showDialog(
            CodeGenerateDialog(mContext!!, mRootNode, mSelectedNode)
                .build()
        )
    }

    private fun showLayoutHierarchy() {
        close()
        val window = LayoutHierarchyFloatyWindow(mRootNode!!)
        window.setSelectedNode(mSelectedNode)
        FloatyService.addWindow(window)
    }

    private fun ensureDialog() {
        if (mNodeInfoDialog == null) {
            mNodeInfoView = NodeInfoView(mContext!!)
            mNodeInfoDialog = MaterialDialog.Builder(mContext!!)
                .customView(mNodeInfoView!!, false)
                .theme(Theme.LIGHT)
                .build()
            mNodeInfoDialog?.window?.setType(FloatyWindowManger.getWindowType())
        }
    }

    fun setSelectedNode(selectedNode: NodeInfo?) {
        mSelectedNode = selectedNode
    }

    companion object {
        fun capture(inspector: LayoutInspector, context: Context?) {
            val listener: CaptureAvailableListener = object : CaptureAvailableListener {
                override fun onCaptureAvailable(capture: NodeInfo?) {
                    inspector.removeCaptureAvailableListener(this)
                    val window = LayoutBoundsFloatyWindow(capture)
                    FloatyWindowManger.addWindow(context, window)
                }
            }
            inspector.addCaptureAvailableListener(listener)
            if (!inspector.captureCurrentWindow()) {
                inspector.removeCaptureAvailableListener(listener)
            }
        }
    }
}