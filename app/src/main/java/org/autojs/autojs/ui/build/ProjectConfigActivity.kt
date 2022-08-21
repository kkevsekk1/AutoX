package org.autojs.autojs.ui.build

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.textfield.TextInputLayout
import com.stardust.autojs.project.ProjectConfig
import com.stardust.autojs.project.ProjectConfig.Companion.configFileOfDir
import com.stardust.autojs.project.ProjectConfig.Companion.fromProjectDirAsync
import com.stardust.pio.PFiles.ensureDir
import com.stardust.pio.PFiles.write
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.androidannotations.annotations.AfterViews
import org.androidannotations.annotations.Click
import org.androidannotations.annotations.EActivity
import org.androidannotations.annotations.ViewById
import org.autojs.autojs.model.explorer.ExplorerDirPage
import org.autojs.autojs.model.explorer.ExplorerFileItem
import org.autojs.autojs.model.explorer.Explorers
import org.autojs.autojs.model.project.ProjectTemplate
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder
import org.autojs.autojs.tool.getRandomString
import org.autojs.autojs.ui.BaseActivity
import org.autojs.autojs.ui.shortcut.ShortcutIconSelectActivity.Companion.getBitmapFromIntent
import org.autojs.autojs.ui.shortcut.ShortcutIconSelectActivity_
import org.autojs.autojs.ui.widget.SimpleTextWatcher
import org.autojs.autoxjs.R
import java.io.File
import java.io.FileOutputStream

@SuppressLint("NonConstantResourceId")
@EActivity(R.layout.activity_project_config)
open class ProjectConfigActivity : BaseActivity() {
    @JvmField
    @ViewById(R.id.project_location)
    var mProjectLocation: EditText? = null

    @JvmField
    @ViewById(R.id.app_name)
    var mAppName: EditText? = null

    @JvmField
    @ViewById(R.id.package_name)
    var mPackageName: EditText? = null

    @JvmField
    @ViewById(R.id.version_name)
    var mVersionName: EditText? = null

    @JvmField
    @ViewById(R.id.version_code)
    var mVersionCode: EditText? = null

    @JvmField
    @ViewById(R.id.main_file_name)
    var mMainFileName: EditText? = null

    @JvmField
    @ViewById(R.id.icon)
    var mIcon: ImageView? = null
    private var mDirectory: File? = null
    private var mParentDirectory: File? = null
    private var mProjectConfig: ProjectConfig? = null
    private var mNewProject = false
    private var mIconBitmap: Bitmap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mNewProject = intent.getBooleanExtra(EXTRA_NEW_PROJECT, false)
        val parentDirectory = intent.getStringExtra(EXTRA_PARENT_DIRECTORY)
        if (mNewProject) {
            if (parentDirectory == null) {
                finish()
                return
            }
            mParentDirectory = File(parentDirectory)
            mProjectConfig = ProjectConfig()
        } else {
            val dir = intent.getStringExtra(EXTRA_DIRECTORY)
            if (dir == null) {
                finish()
                return
            }
            mDirectory = File(dir)
            lifecycleScope.launch {
                mProjectConfig = fromProjectDirAsync(dir)
                if (mProjectConfig == null) {
                    ThemeColorMaterialDialogBuilder(this@ProjectConfigActivity)
                        .title(R.string.text_invalid_project)
                        .positiveText(R.string.ok)
                        .dismissListener { finish() }
                        .show()
                }
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

    @AfterViews
    fun setupViews() {
        if (mProjectConfig == null) {
            return
        }
        setToolbarAsBack(if (mNewProject) getString(R.string.text_new_project) else mProjectConfig!!.name)

        if (mNewProject) {
            setRandomPackageName()
            mAppName!!.addTextChangedListener(
                SimpleTextWatcher { s: Editable ->
                    mProjectLocation!!.setText(
                        File(mParentDirectory, s.toString()).path
                    )
                    mPackageName!!.setText(
                        mPackageName!!.text.toString().replacePackageName(s.toString())
                    )
                }
            )
        } else {
            mAppName!!.setText(mProjectConfig!!.name)
            mVersionCode!!.setText(mProjectConfig!!.versionCode.toString())
            mVersionName!!.setText(mProjectConfig!!.versionName)
            mMainFileName!!.setText(mProjectConfig!!.mainScript)
            mProjectLocation!!.visibility = View.GONE
            mProjectConfig!!.packageName
                .takeIf { !it.isNullOrBlank() && !REGEX_PACKAGE_NAME.matches(it) }
                ?.let { mPackageName!!.setText(it) }
                ?: kotlin.run { setRandomPackageName() }
            val icon = mProjectConfig!!.icon
            if (icon != null) {
                Glide.with(this)
                    .setDefaultRequestOptions(
                        RequestOptions.skipMemoryCacheOf(true).diskCacheStrategy(
                            DiskCacheStrategy.NONE
                        )
                    )
                    .load(File(mDirectory, icon))
                    .into(mIcon!!)
            }
        }
    }

    private fun setRandomPackageName() {
        mPackageName!!.setText(
            getString(
                R.string.format_default_package_name,
                getRandomString(6)
            )
        )
    }

    @SuppressLint("CheckResult")
    @Click(R.id.fab)
    fun commit() {
        if (!checkInputs()) {
            return
        }
        syncProjectConfig()
        if (mIconBitmap != null) {
            saveIcon(mIconBitmap!!)
                .subscribe({ saveProjectConfig() }) { e: Throwable ->
                    e.printStackTrace()
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                }
        } else {
            saveProjectConfig()
        }
    }

    @SuppressLint("CheckResult")
    private fun saveProjectConfig() {
        lifecycleScope.launch {

            try {
                if (mNewProject) {
                    ProjectTemplate(mProjectConfig!!, mDirectory!!).newProject()
                    Explorers.workspace()
                        .notifyChildrenChanged(ExplorerDirPage(mParentDirectory, null))
                } else {
                    asyncWriteProjectConfig()
                    val item = ExplorerFileItem(mDirectory, null)
                    Explorers.workspace().notifyItemChanged(item, item)
                }
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ProjectConfigActivity, e.message, Toast.LENGTH_SHORT).show()
            }

        }
    }

    private suspend fun asyncWriteProjectConfig() = withContext(Dispatchers.IO) {
        write(
            configFileOfDir(mDirectory!!.path),
            mProjectConfig!!.toJson()
        )
    }

    @Click(R.id.icon)
    fun selectIcon() {
        ShortcutIconSelectActivity_.intent(this)
            .startForResult(REQUEST_CODE)
    }

    private fun syncProjectConfig() {
        mProjectConfig!!.name = mAppName!!.text.toString()
        mProjectConfig!!.versionCode = mVersionCode!!.text.toString().toInt()
        mProjectConfig!!.versionName = mVersionName!!.text.toString()
        mProjectConfig!!.mainScript = mMainFileName!!.text.toString()
        mProjectConfig!!.packageName = mPackageName!!.text.toString()
        if (mNewProject) {
            val location = mProjectLocation!!.text.toString()
            mDirectory = File(location)
        }
    }

    private fun checkInputs(): Boolean {
        var inputValid = true
        inputValid = inputValid and checkNotEmpty(mAppName)
        inputValid = inputValid and checkNotEmpty(mVersionCode)
        inputValid = inputValid and checkNotEmpty(mVersionName)
        inputValid = inputValid and checkPackageNameValid(mPackageName)
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
                mIcon!!.setImageBitmap(bitmap)
                mIconBitmap = bitmap
            }) { obj: Throwable -> obj.printStackTrace() }
    }

    @SuppressLint("CheckResult")
    private fun saveIcon(b: Bitmap): Observable<String> {
        return Observable.just(b)
            .map { bitmap: Bitmap ->
                var iconPath = mProjectConfig!!.icon
                if (iconPath == null) {
                    iconPath = "res/logo.png"
                }
                val iconFile = File(mDirectory, iconPath)
                ensureDir(iconFile.path)
                val fos = FileOutputStream(iconFile)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.close()
                iconPath
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { iconPath: String? -> mProjectConfig!!.icon = iconPath }
    }

    companion object {
        const val EXTRA_PARENT_DIRECTORY = "parent_directory"
        const val EXTRA_NEW_PROJECT = "new_project"
        const val EXTRA_DIRECTORY = "directory"
        private const val REQUEST_CODE = 12477
        private val REGEX_PACKAGE_NAME =
            Regex("^([A-Za-z][A-Za-z\\d_]*\\.)+([A-Za-z][A-Za-z\\d_]*)$")
    }
}