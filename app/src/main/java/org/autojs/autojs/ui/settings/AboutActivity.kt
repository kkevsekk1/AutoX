package org.autojs.autojs.ui.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.appbar.MaterialToolbar
import com.stardust.util.ClipboardUtil
import com.stardust.util.IntentUtil
import com.stardust.util.IntentUtilKt.launchQQ
import com.tencent.bugly.crashreport.CrashReport
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder
import org.autojs.autojs.tool.IntentTool
import org.autojs.autoxjs.R

/**
 * Created by Stardust on 2017/2/2.
 */

open class AboutActivity : AppCompatActivity() {
    private lateinit var mVersion: TextView
    private var mLolClickCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        mVersion = findViewById(R.id.version)
        setUpViews()
    }

    private fun setUpViews() {
        setUpToolbar()
        setVersionName()
        findViewById<View>(R.id.github).setOnClickListener { openGitHub() }
        findViewById<View>(R.id.qq).setOnClickListener { openQQToChatWithMe() }
        findViewById<View>(R.id.email).setOnClickListener { openEmailToSendMe() }
        findViewById<View>(R.id.share).setOnClickListener {
            IntentUtil.shareText(this, getString(R.string.share_app))
        }
        findViewById<View>(R.id.icon).setOnClickListener {
            mLolClickCount++
            if (mLolClickCount >= 5) {
                mLolClickCount = 0
                crashTest()
            }
        }
        findViewById<View>(R.id.developer).setOnClickListener {
            Toast.makeText(
                this, R.string.text_it_is_the_developer_of_app, Toast.LENGTH_LONG
            ).show()
        }
    }
    private fun setUpToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }
    @SuppressLint("SetTextI18n")
    private fun setVersionName() {
        mVersion.text = "Version ${packageManager.getPackageInfo(packageName, 0).versionName}"
    }

    private fun openGitHub() {
        IntentTool.browse(this, getString(R.string.my_github))
    }

    private fun openQQToChatWithMe() {
        val qq = getString(R.string.qq)
        ClipboardUtil.setClip(this, qq)
        Toast.makeText(this, R.string.text_qq_already_copy_to_clip, Toast.LENGTH_SHORT).show()
        if (!launchQQ(this)) {
            Toast.makeText(this, R.string.text_mobile_qq_not_installed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openEmailToSendMe() {
        val email = getString(R.string.email)
        IntentUtil.sendMailTo(this, email)
    }

    private fun showEasterEgg() {
        MaterialDialog.Builder(this).customView(R.layout.paint_layout, false).show()
    }

    private fun crashTest() {
        ThemeColorMaterialDialogBuilder(this).title("Crash Test").positiveText("Crash")
            .onPositive { dialog: MaterialDialog?, which: DialogAction? -> CrashReport.testJavaCrash() }
            .show()
    }

    companion object {
        private const val TAG = "AboutActivity"
    }
}
