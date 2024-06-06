package org.autojs.autojs.ui.shortcut

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import org.autojs.autojs.tool.BitmapTool
import org.autojs.autojs.tool.writeTo
import org.autojs.autojs.ui.BaseActivity
import org.autojs.autojs.workground.WrapContentGridLayoutManger
import org.autojs.autoxjs.R
import java.io.File


/**
 * Created by Stardust on 2017/10/25.
 * Modified by wilinz on 2022/5/23
 */
open class ShortcutIconSelectActivity : BaseActivity() {
    override val layoutId = R.layout.activity_shortcut_icon_select

    private lateinit var mApps: RecyclerView
    private val mAppList = mutableListOf<AppItem>()

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK, result.data)
                finish()
            }
        }

    private fun setupViews() {
        setToolbarAsBack(getString(R.string.text_select_icon))
        setupApps()
    }

    private fun setupApps() {
        mApps.adapter = AppsAdapter()
        val manager = WrapContentGridLayoutManger(this, 5)
        manager.setDebugInfo("IconSelectView")
        mApps.layoutManager = manager
        loadApps()
    }

    private fun loadApps() {
        packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.icon != 0 }
            .map { AppItem(it) }
            .forEach {
                mAppList.add(it)
                mApps.adapter!!.notifyItemInserted(mAppList.size - 1)
            }
    }

    private fun selectApp(appItem: AppItem) {
        val file = File(this.cacheDir, "icon/${appItem.info.packageName}.png")
        file.parentFile?.let { if (!it.exists()) it.mkdirs() }
        appItem.icon.toBitmapOrNull()?.writeTo(file)
        setResult(
            RESULT_OK, Intent().setData(file.toUri())
        )
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_shortcut_icon_select, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
        resultLauncher.launch(intent)
        return true
    }

    private inner class AppItem(val info: ApplicationInfo) {
        val icon: Drawable = info.loadIcon(packageManager)
    }

    private inner class AppIconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.imageView)

        init {
            itemView.setOnClickListener { selectApp(mAppList[absoluteAdapterPosition]) }
        }
    }

    private inner class AppsAdapter : RecyclerView.Adapter<AppIconViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppIconViewHolder {
            return AppIconViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.app_icon_list_item, parent, false)
            )
        }

        override fun onBindViewHolder(holder: AppIconViewHolder, position: Int) {
            holder.icon.setImageDrawable(mAppList[position].icon)
        }

        override fun getItemCount(): Int {
            return mAppList.size
        }
    }

    override fun initView() {
        mApps = findViewById(R.id.apps)
        setupViews()
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"

        @JvmStatic
        @Deprecated("Use org/autojs/autojs/ui/shortcut/ShortcutIconSelectResult")
        fun getBitmapFromIntent(context: Context, data: Intent): Observable<Bitmap> {
            val packageName = data.getStringExtra(EXTRA_PACKAGE_NAME)
            if (packageName != null) {
                return Observable.fromCallable {
                    val drawable = context.packageManager.getApplicationIcon(packageName)
                    BitmapTool.drawableToBitmap(drawable)
                }
            }
            val uri = data.data
                ?: return Observable.error(IllegalArgumentException("invalid intent"))
            return Observable.fromCallable {
                BitmapFactory.decodeStream(
                    context.contentResolver.openInputStream(
                        uri
                    )
                )
            }
        }
    }
}