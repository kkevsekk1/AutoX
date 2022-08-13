package org.autojs.autojs.ui.build

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.MaterialDialog
import org.autojs.autojs.ui.compose.theme.AutoXJsTheme
import org.autojs.autojs.ui.compose.util.SetSystemUI

/**
 * Modified by wilinz on 2022/5/23
 */
open class BuildActivity : ComponentActivity() {

    private var progressDialog: MaterialDialog? = null

    // 单文件打包清爽模式
    private lateinit var viewModel: BuildViewModel

    companion object {
        @JvmField
        val EXTRA_SOURCE = BuildActivity::class.java.name + ".extra_source_file"
        const val TAG = "BuildActivity"

        fun getIntent(context: Context, sourcePath: String?): Intent {
            return Intent(context, BuildActivity::class.java).putExtra(EXTRA_SOURCE, sourcePath)
        }

        /**
         * @param sourcePath 可能是项目目录，也可能是脚本文件
         */
        fun start(context: Context, sourcePath: String?) {
            context.startActivity(getIntent(context, sourcePath))
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val source = intent.getStringExtra(EXTRA_SOURCE) ?: kotlin.run {
            finish()
            return
        }
        viewModel = ViewModelProvider(
            this,
            BuildViewModelFactory(application, source)
        )[BuildViewModel::class.java]

        setContent {
            AutoXJsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    SetSystemUI()
                    BuildPage(viewModel)
                }
            }
        }
    }

}