package org.autojs.autojs.ui.build

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import org.autojs.autojs.ui.util.setupToolbar
import org.autojs.autoxjs.databinding.ActivityBuildBinding

/**
 * Modified by wilinz on 2022/5/23
 */
open class BuildActivity : AppCompatActivity() {

    // 单文件打包清爽模式
    private lateinit var viewModel: BuildViewModel
    private lateinit var binding: ActivityBuildBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val source = intent.getStringExtra(EXTRA_SOURCE) ?: run {
            finish()
            return
        }
        binding = ActivityBuildBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(
            this, BuildViewModelFactory(application, source)
        )[BuildViewModel::class.java]
        setContentView(binding.root)
        setupToolbar(binding.toolbar)
//        setContent {
//            AutoXJsTheme {
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colors.background
//                ) {
//                    SetSystemUI()
//                    BuildPage(viewModel)
//                }
//            }
//        }
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