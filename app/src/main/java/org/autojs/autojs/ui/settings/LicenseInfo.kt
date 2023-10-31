package org.autojs.autojs.ui.settings

import android.content.Context
import de.psdev.licensesdialog.LicenseResolver
import de.psdev.licensesdialog.licenses.License
import org.autojs.autoxjs.R

object LicenseInfo {
    fun install(){
        LicenseResolver.registerLicense(MozillaPublicLicense20())
    }

}
class MozillaPublicLicense20 : License() {
    override fun getName(): String {
        return "Mozilla Public License 2.0"
    }
    override fun readSummaryTextFromResources(context: Context): String {
        return getContent(context, R.raw.mpl_20_summary)
    }

    override fun readFullTextFromResources(context: Context): String {
        return getContent(context, R.raw.mpl_20_full)
    }

    override fun getVersion(): String {
        return "2.0"
    }

    override fun getUrl(): String {
        return "https://www.mozilla.org/en-US/MPL/2.0/"
    }
}