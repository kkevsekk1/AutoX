package org.autojs.autojs.ui.main.task

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ThemeColorRecyclerView
import com.bignerdranch.expandablerecyclerview.ChildViewHolder
import com.bignerdranch.expandablerecyclerview.ExpandableRecyclerAdapter
import com.bignerdranch.expandablerecyclerview.ParentViewHolder
import com.stardust.autojs.script.AutoFileSource
import com.stardust.autojs.servicecomponents.BinderScriptListener
import com.stardust.autojs.servicecomponents.EngineController
import com.stardust.autojs.servicecomponents.TaskInfo
import com.stardust.autojs.workground.WrapContentLinearLayoutManager
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.autojs.autojs.storage.database.ModelChange
import org.autojs.autojs.timing.IntentTask
import org.autojs.autojs.timing.TimedTask
import org.autojs.autojs.timing.TimedTaskManager.intentTaskChanges
import org.autojs.autojs.timing.TimedTaskManager.timeTaskChanges
import org.autojs.autojs.ui.main.task.Task.PendingTask
import org.autojs.autojs.ui.main.task.TaskGroup.PendingTaskGroup
import org.autojs.autojs.ui.main.task.TaskGroup.RunningTaskGroup
import org.autojs.autojs.ui.timing.TimedTaskSettingActivity
import org.autojs.autoxjs.R

/**
 * Created by Stardust on 2017/3/24.
 */
class TaskListRecyclerView : ThemeColorRecyclerView {
    private val mRunningTaskGroup: RunningTaskGroup = RunningTaskGroup(context)
    private val mPendingTaskGroup: PendingTaskGroup = PendingTaskGroup(context)
    private val mTaskGroups: MutableList<TaskGroup> =
        mutableListOf(mRunningTaskGroup, mPendingTaskGroup)
    private var mAdapter: Adapter = Adapter(mTaskGroups)
    private var mTimedTaskChangeDisposable: Disposable? = null
    private var mIntentTaskChangeDisposable: Disposable? = null
    private val mScriptExecutionListener: BinderScriptListener =
        object : BinderScriptListener {
            override fun onStart(taskInfo: TaskInfo) {
                post { mAdapter.notifyChildInserted(0, mRunningTaskGroup.addTask(taskInfo)) }
            }

            override fun onSuccess(taskInfo: TaskInfo) = onFinish(taskInfo)
            override fun onException(taskInfo: TaskInfo, e: Throwable) = onFinish(taskInfo)


            private fun onFinish(task: TaskInfo) {
                post {
                    val i = mRunningTaskGroup.removeTask(task)
                    if (i >= 0) {
                        mAdapter.notifyChildRemoved(0, i)
                    } else {
                        refresh()
                    }
                }
            }
        }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) :
            super(context, attrs, defStyle)

    init {
        setLayoutManager(WrapContentLinearLayoutManager(context))
        addItemDecoration(
            HorizontalDividerItemDecoration.Builder(context)
                .color(ContextCompat.getColor(context, R.color.divider))
                .size(2)
                .marginResId(
                    R.dimen.script_and_folder_list_divider_left_margin,
                    R.dimen.script_and_folder_list_divider_right_margin
                )
                .showLastDivider()
                .build()
        )
        setAdapter(mAdapter)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun refresh() {
        GlobalScope.launch {
            for (group in mTaskGroups) {
                group.refresh()
            }
            mAdapter = Adapter(mTaskGroups)
            withContext(Dispatchers.Main) {
                setAdapter(mAdapter)
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        EngineController.registerGlobalScriptExecutionListener(mScriptExecutionListener)
        mTimedTaskChangeDisposable = timeTaskChanges
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { taskChange: ModelChange<TimedTask> -> onTaskChange(taskChange) }
        mIntentTaskChangeDisposable = intentTaskChanges
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { taskChange: ModelChange<IntentTask> -> onTaskChange(taskChange) }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            refresh()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        EngineController.unregisterGlobalScriptExecutionListener(mScriptExecutionListener)
        mTimedTaskChangeDisposable!!.dispose()
        mIntentTaskChangeDisposable!!.dispose()
    }

    fun onTaskChange(taskChange: ModelChange<*>) {
        if (taskChange.action == ModelChange.INSERT) {
            mAdapter.notifyChildInserted(1, mPendingTaskGroup.addTask(taskChange.data))
        } else if (taskChange.action == ModelChange.DELETE) {
            val i = mPendingTaskGroup.removeTask(taskChange.data)
            if (i >= 0) {
                mAdapter.notifyChildRemoved(1, i)
            } else {
                Log.w(LOG_TAG, "data inconsistent on change: $taskChange")
                refresh()
            }
        } else if (taskChange.action == ModelChange.UPDATE) {
            val i = mPendingTaskGroup.updateTask(taskChange.data)
            if (i >= 0) {
                mAdapter.notifyChildChanged(1, i)
            } else {
                refresh()
            }
        }
    }

    private inner class Adapter(parentList: List<TaskGroup?>) :
        ExpandableRecyclerAdapter<TaskGroup, TaskInfo, TaskGroupViewHolder, TaskViewHolder>(
            parentList
        ) {
        override fun onCreateParentViewHolder(
            parentViewGroup: ViewGroup,
            viewType: Int
        ): TaskGroupViewHolder {
            return TaskGroupViewHolder(
                LayoutInflater.from(parentViewGroup.context)
                    .inflate(R.layout.dialog_code_generate_option_group, parentViewGroup, false)
            )
        }

        override fun onCreateChildViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            return TaskViewHolder(
                LayoutInflater.from(context)
                    .inflate(R.layout.task_list_recycler_view_item, parent, false)
            )
        }

        override fun onBindParentViewHolder(
            viewHolder: TaskGroupViewHolder,
            parentPosition: Int,
            taskGroup: TaskGroup
        ) {
            viewHolder.title.text = taskGroup.title
        }

        override fun onBindChildViewHolder(
            viewHolder: TaskViewHolder,
            parentPosition: Int,
            childPosition: Int,
            task: TaskInfo
        ) {
            viewHolder.bind(task)
        }
    }

    internal inner class TaskViewHolder(itemView: View) : ChildViewHolder<Task?>(itemView) {
        private val mFirstChar: TextView = itemView.findViewById(R.id.first_char)
        private val mName: TextView = itemView.findViewById(R.id.name)
        private val mDesc: TextView = itemView.findViewById(R.id.desc)
        private var mTask: TaskInfo? = null
        private val mFirstCharBackground: GradientDrawable =
            mFirstChar.background as GradientDrawable

        init {
            itemView.setOnClickListener { view: View? -> onItemClick(view) }
            itemView.findViewById<View>(R.id.stop).setOnClickListener { stop() }
        }

        fun bind(task: TaskInfo) {
            mTask = task
            mName.text = task.name
            mDesc.text = task.desc
            if (AutoFileSource.ENGINE == mTask!!.engineName) {
                mFirstChar.text = "R"
                mFirstCharBackground.setColor(context.getColor(R.color.color_r))
            } else {
                mFirstChar.text = "J"
                mFirstCharBackground.setColor(context.getColor(R.color.color_j))
            }
        }

        fun stop() {
            val task = mTask ?: return
            if (task is PendingTask) {
                task.cancel()
            } else
                EngineController.stopScript(task.id)
        }

        private fun onItemClick(view: View?) {
            if (mTask is PendingTask) {
                TimedTaskSettingActivity.reviseTimeTask(context, mTask as PendingTask)
            }
        }
    }

    private inner class TaskGroupViewHolder(itemView: View) :
        ParentViewHolder<TaskGroup?, TaskInfo>(itemView) {
        val title: TextView = itemView.findViewById(R.id.title)
        val icon: ImageView = itemView.findViewById(R.id.icon)

        init {
            itemView.setOnClickListener {
                if (isExpanded) {
                    collapseView()
                } else {
                    expandView()
                }
            }
        }

        override fun onExpansionToggled(expanded: Boolean) {
            icon.rotation = (if (expanded) -90 else 0).toFloat()
        }
    }

    companion object {
        private const val LOG_TAG = "TaskListRecyclerView"
    }
}