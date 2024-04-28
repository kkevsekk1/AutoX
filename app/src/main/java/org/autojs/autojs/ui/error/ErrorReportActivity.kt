package org.autojs.autojs.ui.error

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.SparseIntArray
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.heinrichreimersoftware.androidissuereporter.model.DeviceInfo
import org.autojs.autojs.ui.main.MainActivity
import org.autojs.autoxjs.R
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

/**
 * Created by Stardust on 2017/2/2.
 */
class ErrorReportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error_report)
        setUpUI()
    }

    override fun onStart() {
        super.onStart()
        handleIntent()
    }

    private val crashCountText: String by lazy {
        val i = getPreferences(MODE_PRIVATE).getInt(KEY_CRASH_COUNT, 0) + 1
        getPreferences(MODE_PRIVATE).edit().putInt(KEY_CRASH_COUNT, i).apply()
        if (i < 2) "" else getString(
            CRASH_COUNT[min(i.toDouble(), 5.0).toInt()]
        )
    }

    private fun handleIntent() {
        val message = intent.getStringExtra("message")
        val errorDetail = intent.getStringExtra("error")
        val errorTextView: TextView = findViewById(R.id.error)
        val crashInfo =
            String.format("%s错误信息:\n%s\n%s", deviceMessage, message, errorDetail)
        errorTextView.text = crashInfo
        saveCrashLog(crashInfo)
    }

    private val deviceMessage: String by lazy {
        val deviceInfo = DeviceInfo(this)
        String.format(Locale.getDefault(), "设备信息: \n%s\n\n", deviceInfo)
    }

    private fun copyToClip(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Debug", text)
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip)
            Toast.makeText(
                this@ErrorReportActivity,
                R.string.text_already_copy_to_clip,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setUpUI() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        val copyButton: Button = findViewById(R.id.copy)
        val restartButton: Button = findViewById(R.id.restart)
        val exitButton: Button = findViewById(R.id.exit)
        val mTitle = String.format("%s%s", crashCountText, getString(R.string.text_crash))
        toolbar.setTitle(mTitle)
        setSupportActionBar(toolbar)
        supportActionBar!!.setHomeButtonEnabled(false)
        copyButton.setOnClickListener { view: View -> copy(view) }
        restartButton.setOnClickListener { view: View -> restart(view) }
        exitButton.setOnClickListener { exit() }
    }


    private fun exit() {
        finishAffinity()
    }

    private fun saveCrashLog(crashInfo: String) {
        try {
            val logDir = File(getExternalFilesDir(null), "AutoJs_Log")
            if (!logDir.exists() && !logDir.mkdirs()) {
                Log.e(TAG, "Error creating directory: " + logDir.absolutePath)
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Log_$timeStamp.txt"
            val logFile = File(logDir, fileName)
            FileWriter(logFile).use { writer -> writer.write(crashInfo) }
            val saveTextView: TextView = findViewById(R.id.save_path)
            val savedToText = getString(R.string.text_error_save, logFile.absolutePath)
            saveTextView.text = savedToText
        } catch (e: IOException) {
            Log.e(TAG, "Error saving crash log", e)
        }
    }

    private fun copy(view: View) {
        val errorTextView = findViewById<TextView>(R.id.error)
        if (errorTextView != null) {
            val crashInfo = errorTextView.getText().toString()
            copyToClip(crashInfo)
        }
    }

    private fun restart(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "ErrorReportActivity"
        private val CRASH_COUNT = SparseIntArray()
        private const val KEY_CRASH_COUNT = "crash_count"

        init {
            CRASH_COUNT.put(2, R.string.text_again)
            CRASH_COUNT.put(3, R.string.text_again_and_again)
            CRASH_COUNT.put(4, R.string.text_again_and_again_again)
            CRASH_COUNT.put(5, R.string.text_again_and_again_again_again)
        }
    }
}
