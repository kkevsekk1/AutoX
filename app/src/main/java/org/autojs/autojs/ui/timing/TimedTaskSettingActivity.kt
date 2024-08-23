package org.autojs.autojs.ui.timing

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.DatePicker
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import com.github.aakira.expandablelayout.ExpandableRelativeLayout
import com.stardust.autojs.execution.ExecutionConfig
import com.stardust.autojs.execution.ExecutionConfig.CREATOR.default
import com.stardust.util.BiMap
import com.stardust.util.BiMaps
import com.stardust.util.MapBuilder
import org.autojs.autojs.external.ScriptIntents
import org.autojs.autojs.external.receiver.DynamicBroadcastReceivers
import org.autojs.autojs.model.script.ScriptFile
import org.autojs.autojs.timing.IntentTask
import org.autojs.autojs.timing.TaskReceiver
import org.autojs.autojs.timing.TimedTask
import org.autojs.autojs.timing.TimedTask.Companion.dailyTask
import org.autojs.autojs.timing.TimedTask.Companion.disposableTask
import org.autojs.autojs.timing.TimedTask.Companion.getDayOfWeekTimeFlag
import org.autojs.autojs.timing.TimedTask.Companion.weeklyTask
import org.autojs.autojs.timing.TimedTaskManager.addTask
import org.autojs.autojs.timing.TimedTaskManager.getIntentTask
import org.autojs.autojs.timing.TimedTaskManager.getTimedTask
import org.autojs.autojs.timing.TimedTaskManager.removeTask
import org.autojs.autojs.timing.TimedTaskManager.updateTask
import org.autojs.autojs.ui.BaseActivity
import org.autojs.autojs.ui.main.task.Task
import org.autojs.autoxjs.R
import org.autojs.autoxjs.databinding.ActivityTimedTaskSettingBinding
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

/**
 * Created by Stardust on 2017/11/28.
 */
class TimedTaskSettingActivity : BaseActivity() {
    private lateinit var binding: ActivityTimedTaskSettingBinding
    private var mDisposableTaskRadio: RadioButton? = null
    private var mDailyTaskRadio: RadioButton? = null
    private var mRunOnBroadcastRadio: RadioButton? = null
    private var mOtherBroadcastAction: EditText? = null
    private var mDisposableTaskTime: TextView? = null
    private var mDisposableTaskDate: TextView? = null
    private var mDailyTaskTimePicker: TimePicker? = null
    private var mWeeklyTaskTimePicker: TimePicker? = null

    private val mDayOfWeekCheckBoxes: MutableList<CheckBox> = ArrayList()

    private var mScriptFile: ScriptFile? = null
    private var mTimedTask: TimedTask? = null
    private var mIntentTask: IntentTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimedTaskSettingBinding.inflate(
            layoutInflater
        )
        mDailyTaskRadio = binding.dailyTaskRadio
        mDisposableTaskRadio = binding.disposableTaskRadio
        mRunOnBroadcastRadio = binding.runOnOtherBroadcast
        mOtherBroadcastAction = binding.action
        mDisposableTaskTime = binding.disposableTaskTime
        mDisposableTaskDate = binding.disposableTaskDate
        mDailyTaskTimePicker = binding.dailyTaskTimePicker
        mWeeklyTaskTimePicker = binding.weeklyTaskTimePicker
        setContentView(binding.root)
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        if (taskId != -1L) {
            mTimedTask = getTimedTask(taskId)
            mScriptFile = ScriptFile(mTimedTask!!.scriptPath!!)
        } else {
            val intentTaskId = intent.getLongExtra(EXTRA_INTENT_TASK_ID, -1)
            if (intentTaskId != -1L) {
                mIntentTask = getIntentTask(intentTaskId)
                mScriptFile = ScriptFile(mIntentTask!!.scriptPath!!)
            } else {
                val path = intent.getStringExtra(ScriptIntents.EXTRA_KEY_PATH)
                if (path.isNullOrEmpty()) {
                    return finish()
                }
                mScriptFile = ScriptFile(path)
            }
        }
        setupViews()
    }

    private fun setupViews() {
        setToolbarAsBack(getString(R.string.text_timed_task))
        binding.toolbar.subtitle = mScriptFile!!.name
        mDailyTaskTimePicker!!.setIs24HourView(true)
        mWeeklyTaskTimePicker!!.setIs24HourView(true)
        findDayOfWeekCheckBoxes(binding.weeklyTaskContainer)
        setUpTaskSettings()
        binding.disposableTaskTimeContainer.setOnClickListener { showDisposableTaskTimePicker() }
        binding.disposableTaskDateContainer.setOnClickListener { showDisposableTaskDatePicker() }
        //        @CheckedChange({R.id.daily_task_radio, R.id.weekly_task_radio, R.id.disposable_task_radio, R.id.run_on_broadcast})
        val listener =
            View.OnClickListener { view: View -> onCheckedChanged(view as CompoundButton) }
        binding.dailyTaskRadio.setOnClickListener(listener)
        binding.weeklyTaskRadio.setOnClickListener(listener)
        binding.disposableTaskRadio.setOnClickListener(listener)
        binding.runOnBroadcast.setOnClickListener(listener)
    }

    private fun findDayOfWeekCheckBoxes(parent: ViewGroup) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is CheckBox) {
                mDayOfWeekCheckBoxes.add(child)
            } else if (child is ViewGroup) {
                findDayOfWeekCheckBoxes(child)
            }
            if (mDayOfWeekCheckBoxes.size >= 7) break
        }
    }

    private fun setUpTaskSettings() {
        mDisposableTaskDate!!.text =
            DATE_FORMATTER.print(LocalDate.now())
        mDisposableTaskTime!!.text =
            TIME_FORMATTER.print(LocalTime.now())
        if (mTimedTask != null) {
            setupTime()
            return
        }
        if (mIntentTask != null) {
            setupAction()
            return
        }
        mDailyTaskRadio!!.isChecked = true
    }

    private fun setupAction() {
        mRunOnBroadcastRadio!!.isChecked = true
        val buttonId = ACTIONS.getKey(mIntentTask!!.action)
        if (buttonId == null) {
            binding.runOnOtherBroadcast.isChecked = true
            mOtherBroadcastAction!!.setText(mIntentTask!!.action)
        } else {
            (findViewById<View>(buttonId) as RadioButton).isChecked = true
        }
    }

    private fun setupTime() {
        if (mTimedTask!!.isDisposable) {
            mDisposableTaskRadio!!.isChecked = true
            mDisposableTaskTime!!.text = TIME_FORMATTER.print(
                mTimedTask!!.millis
            )
            mDisposableTaskDate!!.text = DATE_FORMATTER.print(
                mTimedTask!!.millis
            )
            return
        }
        val time = LocalTime.fromMillisOfDay(mTimedTask!!.millis)
        mDailyTaskTimePicker!!.currentHour = time.hourOfDay
        mDailyTaskTimePicker!!.currentMinute = time.minuteOfHour
        mWeeklyTaskTimePicker!!.currentHour = time.hourOfDay
        mWeeklyTaskTimePicker!!.currentMinute = time.minuteOfHour
        if (mTimedTask!!.isDaily) {
            mDailyTaskRadio!!.isChecked = true
        } else {
            binding.weeklyTaskRadio.isChecked = true
            for (i in mDayOfWeekCheckBoxes.indices) {
                mDayOfWeekCheckBoxes[i].isChecked = mTimedTask!!.hasDayOfWeek(i + 1)
            }
        }
    }


    fun onCheckedChanged(button: CompoundButton) {
        val relativeLayout = findExpandableLayoutOf(button)
        if (button.isChecked) {
            relativeLayout.post { relativeLayout.expand() }
        } else {
            relativeLayout.collapse()
        }
    }

    private fun findExpandableLayoutOf(button: CompoundButton): ExpandableRelativeLayout {
        val parent = button.parent as ViewGroup
        for (i in 0 until parent.childCount) {
            if (parent.getChildAt(i) === button) {
                return (parent.getChildAt(i + 1) as ExpandableRelativeLayout)
            }
        }
        throw IllegalStateException("findExpandableLayout: button = " + button + ", parent = " + parent + ", childCount = " + parent.childCount)
    }

    fun showDisposableTaskTimePicker() {
        val time = TIME_FORMATTER.parseLocalTime(
            mDisposableTaskTime!!.text.toString()
        )
        TimePickerDialog(this, { view: TimePicker?, hourOfDay: Int, minute: Int ->
            mDisposableTaskTime!!.text = TIME_FORMATTER.print(
                LocalTime(hourOfDay, minute)
            )
        }, time.hourOfDay, time.minuteOfHour, true)
            .show()
    }


    fun showDisposableTaskDatePicker() {
        val date = DATE_FORMATTER.parseLocalDate(
            mDisposableTaskDate!!.text.toString()
        )
        DatePickerDialog(
            this, { view: DatePicker?, year: Int, month: Int, dayOfMonth: Int ->
                mDisposableTaskDate!!.text = DATE_FORMATTER.print(
                    LocalDate(year, month + 1, dayOfMonth)
                )
            },
            date.year, date.monthOfYear - 1, date.dayOfMonth
        ).show()
    }

    private fun createTimedTask(): TimedTask? {
        return if (mDisposableTaskRadio!!.isChecked) {
            createDisposableTask()
        } else if (mDailyTaskRadio!!.isChecked) {
            createDailyTask()
        } else {
            createWeeklyTask()
        }
    }

    private fun createWeeklyTask(): TimedTask? {
        var timeFlag: Long = 0
        for (i in mDayOfWeekCheckBoxes.indices) {
            if (mDayOfWeekCheckBoxes[i].isChecked) {
                timeFlag = timeFlag or getDayOfWeekTimeFlag(i + 1)
            }
        }
        if (timeFlag == 0L) {
            Toast.makeText(
                this,
                R.string.text_weekly_task_should_check_day_of_week,
                Toast.LENGTH_SHORT
            ).show()
            return null
        }
        val time =
            LocalTime(mWeeklyTaskTimePicker!!.currentHour, mWeeklyTaskTimePicker!!.currentMinute)
        return weeklyTask(time, timeFlag, mScriptFile!!.path, default)
    }

    private fun createDailyTask(): TimedTask {
        val time =
            LocalTime(mDailyTaskTimePicker!!.currentHour, mDailyTaskTimePicker!!.currentMinute)
        return dailyTask(time, mScriptFile!!.path, ExecutionConfig())
    }

    private fun createDisposableTask(): TimedTask? {
        val time = TIME_FORMATTER.parseLocalTime(
            mDisposableTaskTime!!.text.toString()
        )
        val date = DATE_FORMATTER.parseLocalDate(
            mDisposableTaskDate!!.text.toString()
        )
        val dateTime = LocalDateTime(
            date.year, date.monthOfYear, date.dayOfMonth,
            time.hourOfDay, time.minuteOfHour
        )
        if (dateTime.isBefore(LocalDateTime.now())) {
            Toast.makeText(this, R.string.text_disposable_task_time_before_now, Toast.LENGTH_SHORT)
                .show()
            return null
        }
        return disposableTask(dateTime, mScriptFile!!.path, default)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_timed_task_setting, menu)
        return true
    }

    @SuppressLint("BatteryLife")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_done) {
            if (!(getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(
                    packageName
                )
            ) {
                try {
                    startActivityForResult(
                        Intent().setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            .setData(Uri.parse("package:$packageName")), REQUEST_CODE_IGNORE_BATTERY
                    )
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                    createOrUpdateTask()
                }
            } else {
                createOrUpdateTask()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_IGNORE_BATTERY) {
            Log.d(LOG_TAG, "result code = $requestCode")
            createOrUpdateTask()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun createOrUpdateTask() {
        if (mRunOnBroadcastRadio!!.isChecked) {
            createOrUpdateIntentTask()
            return
        }
        val task = createTimedTask() ?: return
        if (mTimedTask == null) {
            addTask(task)
            if (mIntentTask != null) {
                removeTask(mIntentTask!!)
            }
            Toast.makeText(this, R.string.text_already_create, Toast.LENGTH_SHORT).show()
        } else {
            task.id = mTimedTask!!.id
            updateTask(task)
        }
        finish()
    }


    private fun createOrUpdateIntentTask() {
        val buttonId = binding!!.broadcastGroup.checkedRadioButtonId
        if (buttonId == -1) {
            Toast.makeText(this, R.string.error_empty_selection, Toast.LENGTH_SHORT).show()
            return
        }
        val action: String?
        if (buttonId == R.id.run_on_other_broadcast) {
            action = mOtherBroadcastAction!!.text.toString()
            if (action.isEmpty()) {
                mOtherBroadcastAction!!.error = getString(R.string.text_should_not_be_empty)
                return
            }
        } else {
            action = ACTIONS[buttonId]
        }
        val task = IntentTask()
        task.action = action
        task.scriptPath = mScriptFile!!.path
        task.isLocal = action == DynamicBroadcastReceivers.ACTION_STARTUP
        if (mIntentTask != null) {
            task.id = mIntentTask!!.id
            updateTask(task)
            Toast.makeText(this, R.string.text_already_create, Toast.LENGTH_SHORT).show()
        } else {
            addTask(task)
            if (mTimedTask != null) {
                removeTask(mTimedTask!!)
            }
        }

        finish()
    }

    companion object {
        const val EXTRA_INTENT_TASK_ID: String = "intent_task_id"
        const val EXTRA_TASK_ID: String = TaskReceiver.EXTRA_TASK_ID

        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormat.forPattern("HH:mm")
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
        private const val REQUEST_CODE_IGNORE_BATTERY = 27101

        private const val LOG_TAG = "TimedTaskSettings"

        fun reviseTimeTask(context: Context,task: Task.PendingTask){
            val intent = Intent(context, TimedTaskSettingActivity::class.java)
            task.timedTask?.let {
                intent.putExtra(EXTRA_TASK_ID, it.id)
                context.startActivity(intent)
                return
            }
            task.mIntentTask?.let {
                intent.putExtra(EXTRA_INTENT_TASK_ID, it.id)
                context.startActivity(intent)
                return
            }
        }

        val ACTION_DESC_MAP: Map<String, Int> = MapBuilder<String, Int>()
            .put(DynamicBroadcastReceivers.ACTION_STARTUP, R.string.text_run_on_startup)
            .put(Intent.ACTION_BOOT_COMPLETED, R.string.text_run_on_boot)
            .put(Intent.ACTION_SCREEN_OFF, R.string.text_run_on_screen_off)
            .put(Intent.ACTION_SCREEN_ON, R.string.text_run_on_screen_on)
            .put(Intent.ACTION_USER_PRESENT, R.string.text_run_on_screen_unlock)
            .put(Intent.ACTION_BATTERY_CHANGED, R.string.text_run_on_battery_change)
            .put(Intent.ACTION_POWER_CONNECTED, R.string.text_run_on_power_connect)
            .put(Intent.ACTION_POWER_DISCONNECTED, R.string.text_run_on_power_disconnect)
            .put(ConnectivityManager.CONNECTIVITY_ACTION, R.string.text_run_on_conn_change)
            .put(Intent.ACTION_PACKAGE_ADDED, R.string.text_run_on_package_install)
            .put(Intent.ACTION_PACKAGE_REMOVED, R.string.text_run_on_package_uninstall)
            .put(Intent.ACTION_PACKAGE_REPLACED, R.string.text_run_on_package_update)
            .put(Intent.ACTION_HEADSET_PLUG, R.string.text_run_on_headset_plug)
            .put(Intent.ACTION_CONFIGURATION_CHANGED, R.string.text_run_on_config_change)
            .put(Intent.ACTION_TIME_TICK, R.string.text_run_on_time_tick)
            .build()

        private val ACTIONS: BiMap<Int, String?> = BiMaps.newBuilder<Int, String?>()
            .put(R.id.run_on_startup, DynamicBroadcastReceivers.ACTION_STARTUP)
            .put(R.id.run_on_boot, Intent.ACTION_BOOT_COMPLETED)
            .put(R.id.run_on_screen_off, Intent.ACTION_SCREEN_OFF)
            .put(R.id.run_on_screen_on, Intent.ACTION_SCREEN_ON)
            .put(R.id.run_on_screen_unlock, Intent.ACTION_USER_PRESENT)
            .put(R.id.run_on_battery_change, Intent.ACTION_BATTERY_CHANGED)
            .put(R.id.run_on_power_connect, Intent.ACTION_POWER_CONNECTED)
            .put(R.id.run_on_power_disconnect, Intent.ACTION_POWER_DISCONNECTED)
            .put(R.id.run_on_conn_change, ConnectivityManager.CONNECTIVITY_ACTION)
            .put(R.id.run_on_package_install, Intent.ACTION_PACKAGE_ADDED)
            .put(R.id.run_on_package_uninstall, Intent.ACTION_PACKAGE_REMOVED)
            .put(R.id.run_on_package_update, Intent.ACTION_PACKAGE_REPLACED)
            .put(R.id.run_on_headset_plug, Intent.ACTION_HEADSET_PLUG)
            .put(R.id.run_on_config_change, Intent.ACTION_CONFIGURATION_CHANGED)
            .put(R.id.run_on_time_tick, Intent.ACTION_TIME_TICK)
            .build()
    }
}
