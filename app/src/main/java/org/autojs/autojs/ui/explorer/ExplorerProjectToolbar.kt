package org.autojs.autojs.ui.explorer

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
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
import org.autojs.autojs.ui.build.ProjectConfigActivity_
import org.autojs.autoxjs.R
import org.greenrobot.eventbus.Subscribe
import java.io.File

class ExplorerProjectToolbar : CardView {
    private var mProjectConfig: ProjectConfig? = null
    private var mDirectory: PFile? = null

    @JvmField
    @BindView(R.id.project_name)
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
        inflate(context, R.layout.explorer_project_toolbar, this)
        ButterKnife.bind(this)
        setOnClickListener { view: View? -> edit() }
    }

    fun setProject(dir: PFile) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO){
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

    @OnClick(R.id.run)
    fun run() {
        try {
            ProjectLauncher(mDirectory!!.path)
                .launch(AutoJs.getInstance().scriptEngineService)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
        }
    }

    @OnClick(R.id.build)
    fun build() {
        start(context, mDirectory!!.path)
    }

    @OnClick(R.id.sync)
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
        ProjectConfigActivity_.intent(context)
            .extra(ProjectConfigActivity.EXTRA_DIRECTORY, mDirectory!!.path)
            .start()
    }
}