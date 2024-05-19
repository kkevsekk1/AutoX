package org.autojs.autojs.ui.explorer

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.stardust.autojs.project.ProjectConfig
import com.stardust.autojs.project.ProjectConfig.Companion.fromProject
import com.stardust.autojs.project.ProjectLauncher
import com.stardust.pio.PFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.model.explorer.ExplorerChangeEvent
import org.autojs.autojs.model.explorer.Explorers
import org.autojs.autojs.ui.build.BuildActivity.Companion.start
import org.autojs.autojs.ui.build.ProjectConfigActivity
import org.autojs.autojs.ui.util.launchActivity
import org.autojs.autoxjs.R
import org.greenrobot.eventbus.Subscribe
import java.io.File

class ExplorerProjectToolbar : CardView {
    private var mProjectConfig: ProjectConfig? = null
    private var mDirectory: PFile? = null
    var mProjectName: TextView? = null

    constructor(context: Context?) : super(context!!) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!, attrs, defStyleAttr
    ) {
        init()
    }

    private fun init() {
        val view = inflate(context, R.layout.explorer_project_toolbar, this)
        mProjectName = view.findViewById(R.id.project_name)
        view.findViewById<View>(R.id.run).setOnClickListener {
            run()
        }
        view.findViewById<View>(R.id.build).setOnClickListener {
            build()
        }
        view.findViewById<View>(R.id.sync).setOnClickListener {
            sync()
        }
        setOnClickListener { edit() }
    }

    fun setProject(dir: PFile) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                mProjectConfig = fromProject(File(dir.path))
            }

            if (mProjectConfig == null) {
                visibility = GONE
                return@launch
            }
            mDirectory = dir
            mProjectName!!.text = mProjectConfig!!.name
        }
    }

    fun refresh() {
        if (mDirectory != null) {
            setProject(mDirectory!!)
        }
    }

    fun run() {
        try {
            ProjectLauncher(mDirectory!!.path)
                .launch(AutoJs.getInstance().scriptEngineService)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
        }
    }

    fun build() {
        start(context, mDirectory!!.path)
    }

    fun sync() {
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Explorers.workspace().registerChangeListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Explorers.workspace().unregisterChangeListener(this)
    }

    @Subscribe
    fun onExplorerChange(event: ExplorerChangeEvent) {
        if (mDirectory == null) {
            return
        }
        val item = event.item
        if (event.action == ExplorerChangeEvent.ALL
            || item != null && mDirectory!!.path == item.path
        ) {
            refresh()
        }
    }

    fun edit() {
        context.launchActivity<ProjectConfigActivity> {
            putExtra(ProjectConfigActivity.EXTRA_DIRECTORY, mDirectory!!.path)
        }
    }
}