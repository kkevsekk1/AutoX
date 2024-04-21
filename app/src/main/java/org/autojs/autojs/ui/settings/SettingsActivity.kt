package org.autojs.autojs.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.util.Pair
import com.stardust.theme.app.ColorSelectActivity
import com.stardust.theme.app.ColorSelectActivity.ColorItem
import com.stardust.theme.util.ListBuilder
import org.autojs.autojs.ui.settings.LicenseInfo.install
import org.autojs.autoxjs.R


/**
 * Created by Stardust on 2017/2/2.
 * update by aaron 2022年1月16日
 */
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setUpUI()
    }

    private fun setUpUI() {
        setUpToolbar()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_setting, PreferenceFragment()).commit()
    }

    private fun setUpToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setTitle(R.string.text_setting)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
    }


    companion object {
        init {
            install()
        }

        private val COLOR_ITEMS = ListBuilder<Pair<Int, Int>>()
            .add(Pair(R.color.theme_color_red, R.string.theme_color_red))
            .add(Pair(R.color.theme_color_pink, R.string.theme_color_pink))
            .add(Pair(R.color.theme_color_purple, R.string.theme_color_purple))
            .add(Pair(R.color.theme_color_dark_purple, R.string.theme_color_dark_purple))
            .add(Pair(R.color.theme_color_indigo, R.string.theme_color_indigo))
            .add(Pair(R.color.theme_color_blue, R.string.theme_color_blue))
            .add(Pair(R.color.theme_color_light_blue, R.string.theme_color_light_blue))
            .add(Pair(R.color.theme_color_blue_green, R.string.theme_color_blue_green))
            .add(Pair(R.color.theme_color_cyan, R.string.theme_color_cyan))
            .add(Pair(R.color.theme_color_green, R.string.theme_color_green))
            .add(Pair(R.color.theme_color_light_green, R.string.theme_color_light_green))
            .add(Pair(R.color.theme_color_yellow_green, R.string.theme_color_yellow_green))
            .add(Pair(R.color.theme_color_yellow, R.string.theme_color_yellow))
            .add(Pair(R.color.theme_color_amber, R.string.theme_color_amber))
            .add(Pair(R.color.theme_color_orange, R.string.theme_color_orange))
            .add(Pair(R.color.theme_color_dark_orange, R.string.theme_color_dark_orange))
            .add(Pair(R.color.theme_color_brown, R.string.theme_color_brown))
            .add(Pair(R.color.theme_color_gray, R.string.theme_color_gray))
            .add(Pair(R.color.theme_color_blue_gray, R.string.theme_color_blue_gray))
            .list()

        fun selectThemeColor(context: Context) {
            val colorItems: MutableList<ColorItem> = ArrayList(COLOR_ITEMS.size)
            for (item in COLOR_ITEMS) {
                colorItems.add(
                    ColorItem(
                        context.getString(item.second),
                        context.resources.getColor(item.first)
                    )
                )
            }
            ColorSelectActivity.startColorSelect(
                context,
                context.getString(R.string.mt_color_picker_title),
                colorItems
            )
        }
    }
}
