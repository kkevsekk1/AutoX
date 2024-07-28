package org.autojs.autojs.ui.build

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.aiselp.autox.ui.material3.theme.AppTheme

/**
 * Modified by wilinz on 2022/5/23
 */
class BuildActivity : AppCompatActivity() {

    // 单文件打包清爽模式
    private lateinit var viewModel: BuildViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val source = intent.getStringExtra(EXTRA_SOURCE) ?: run {
            finish()
            return
        }
        viewModel = ViewModelProvider(
            this, BuildViewModelFactory(application, source)
        )[BuildViewModel::class.java]
        setContent {
            AppTheme {
                com.aiselp.autox.ui.material3.BuildPage(viewModel)
            }
        }
    }

    companion object {
        private val EXTRA_SOURCE = BuildActivity::class.java.name + ".extra_source_file"
        const val TAG = "BuildActivity"

        private fun getIntent(context: Context, sourcePath: String?): Intent {
            return Intent(context, BuildActivity::class.java).putExtra(EXTRA_SOURCE, sourcePath)
        }

        /**
         * @param sourcePath 可能是项目目录，也可能是脚本文件
         */
        fun start(context: Context, sourcePath: String?) {
            context.startActivity(getIntent(context, sourcePath))
        }

    }
}