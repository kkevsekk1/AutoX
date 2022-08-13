package org.autojs.autojs.ui.floating.layoutinspector

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import com.stardust.util.ViewUtil
import com.stardust.view.accessibility.NodeInfo
import org.autojs.autojs.ui.widget.LevelBeamView
import org.autojs.autoxjs.R
import pl.openrnd.multilevellistview.*
import java.util.*

/**
 * Created by Stardust on 2017/3/10.
 */
open class LayoutHierarchyView : MultiLevelListView {
    interface OnItemLongClickListener {
        fun onItemLongClick(view: View, nodeInfo: NodeInfo)
    }

    private var mAdapter: Adapter? = null
    private var mOnItemLongClickListener: ((view: View, nodeInfo: NodeInfo) -> Unit)? = null
    private var onItemTouchListener: ((view: View, event: MotionEvent) -> Boolean)? = null
    private val mOnItemLongClickListenerProxy =
        AdapterView.OnItemLongClickListener { parent, view, position, id ->
            (view.tag as ViewHolder).nodeInfo?.let {
                mOnItemLongClickListener?.invoke(view, it)
                return@OnItemLongClickListener true
            }
            false
        }


    var boundsPaint: Paint? = null
        private set
    private var mBoundsInScreen: IntArray? = null
    var mStatusBarHeight = 0
    var mClickedNodeInfo: NodeInfo? = null
    private var mClickedView: View? = null
    private var mOriginalBackground: Drawable? = null
    var mShowClickedNodeBounds = false
    private var mClickedColor = -0x664d4c49
    private var mRootNode: NodeInfo? = null
    private val mInitiallyExpandedNodes: MutableSet<NodeInfo?> = HashSet()

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    fun setShowClickedNodeBounds(showClickedNodeBounds: Boolean) {
        mShowClickedNodeBounds = showClickedNodeBounds
    }

    fun setClickedColor(clickedColor: Int) {
        mClickedColor = clickedColor
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun init() {
        mAdapter = Adapter()
        setAdapter(mAdapter)
        nestType = NestType.MULTIPLE
        (getChildAt(0) as ListView).apply {
            setOnTouchListener { view, motionEvent ->
                return@setOnTouchListener onItemTouchListener?.invoke(view, motionEvent) ?: false
            }
            onItemLongClickListener = mOnItemLongClickListenerProxy
        }
        setWillNotDraw(false)
        initPaint()
        setOnItemClickListener(object : OnItemClickListener {
            override fun onItemClicked(
                parent: MultiLevelListView,
                view: View,
                item: Any,
                itemInfo: ItemInfo
            ) {
                setClickedItem(view, item as NodeInfo)
            }

            override fun onGroupItemClicked(
                parent: MultiLevelListView,
                view: View,
                item: Any,
                itemInfo: ItemInfo
            ) {
                setClickedItem(view, item as NodeInfo)
            }
        })
    }

    private fun setClickedItem(view: View, item: NodeInfo) {
        mClickedNodeInfo = item
        if (mClickedView == null) {
            mOriginalBackground = view.background
        } else {
            mClickedView!!.background = mOriginalBackground
        }
        view.setBackgroundColor(mClickedColor)
        mClickedView = view
        invalidate()
    }

    private fun initPaint() {
        boundsPaint = Paint()
        boundsPaint!!.color = Color.DKGRAY
        boundsPaint!!.style = Paint.Style.STROKE
        boundsPaint!!.isAntiAlias = true
        boundsPaint!!.strokeWidth = 3f
        mStatusBarHeight = ViewUtil.getStatusBarHeight(context)
    }

    fun setRootNode(rootNodeInfo: NodeInfo) {
        mRootNode = rootNodeInfo
        mAdapter!!.setDataItems(listOf(rootNodeInfo))
        mClickedNodeInfo = null
        mInitiallyExpandedNodes.clear()
    }

    fun setOnItemTouchListener(listener: ((view: View, event: MotionEvent) -> Boolean)) {
        onItemTouchListener = listener
    }

    fun setOnItemLongClickListener(onNodeInfoSelectListener: (view: View, nodeInfo: NodeInfo) -> Unit) {
        mOnItemLongClickListener = onNodeInfoSelectListener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mBoundsInScreen == null) {
            mBoundsInScreen = IntArray(4)
            getLocationOnScreen(mBoundsInScreen)
            mStatusBarHeight = mBoundsInScreen!![1]
        }
        if (mShowClickedNodeBounds && mClickedNodeInfo != null) {
            LayoutBoundsView.drawRect(
                canvas,
                mClickedNodeInfo!!.boundsInScreen,
                mStatusBarHeight,
                boundsPaint
            )
        }
    }

    fun setSelectedNode(selectedNode: NodeInfo) {
        mInitiallyExpandedNodes.clear()
        val parents = Stack<NodeInfo?>()
        searchNodeParents(selectedNode, mRootNode, parents)
        mClickedNodeInfo = parents.peek()
        mInitiallyExpandedNodes.addAll(parents)
        mAdapter!!.reloadData()
    }

    private fun searchNodeParents(
        nodeInfo: NodeInfo,
        rootNode: NodeInfo?,
        stack: Stack<NodeInfo?>
    ): Boolean {
        stack.push(rootNode)
        if (nodeInfo == rootNode) {
            return true
        }
        var found = false
        for (child in rootNode!!.getChildren()) {
            if (searchNodeParents(nodeInfo, child, stack)) {
                found = true
                break
            }
        }
        if (!found) {
            stack.pop()
        }
        return found
    }

    private inner class ViewHolder internal constructor(view: View) {
        var nameView: TextView
        var infoView: TextView
        var arrowView: ImageView
        var levelBeamView: LevelBeamView
        var nodeInfo: NodeInfo? = null

        init {
            infoView = view.findViewById<View>(R.id.dataItemInfo) as TextView
            nameView = view.findViewById<View>(R.id.dataItemName) as TextView
            arrowView = view.findViewById<View>(R.id.dataItemArrow) as ImageView
            levelBeamView = view.findViewById<View>(R.id.dataItemLevelBeam) as LevelBeamView
        }
    }

    private inner class Adapter : MultiLevelListAdapter() {
        override fun getSubObjects(`object`: Any): List<*> {
            return (`object` as NodeInfo).getChildren()
        }

        override fun isExpandable(`object`: Any): Boolean {
            return !(`object` as NodeInfo).getChildren().isEmpty()
        }

        override fun isInitiallyExpanded(`object`: Any): Boolean {
            return mInitiallyExpandedNodes.contains(`object` as NodeInfo)
        }

        public override fun getViewForObject(
            `object`: Any,
            convertView: View?,
            itemInfo: ItemInfo
        ): View {
            val nodeInfo = `object` as NodeInfo
            val viewHolder: ViewHolder
            val convertView1 = if (convertView != null) {
                viewHolder = convertView.tag as ViewHolder
                convertView
            } else {
                val convertView2 =
                    LayoutInflater.from(context).inflate(R.layout.layout_hierarchy_view_item, null)
                viewHolder = ViewHolder(convertView2)
                convertView2.tag = viewHolder
                convertView2
            }

            viewHolder.nameView.text = simplifyClassName(nodeInfo.className)
            viewHolder.nodeInfo = nodeInfo
            if (viewHolder.infoView.visibility == VISIBLE) viewHolder.infoView.text =
                getItemInfoDsc(itemInfo)
            if (itemInfo.isExpandable && !isAlwaysExpanded) {
                viewHolder.arrowView.visibility = VISIBLE
                viewHolder.arrowView.setImageResource(if (itemInfo.isExpanded) R.drawable.arrow_up else R.drawable.arrow_down)
            } else {
                viewHolder.arrowView.visibility = GONE
            }
            viewHolder.levelBeamView.setLevel(itemInfo.level)
            if (nodeInfo == mClickedNodeInfo) {
                convertView1?.let { setClickedItem(it, nodeInfo) }
            }
            return convertView1!!
        }

        private fun simplifyClassName(className: CharSequence?): String? {
            if (className == null) return null
            var s = className.toString()
            if (s.startsWith("android.widget.")) {
                s = s.substring(15)
            }
            return s
        }

        private fun getItemInfoDsc(itemInfo: ItemInfo): String {
            val builder = StringBuilder()
            builder.append(
                String.format(
                    Locale.getDefault(), "level[%d], idx in level[%d/%d]",
                    itemInfo.level + 1,  /*Indexing starts from 0*/
                    itemInfo.idxInLevel + 1 /*Indexing starts from 0*/,
                    itemInfo.levelSize
                )
            )
            if (itemInfo.isExpandable) {
                builder.append(String.format(", expanded[%b]", itemInfo.isExpanded))
            }
            return builder.toString()
        }
    }
}