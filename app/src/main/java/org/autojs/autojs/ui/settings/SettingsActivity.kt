package org.autojs.autojs.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.util.Pair
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.stardust.pio.PFiles.read
import com.stardust.theme.app.ColorSelectActivity
import com.stardust.theme.app.ColorSelectActivity.ColorItem
import com.stardust.theme.util.ListBuilder
import de.psdev.licensesdialog.LicensesDialog
import org.autojs.autojs.ui.settings.LicenseInfo.install
import org.autojs.autojs.ui.widget.CommonMarkdownView.DialogBuilder
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

    class PreferenceFragment : PreferenceFragmentCompat() {
        private val ACTION_MAP = mutableMapOf<String, (activity: Activity) -> Unit>()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            ACTION_MAP.apply {
                //.put(getString(R.string.text_theme_color), () -> selectThemeColor(getActivity()))
                // .put(getString(R.string.text_check_for_updates), () -> new UpdateCheckDialog(getActivity()).show())
                // .put(getString(R.string.text_issue_report), () -> startActivity(new Intent(getActivity(), IssueReporterActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)))
                put(getString(R.string.text_about_me_and_repo)) {
                    it.startActivity(
                        Intent(it, AboutActivity_::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                put(getString(R.string.text_licenses)) { showLicenseDialog(it) }
                put(getString(R.string.text_licenses_other)) { showLicenseDialog2(it) }
            }

        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.preferences)
        }

        override fun onDisplayPreferenceDialog(preference: Preference) {
            var dialogFragment: DialogFragment? = null
            if (preference is ScriptDirPathPreference) {
                ScriptDirPathPreferenceFragmentCompat.newInstance(preference.getKey())?.let {
                    it.setTargetFragment(this, 1234)
                    it.show(
                        this.parentFragmentManager,
                        "androidx.preference.PreferenceFragment.DIALOG1"
                    )
                    return
                }
            }
            super.onDisplayPreferenceDialog(preference)
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            val action = ACTION_MAP[preference.title.toString()]
            val activity = requireActivity()
            return if (action != null) {
                action(activity)
                true
            } else {
                super.onPreferenceTreeClick(preference)
            }
        }

        companion object {
            private fun showLicenseDialog(context: Context) {
                LicensesDialog.Builder(context)
                    .setNotices(R.raw.licenses)
                    .setIncludeOwnLicense(true)
                    .build()
                    .show()
            }

            private fun showLicenseDialog2(context: Context) {
                DialogBuilder(context)
                    .padding(36, 0, 36, 0)
                    .markdown(read(context.resources.openRawResource(R.raw.licenses_other)))
                    .title(R.string.text_licenses_other)
                    .positiveText(R.string.ok)
                    .canceledOnTouchOutside(false)
                    .show()
            }
        }
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
