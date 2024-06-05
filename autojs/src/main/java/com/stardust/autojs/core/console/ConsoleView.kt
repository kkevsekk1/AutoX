package com.stardust.autojs.core.console

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.postDelayed
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stardust.autojs.R
import com.stardust.autojs.core.console.ConsoleImpl.LogListener
import com.stardust.enhancedfloaty.ResizableExpandableFloatyWindow
import com.stardust.util.SparseArrayEntries
import com.stardust.util.ViewUtils

/**
 * Created by Stardust on 2017/5/2.
 *
 *
 * TODO: 优化为无锁形式
 */
class ConsoleView : FrameLayout, LogListener {
    val colors = COLORS.clone()
    private var mConsole: ConsoleImpl? = null
    private lateinit var mLogListRecyclerView: RecyclerView
    private lateinit var mEditText: EditText
    private lateinit var submitButton: Button
    private var mWindow: ResizableExpandableFloatyWindow? = null
    private var mShouldStopRefresh = false
    private val mLogEntries = ArrayList<ConsoleImpl.LogEntry>()
    private var mLogSize = -1f

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        inflate(context, R.layout.console_view, this)
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ConsoleView)
            for ((styleable, logLevel) in ATTRS) {
                colors.put(logLevel, typedArray.getColor(styleable, colors[logLevel]))
            }
            typedArray.recycle()
        }
        mLogListRecyclerView = findViewById(R.id.log_list)
        val manager = LinearLayoutManager(context)
        mLogListRecyclerView.layoutManager = manager
        mLogListRecyclerView.adapter = Adapter()
        initEditText()
        initSubmitButton()
    }

    private fun initSubmitButton() {
        submitButton = findViewById(R.id.submit)
        submitButton.setOnClickListener {
            val input: CharSequence = mEditText.text
            submitInput(input)
        }
    }

    private fun submitInput(input: CharSequence) {
        if (TextUtils.isEmpty(input)) {
            return
        }
        if (mConsole!!.submitInput(input)) {
            mEditText.setText("")
        }
    }

    private fun initEditText() {
        mEditText = findViewById(R.id.input)
        mEditText.isFocusableInTouchMode = true
        val mInputContainer = findViewById<LinearLayout>(R.id.input_container)
        val listener = OnClickListener { _: View? ->
            if (mWindow != null) {
                mWindow!!.requestWindowFocus()
                mEditText.requestFocus()
            }
        }
        mEditText.setOnClickListener(listener)
        mInputContainer.setOnClickListener(listener)
    }

    fun setConsole(console: ConsoleImpl) {
        mConsole = console
        console.setConsoleView(this)
    }

    override fun onNewLog(logEntry: ConsoleImpl.LogEntry) = Unit
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mShouldStopRefresh = false
        postDelayed(
            object : Runnable {
                override fun run() {
                    refreshLog()
                    if (!mShouldStopRefresh) {
                        postDelayed(this, REFRESH_INTERVAL)
                    }
                }
            },
            REFRESH_INTERVAL
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mShouldStopRefresh = true
    }

    override fun onLogClear() {
        post {
            mLogEntries.clear()
            mLogListRecyclerView.adapter!!.notifyDataSetChanged()
        }
    }

    private fun refreshLog() {
        if (mConsole == null) return
        val oldSize = mLogEntries.size
        val logEntries = mConsole!!.allLogs
        synchronized(mConsole!!.allLogs) {
            val size = logEntries.size
            if (size == 0) {
                return
            }
            if (oldSize >= size) {
                return
            }
            if (oldSize == 0) {
                mLogEntries.addAll(logEntries)
            } else {
                for (i in oldSize until size) {
                    mLogEntries.add(logEntries[i])
                }
            }
            mLogListRecyclerView.adapter!!.notifyItemRangeInserted(oldSize, size - 1)
            mLogListRecyclerView.scrollToPosition(size - 1)
        }
    }

    fun setWindow(window: ResizableExpandableFloatyWindow?) {
        mWindow = window
    }

    fun showEditText() {
        post {
            mEditText.visibility = VISIBLE
            submitButton.visibility = VISIBLE
            mWindow!!.requestWindowFocus()
            // mInputContainer.setVisibility(VISIBLE);
            mEditText.requestFocus()
        }
    }

    fun hideEditText() {
        post {
            mEditText.visibility = GONE
            submitButton.visibility = GONE
        }
    }

    fun setLogSize(size: Int) {
        mLogSize = ViewUtils.spToPx(context, size.toFloat()).toInt().toFloat()
    }

    private class ViewHolder(itemView: View, val logSize: Float) :
        RecyclerView.ViewHolder(itemView) {
        var textView: TextView

        init {
            textView = itemView as TextView
        }

        fun bindData(logEntry: ConsoleImpl.LogEntry) {
            val text = if (logEntry.content.length > 5000) {
                logEntry.content.substring(0, 2000) + " ......<${logEntry.content.length - 5000}>"
            } else {
                logEntry.content
            }
            textView.text = text
            if (logSize != -1f) {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, logSize)
            }
        }
    }

    private inner class Adapter : RecyclerView.Adapter<ViewHolder?>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.console_view_item, parent, false),
                mLogSize
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val logEntry = mLogEntries[position]
            holder.bindData(logEntry)
            holder.textView.setTextColor(colors[logEntry.level])
        }

        override fun getItemCount(): Int {
            return mLogEntries.size
        }
    }

    companion object {
        private val ATTRS = mapOf(
            R.styleable.ConsoleView_color_verbose to Log.VERBOSE,
            R.styleable.ConsoleView_color_debug to Log.DEBUG,
            R.styleable.ConsoleView_color_info to Log.INFO,
            R.styleable.ConsoleView_color_warn to Log.WARN,
            R.styleable.ConsoleView_color_error to Log.ERROR,
            R.styleable.ConsoleView_color_assert to Log.ASSERT,
        )
        val COLORS = SparseArrayEntries<Int>()
            .entry(Log.VERBOSE, -0x203f3f40)
            .entry(Log.DEBUG, -0x20000001)
            .entry(Log.INFO, -0x9b22e9)
            .entry(Log.WARN, -0xd69d01)
            .entry(Log.ERROR, -0x2b0000)
            .entry(Log.ASSERT, -0xacb2)
            .sparseArray()
        private const val REFRESH_INTERVAL = 100L
    }
}
