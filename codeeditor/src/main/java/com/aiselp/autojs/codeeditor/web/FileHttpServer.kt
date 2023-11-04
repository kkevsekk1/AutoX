package com.aiselp.autojs.codeeditor.web

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.yanzhenjie.andserver.AndServer
import com.yanzhenjie.andserver.Server
import com.yanzhenjie.andserver.annotation.Config
import com.yanzhenjie.andserver.framework.config.WebConfig
import com.yanzhenjie.andserver.framework.website.StorageWebsite
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.TimeUnit
import kotlin.random.Random


class FileHttpServer(context: Context, path: File) {
    companion object {
        fun isPortAvailable(port: Int): Boolean {
            try {
                ServerSocket(port).use {
                    return true // 端口可用
                }
            } catch (e: IOException) {
                return false // 端口已被占用
            }
        }
    }

    private val port = getPort()
    val server: Server = AndServer.webServer(context)
        .port(port)
        .timeout(10, TimeUnit.SECONDS)
        .build();

    private fun getPort(): Int {
        while (true) {
            val port = Random.nextInt(2000, 50000)
            if (isPortAvailable(port)) {
                return port
            }
        }
    }
    fun getAddress(): String {
        return "http://127.0.0.1:$port"
    }
    fun start(){
        Log.d("FileHttpServer", "FileHttpServer init host: 127.0.0.1 port: $port")
        server.startup()
    }
    fun stop(){
        server.shutdown()
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Config
class AppConfig : WebConfig {
    override fun onConfig(context: Context, delegate: WebConfig.Delegate) {
        // 增加一个位于/sdcard/Download/AndServer/目录的网站
        delegate.addWebsite(
            StorageWebsite(
                File(
                    context.filesDir, "${EditorAppManager.WEB_PUBLIC_PATH}/dist"
                ).path
            )
        )
    }
}