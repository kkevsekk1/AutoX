package org.autojs.autojs.ui.filechooser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.SimpleItemAnimator
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnCheckedChanged
import butterknife.OnClick
import com.stardust.pio.PFile
import com.stardust.pio.PFiles
import org.autojs.autoxjs.R
import org.autojs.autojs.model.explorer.ExplorerItem
import org.autojs.autojs.model.explorer.ExplorerPage
import org.autojs.autojs.model.script.ScriptFile
import org.autojs.autojs.ui.explorer.ExplorerViewHelper
import org.autojs.autojs.ui.explorer.ExplorerViewKt
import org.autojs.autojs.ui.widget.BindableViewHolder
import org.autojs.autojs.ui.widget.CheckBoxCompat

/**
 * Created by Stardust on 2017/10/19.
 */
class FileChooseListView : ExplorerViewKt {
    private var mMaxChoice = 1
    private val mSelectedFiles = LinkedHashMap<PFile, Int>()
    private var mCanChooseDir = false

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    fun setMaxChoice(maxChoice: Int) {
        mMaxChoice = maxChoice
    }

    fun setCanChooseDir(canChooseDir: Boolean) {
        mCanChooseDir = canChooseDir
    }

    val selectedFiles: List<PFile>
        get() {
            val list = ArrayList<PFile>(mSelectedFiles.size)
            for ((key) in mSelectedFiles) {
                list.add(key)
            }
            return list
        }

    private fun init() {
        (explorerItemListView!!.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations =
            false
    }

    override fun onCreateViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        viewType: Int
    ): BindableViewHolder<Any> {
        return when (viewType) {
            VIEW_TYPE_ITEM -> {
                ExplorerItemViewHolder(
                    inflater.inflate(
                        R.layout.file_choose_list_file,
                        parent,
                        false
                    )
                )
            }
            VIEW_TYPE_PAGE -> {
                ExplorerPageViewHolder(
                    inflater.inflate(
                        R.layout.file_choose_list_directory,
                        parent,
                        false
                    )
                )
            }
            else -> {
                super.onCreateViewHolder(inflater, parent, viewType)
            }
        }
    }

    private fun check(file: ScriptFile, position: Int) {
        if (mSelectedFiles.size == mMaxChoice) {
            val (key, positionOfItemToUncheck) = mSelectedFiles.entries.iterator().next()
            mSelectedFiles.remove(key)
            explorerItemListView!!.adapter!!.notifyItemChanged(positionOfItemToUncheck)
        }
        mSelectedFiles[file] = position
    }

    @SuppressLint("NonConstantResourceId")
    internal inner class ExplorerItemViewHolder(itemView: View?) :
        BindableViewHolder<Any>(itemView) {

        @JvmField
        @BindView(R.id.name)
        var mName: TextView? = null

        @JvmField
        @BindView(R.id.first_char)
        var mFirstChar: TextView? = null

        @JvmField
        @BindView(R.id.checkbox)
        var mCheckBox: CheckBoxCompat? = null

        @JvmField
        @BindView(R.id.desc)
        var mDesc: TextView? = null
        var mFirstCharBackground: GradientDrawable
        private var mExplorerItem: ExplorerItem? = null
        override fun bind(item: Any, position: Int) {
            if (item !is ExplorerItem) return
            mExplorerItem = item
            mName!!.text = ExplorerViewHelper.getDisplayName(item)
            mDesc!!.text = PFiles.getHumanReadableSize(item.size)
            mFirstChar!!.text = ExplorerViewHelper.getIconText(item)
            mFirstCharBackground.setColor(ExplorerViewHelper.getIconColor(item))
            mCheckBox!!.setChecked(
                mSelectedFiles.containsKey(mExplorerItem!!.toScriptFile()),
                false
            )
        }

        @OnClick(R.id.item)
        fun onItemClick() {
            mCheckBox!!.toggle()
        }

        @OnCheckedChanged(R.id.checkbox)
        fun onCheckedChanged() {
            if (mCheckBox!!.isChecked) {
                check(mExplorerItem!!.toScriptFile(), absoluteAdapterPosition)
            } else {
                mSelectedFiles.remove(mExplorerItem!!.toScriptFile())
            }
        }

        init {
            ButterKnife.bind(this, itemView!!)
            mFirstCharBackground = mFirstChar!!.background as GradientDrawable
        }
    }

    @SuppressLint("NonConstantResourceId")
    internal inner class ExplorerPageViewHolder(itemView: View?) :
        BindableViewHolder<Any>(itemView) {

        @JvmField
        @BindView(R.id.name)
        var mName: TextView? = null

        @JvmField
        @BindView(R.id.checkbox)
        var mCheckBox: CheckBoxCompat? = null

        @JvmField
        @BindView(R.id.icon)
        var mIcon: ImageView? = null
        private var mExplorerPage: ExplorerPage? = null
        override fun bind(data0: Any, position: Int) {
            if (data0 !is ExplorerPage) return
            mExplorerPage = data0
            mName!!.text = ExplorerViewHelper.getDisplayName(data0)
            mIcon!!.setImageResource(ExplorerViewHelper.getIcon(data0))
            if (mCanChooseDir) {
                mCheckBox!!.setChecked(mSelectedFiles.containsKey(data0.toScriptFile()), false)
            }
        }

        @OnClick(R.id.item)
        fun onItemClick() {
            enterDirectChildPage(mExplorerPage)
        }

        @OnCheckedChanged(R.id.checkbox)
        fun onCheckedChanged() {
            if (mCheckBox!!.isChecked) {
                check(mExplorerPage!!.toScriptFile(), absoluteAdapterPosition)
            } else {
                mSelectedFiles.remove(mExplorerPage!!.toScriptFile())
            }
        }

        init {
            ButterKnife.bind(this, itemView!!)
            mCheckBox!!.visibility = if (mCanChooseDir) VISIBLE else GONE
        }
    }
}