package org.autojs.autojs.build

import android.content.Context
import java.io.IOException
import java.io.InputStream

/**
 * Created by Stardust on 2017/11/29.
 * Modified by wilinz on 2022/5/23
 */
object ApkBuilderPluginHelper {
    private const val TEMPLATE_APK_PATH = "template.apk"
    fun openTemplateApk(context: Context): InputStream? {
        try {
            return context.assets.open(TEMPLATE_APK_PATH)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}