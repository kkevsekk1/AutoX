package org.autojs.autojs.tool

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.wifi.WifiManager
import android.text.format.Formatter
import androidx.core.content.getSystemService

/**
 * Created by Stardust on 2017/5/11.
 */
object WifiTool {
    fun getRouterIp(context: Context): String? {
        val wifiService = context.getSystemService<WifiManager>()!!
        val dhcpInfo = wifiService.dhcpInfo
        return dhcpInfo?.gateway?.let { Formatter.formatIpAddress(it) }
    }
}
