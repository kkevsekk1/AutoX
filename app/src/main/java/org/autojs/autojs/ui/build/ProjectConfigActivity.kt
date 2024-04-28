package org.autojs.autojs.ui.build

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.textfield.TextInputLayout
import com.stardust.autojs.project.ProjectConfig
import com.stardust.autojs.project.ProjectConfig.Companion.configFileOfDir
import com.stardust.pio.PFiles.ensureDir
import com.stardust.pio.PFiles.write
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.autojs.autojs.model.explorer.ExplorerDirPage
import org.autojs.autojs.model.explorer.ExplorerFileItem
import org.autojs.autojs.model.explorer.Explorers
import org.autojs.autojs.model.project.ProjectTemplate
import org.autojs.autojs.tool.getRandomString
import org.autojs.autojs.ui.shortcut.ShortcutIconSelectActivity
import org.autojs.autojs.ui.shortcut.ShortcutIconSelectActivity.Companion.getBitmapFromIntent
import org.autojs.autoxjs.R
import org.autojs.autoxjs.databinding.ActivityProjectConfigBinding
import java.io.File
import java.io.FileOutputStream


open class ProjectConfigActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProjectConfigBinding
    private var mDirectory: File? = null

    private var mParentDirectory: File? = null
    private var mProjectConfig: ProjectConfig? = null
    private var mNewProject = false
    private var mIconBitmap: Bitmap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProjectConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupViews()
        initProject()
    }

    private fun initProject() = lifecycleScope.launch(Dispatchers.Default) {
        mNewProject = intent.getBooleanExtra(EXTRA_NEW_PROJECT, false)
        val parentDirectory = intent.getStringExtra(EXTRA_PARENT_DIRECTORY)
        if (mNewProject) {
            if (parentDirectory == null) return@launch finish()
            mParentDirectory = File(parentDirectory)
            mProjectConfig = ProjectConfig()
        } else {
            val dir = intent.getStringExtra(EXTRA_DIRECTORY) ?: return@launch finish()
            mDirectory = File(dir)
            mProjectConfig = ProjectConfig.fromProject(mDirectory!!)
            if (mProjectConfig == null) {
                launch(Dispatchers.Main) {
                    Toast.makeText(
                        this@ProjectConfigActivity,
                        R.string.text_invalid_project,
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
        Log.i(TAG, "project config: $mProjectConfig")
        if (mProjectConfig == null) return@launch
        bindProjectConfig(mProjectConfig!!)
    }

    private suspend fun bindProjectConfig(projectConfig: ProjectConfig): Unit =
        withContext(Dispatchers.Main) {
            if (mNewProject) {
                setRandomPackageName()
                binding.appName.addTextChangedListener {
                    binding.projectLocation.setText(
                        File(mParentDirectory, it.toString()).path
                    )
                    binding.packageName.setText(
                        binding.packageName.text.toString().replacePackageName(it.toString())
                    )
                }
            } else {
                binding.toolbar.title = projectConfig.name
                binding.appName.setText(projectConfig.name)
                binding.versionCode.setText(projectConfig.versionCode.toString())
                binding.versionName.setText(projectConfig.versionName)
                binding.mainFileName.setText(projectConfig.mainScript)
                binding.projectLocation.visibility = View.GONE
                projectConfig.packageName
                    .takeIf { !it.isNullOrBlank() && !REGEX_PACKAGE_NAME.matches(it) }
                    ?.let { binding.packageName.setText(it) }
                    ?: kotlin.run { setRandomPackageName() }
                val icon = projectConfig.icon
                if (icon != null) {
                    Glide.with(this@ProjectConfigActivity)
                        .setDefaultRequestOptions(
                            RequestOptions.skipMemoryCacheOf(true).diskCacheStrategy(
                                DiskCacheStrategy.NONE
                            )
                        )
                        .load(File(mDirectory, icon))
                        .into(binding.icon)
                }
            }
        }

    private fun String.replacePackageName(newValue: String): String {
        val split = this.split(".").toMutableList()
        split.lastIndex.takeIf { it >= 0 }?.let { split[it] = newValue }
        val newPackage = split.joinToString(".")
        return if (REGEX_PACKAGE_NAME.matches(newPackage)) newPackage
        else this
    }

    fun setupViews() {
        binding.fab.setOnClickListener { commit() }
        binding.icon.setOnClickListener { selectIcon() }
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

    }

    private fun setRandomPackageName() {
        binding.packageName.setText(
            getString(
                R.string.format_default_package_name,
                getRandomString(6)
            )
        )
    }

    private fun commit() = lifecycleScope.launch(Dispatchers.IO) {
        if (!checkInputs()) return@launch
        try {
            syncProjectConfig()
            mIconBitmap?.let { saveIcon(it) }
            saveProjectConfig()
        } catch (e: Throwable) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ProjectConfigActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }

    private suspend fun saveProjectConfig() {
        if (mNewProject) {
            ProjectTemplate(mProjectConfig!!, mDirectory!!).newProject()
            Explorers.workspace()
                .notifyChildrenChanged(ExplorerDirPage(mParentDirectory, null))
        } else {
            asyncWriteProjectConfig()
            val item = ExplorerFileItem(mDirectory, null)
            Explorers.workspace().notifyItemChanged(item, item)
        }
    }

    private suspend fun asyncWriteProjectConfig() = withContext(Dispatchers.IO) {
        write(
            configFileOfDir(mDirectory!!.path),
            mProjectConfig!!.toJson()
        )
    }

    private fun selectIcon() {
        startActivityForResult(Intent(this, ShortcutIconSelectActivity::class.java), REQUEST_CODE)
    }

    private fun syncProjectConfig() {
        mProjectConfig!!.name = binding.appName.text.toString()
        mProjectConfig!!.versionCode = binding.versionCode.text.toString().toInt()
        mProjectConfig!!.versionName = binding.versionName.text.toString()
        mProjectConfig!!.mainScript = binding.mainFileName.text.toString()
        mProjectConfig!!.packageName = binding.packageName.text.toString()
        if (mNewProject) {
            val location = binding.projectLocation.text.toString()
            mDirectory = File(location)
        }
    }

    private fun checkInputs(): Boolean {
        var inputValid = true
        inputValid = inputValid and checkNotEmpty(binding.appName)
        inputValid = inputValid and checkNotEmpty(binding.versionCode)
        inputValid = inputValid and checkNotEmpty(binding.versionName)
        inputValid = inputValid and checkPackageNameValid(binding.packageName)
        return inputValid
    }

    private fun checkPackageNameValid(editText: EditText?): Boolean {
        val text = editText!!.text
        val hint = (editText.parent.parent as TextInputLayout).hint.toString()
        if (TextUtils.isEmpty(text)) {
            editText.error = hint + getString(R.string.text_should_not_be_empty)
            return false
        }
        if (!REGEX_PACKAGE_NAME.matches(text)) {
            editText.error = getString(R.string.text_invalid_package_name)
            return false
        }
        return true
    }

    private fun checkNotEmpty(editText: EditText?): Boolean {
        if (!TextUtils.isEmpty(editText!!.text)) return true
        // TODO: 2017/12/8 more beautiful ways?
        val hint = (editText.parent.parent as TextInputLayout).hint.toString()
        editText.error = hint + getString(R.string.text_should_not_be_empty)
        return false
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("CheckResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            return
        }
        getBitmapFromIntent(applicationContext, data!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ bitmap: Bitmap? ->
                binding.icon.setImageBitmap(bitmap)
                mIconBitmap = bitmap
            }) { obj: Throwable -> obj.printStackTrace() }
    }

    private fun saveIcon(bitmap: Bitmap) {
        val projectConfig = mProjectConfig!!
        var iconPath = projectConfig.icon
        if (iconPath == null) {
            iconPath = "res/logo.png"
        }
        val iconFile = File(mDirectory, iconPath)
        ensureDir(iconFile.path)
        FileOutputStream(iconFile).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        mProjectConfig!!.icon = iconPath
    }

    companion object {
        fun editProjectConfig(context: Context, directory: File) {
            context.startActivity(
                Intent(context, ProjectConfigActivity::class.java).putExtra(
                    EXTRA_DIRECTORY, directory.path
                )
            )
        }

        fun newProject(context: Context, parentDirectory: File) {
            context.startActivity(Intent(context, ProjectConfigActivity::class.java).apply {
                putExtra(
                    EXTRA_PARENT_DIRECTORY, parentDirectory.path
                )
                putExtra(EXTRA_NEW_PROJECT, true)
            })
        }

        private const val TAG = "ProjectConfigActivity"
        const val EXTRA_PARENT_DIRECTORY = "parent_directory"
        const val EXTRA_NEW_PROJECT = "new_project"
        const val EXTRA_DIRECTORY = "directory"
        private const val REQUEST_CODE = 12477
        private val REGEX_PACKAGE_NAME =
            Regex("^([A-Za-z][A-Za-z\\d_]*\\.)+([A-Za-z][A-Za-z\\d_]*)$")
    }
}