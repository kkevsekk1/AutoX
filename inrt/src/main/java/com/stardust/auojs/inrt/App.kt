package com.stardust.auojs.inrt

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.fanjun.keeplive.KeepLive
import com.fanjun.keeplive.config.ForegroundNotification
import com.google.mlkit.common.MlKit
import com.linsh.utilseverywhere.Utils
import com.stardust.app.GlobalAppContext
import com.stardust.auojs.inrt.autojs.AutoJs
import com.stardust.auojs.inrt.autojs.GlobalKeyObserver
import com.stardust.auojs.inrt.pluginclient.AutoXKeepLiveService
import com.stardust.autojs.core.ui.inflater.ImageLoader
import com.stardust.autojs.core.ui.inflater.util.Drawables
import com.stardust.autojs.execution.ScriptExecuteActivity
import org.autojs.autoxjs.inrt.BuildConfig
import org.autojs.autoxjs.inrt.R


/**
 * Created by Stardust on 2017/7/1.
 */

class App : Application() {

    var TAG = "inrt.application";
    override fun onCreate() {
        super.onCreate()
        GlobalAppContext.set(
            this, com.stardust.app.BuildConfig.generate(BuildConfig::class.java)
        )
        MlKit.initialize(this)
        Utils.init(this);
        AutoJs.initInstance(this)
        GlobalKeyObserver.init()
        Drawables.setDefaultImageLoader(object : ImageLoader {
            override fun loadInto(imageView: ImageView, uri: Uri) {
                Glide.with(this@App)
                    .load(uri)
                    .into(imageView)
            }

            override fun loadIntoBackground(view: View, uri: Uri) {
                Glide.with(this@App)
                    .load(uri)
                    .into(object : SimpleTarget<Drawable>() {
                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable>
                        ) {
                            view.background = resource
                        }
                    })
            }

            override fun load(view: View, uri: Uri): Drawable {
                throw UnsupportedOperationException()
            }

            override fun load(
                view: View,
                uri: Uri,
                drawableCallback: ImageLoader.DrawableCallback
            ) {
                Glide.with(this@App)
                    .load(uri)
                    .into(object : SimpleTarget<Drawable>() {
                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable>
                        ) {
                            drawableCallback.onLoaded(resource)
                        }
                    })
            }

            override fun load(view: View, uri: Uri, bitmapCallback: ImageLoader.BitmapCallback) {
                Glide.with(this@App)
                    .asBitmap()
                    .load(uri)
                    .into(object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>
                        ) {
                            bitmapCallback.onLoaded(resource)
                        }
                    })
            }
        })

        //启动保活服务
        KeepLive.useSilenceMusice = false;
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this)
        val keepRunningWithForegroundService = sharedPreferences.getBoolean(
            getString(R.string.key_keep_running_with_foreground_service),
            false
        )
        if (keepRunningWithForegroundService) {
            val foregroundNotification = ForegroundNotification(
                GlobalAppContext.appName + "正在运行中",
                "点击打开【" + GlobalAppContext.appName + "】",
                R.mipmap.ic_launcher
            )  //定义前台服务的通知点击事件
            { context, intent ->
                Log.d(TAG, "foregroundNotificationClick: ");
                val splashActivityintent = Intent(context, ScriptExecuteActivity::class.java)
                splashActivityintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context!!.startActivity(splashActivityintent)
            }
            KeepLive.startWork(
                this,
                KeepLive.RunMode.ENERGY,
                foregroundNotification,
                AutoXKeepLiveService()
            );
        }

        if (BuildConfig.isMarket) {
            showNotification(this);
        }
    }

    private fun showNotification(context: Context) {
        val manager: NotificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val builder: Notification.Builder = Notification.Builder(context)
        builder.setWhen(System.currentTimeMillis())
            .setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(GlobalAppContext.appName + "保持运行中")
            .setContentText("点击打开【" + GlobalAppContext.appName + "】")
            .setDefaults(NotificationCompat.FLAG_ONGOING_EVENT)
            .setPriority(Notification.PRIORITY_MAX)
        //SDK版本>=21才能设置悬挂式通知栏
        builder.setCategory(Notification.FLAG_ONGOING_EVENT.toString())
            .setVisibility(Notification.VISIBILITY_PUBLIC)
        val intent = Intent(context, SplashActivity::class.java)
        val pi =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(pi)
        manager.notify(null, 0, builder.build())
    }

}
