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
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.bignerdranch.expandablerecyclerview.ChildViewHolder
import com.bignerdranch.expandablerecyclerview.ExpandableRecyclerAdapter
import com.bignerdranch.expandablerecyclerview.ParentViewHolder
import com.stardust.autojs.execution.ScriptExecution
import com.stardust.autojs.execution.ScriptExecutionListener
import com.stardust.autojs.execution.SimpleScriptExecutionListener
import com.stardust.autojs.script.AutoFileSource
import com.stardust.autojs.servicecomponents.TaskInfo
import com.stardust.autojs.workground.WrapContentLinearLayoutManager
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
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
import org.autojs.autojs.ui.timing.TimedTaskSettingActivity_
import org.autojs.autoxjs.R

/**
 * Created by Stardust on 2017/3/24.
 */
class TaskListRecyclerView : ThemeColorRecyclerView {
    private val mTaskGroups: MutableList<TaskGroup> = ArrayList()
    private var mRunningTaskGroup: RunningTaskGroup? = null
    private var mPendingTaskGroup: PendingTaskGroup? = null
    private var mAdapter: Adapter? = null
    private var mTimedTaskChangeDisposable: Disposable? = null
    private var mIntentTaskChangeDisposable: Disposable? = null
    private val mScriptExecutionListener: ScriptExecutionListener =
        object : SimpleScriptExecutionListener() {
            override fun onStart(execution: ScriptExecution) {
                post { mAdapter!!.notifyChildInserted(0, mRunningTaskGroup!!.addTask(execution)) }
            }

            override fun onSuccess(execution: ScriptExecution, result: Any) {
                onFinish(execution)
            }

            override fun onException(execution: ScriptExecution, e: Throwable) {
                onFinish(execution)
            }

            private fun onFinish(execution: ScriptExecution) {
                post {
                    val i = mRunningTaskGroup!!.removeTask(execution)
                    if (i >= 0) {
                        mAdapter!!.notifyChildRemoved(0, i)
                    } else {
                        refresh()
                    }
                }
            }
        }

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context, attrs, defStyle
    ) {
        init()
    }

    private fun init() {
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
        mRunningTaskGroup = RunningTaskGroup(context)
        mTaskGroups.add(mRunningTaskGroup!!)
        mPendingTaskGroup = PendingTaskGroup(context)
        mTaskGroups.add(mPendingTaskGroup!!)
        mAdapter = Adapter(mTaskGroups)
        setAdapter(mAdapter)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun refresh() {
        GlobalScope.launch {
            for (group in mTaskGroups) {
                group.refresh()
            }
            mAdapter = Adapter(mTaskGroups)
            withContext(Dispatchers.Main){
                setAdapter(mAdapter)
            }
        }
        //notifyDataSetChanged not working...
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
//        AutoJs.getInstance().scriptEngineService.registerGlobalScriptExecutionListener(
//            mScriptExecutionListener
//        )
        mTimedTaskChangeDisposable = timeTaskChanges
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(Consumer { taskChange: ModelChange<TimedTask> -> onTaskChange(taskChange) })
        mIntentTaskChangeDisposable = intentTaskChanges
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(Consumer { taskChange: ModelChange<IntentTask> -> onTaskChange(taskChange) })
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            refresh()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
//        AutoJs.getInstance().scriptEngineService.unregisterGlobalScriptExecutionListener(
//            mScriptExecutionListener
//        )
        mTimedTaskChangeDisposable!!.dispose()
        mIntentTaskChangeDisposable!!.dispose()
    }

    fun onTaskChange(taskChange: ModelChange<*>) {
        if (taskChange.action == ModelChange.INSERT) {
            mAdapter!!.notifyChildInserted(1, mPendingTaskGroup!!.addTask(taskChange.data))
        } else if (taskChange.action == ModelChange.DELETE) {
            val i = mPendingTaskGroup!!.removeTask(taskChange.data)
            if (i >= 0) {
                mAdapter!!.notifyChildRemoved(1, i)
            } else {
                Log.w(LOG_TAG, "data inconsistent on change: $taskChange")
                refresh()
            }
        } else if (taskChange.action == ModelChange.UPDATE) {
            val i = mPendingTaskGroup!!.updateTask(taskChange.data)
            if (i >= 0) {
                mAdapter!!.notifyChildChanged(1, i)
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
        @JvmField
        @BindView(R.id.first_char)
        var mFirstChar: TextView? = null

        @JvmField
        @BindView(R.id.name)
        var mName: TextView? = null

        @JvmField
        @BindView(R.id.desc)
        var mDesc: TextView? = null
        private var mTask: TaskInfo? = null
        private val mFirstCharBackground: GradientDrawable

        init {
            itemView.setOnClickListener { view: View? -> onItemClick(view) }
            ButterKnife.bind(this, itemView)
            mFirstCharBackground = mFirstChar!!.background as GradientDrawable
        }

        fun bind(task: TaskInfo) {
            mTask = task
            mName!!.text = task.name
            mDesc!!.text = task.desc
            if (AutoFileSource.ENGINE == mTask!!.engineName) {
                mFirstChar!!.text = "R"
                mFirstCharBackground.setColor(resources.getColor(R.color.color_r))
            } else {
                mFirstChar!!.text = "J"
                mFirstCharBackground.setColor(resources.getColor(R.color.color_j))
            }
        }

        @OnClick(R.id.stop)
        fun stop() {
            if (mTask != null) {
                //mTask!!.cancel()
            }
        }

        fun onItemClick(view: View?) {
            if (mTask is PendingTask) {
                val task = mTask as PendingTask
                val extra =
                    if (task.timedTask == null) TimedTaskSettingActivity.EXTRA_INTENT_TASK_ID else TimedTaskSettingActivity.EXTRA_TASK_ID
                TimedTaskSettingActivity_.intent(context)
                    .extra(extra, task.getId())
                    .start()
            }
        }
    }

    private inner class TaskGroupViewHolder internal constructor(itemView: View) :
        ParentViewHolder<TaskGroup?, TaskInfo>(itemView) {
        var title: TextView
        var icon: ImageView

        init {
            title = itemView.findViewById(R.id.title)
            icon = itemView.findViewById(R.id.icon)
            itemView.setOnClickListener { view: View? ->
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