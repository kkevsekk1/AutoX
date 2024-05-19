package org.autojs.autojs.ui.settings

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.stardust.pio.PFiles
import de.psdev.licensesdialog.LicensesDialog
import org.autojs.autojs.external.open.RunIntentActivity
import org.autojs.autojs.ui.util.launchActivity
import org.autojs.autojs.ui.widget.CommonMarkdownView
import org.autojs.autoxjs.R

class PreferenceFragment : PreferenceFragmentCompat() {
    private val ACTION_MAP = mutableMapOf<String, (activity: Activity) -> Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ACTION_MAP.apply {
            //.put(getString(R.string.text_theme_color), () -> selectThemeColor(getActivity()))
            // .put(getString(R.string.text_check_for_updates), () -> new UpdateCheckDialog(getActivity()).show())
            // .put(getString(R.string.text_issue_report), () -> startActivity(new Intent(getActivity(), IssueReporterActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)))
            put(getString(R.string.text_about_me_and_repo)) {
                it.launchActivity<AboutActivity> {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            put(getString(R.string.text_licenses)) { showLicenseDialog(it) }
            put(getString(R.string.text_licenses_other)) { showLicenseDialog2(it) }
        }

    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
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
        if (preference.title == getString(R.string.text_intent_run_script)) {
            val state = if ((preference as SwitchPreference).isChecked) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            activity.packageManager.setComponentEnabledSetting(
                ComponentName(activity, RunIntentActivity::class.java),
                state,
                PackageManager.DONT_KILL_APP
            );
            return true
        }
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
            CommonMarkdownView.DialogBuilder(context)
                .padding(36, 0, 36, 0)
                .markdown(PFiles.read(context.resources.openRawResource(R.raw.licenses_other)))
                .title(R.string.text_licenses_other)
                .positiveText(R.string.ok)
                .canceledOnTouchOutside(false)
                .show()
        }
    }
}