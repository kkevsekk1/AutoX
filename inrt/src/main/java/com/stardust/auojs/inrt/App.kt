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
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.fanjun.keeplive.KeepLive
import com.fanjun.keeplive.config.ForegroundNotification
import com.fanjun.keeplive.config.ForegroundNotificationClickListener
import com.linsh.utilseverywhere.Utils
import com.stardust.app.GlobalAppContext
import com.stardust.auojs.inrt.autojs.AutoJs
import com.stardust.auojs.inrt.autojs.GlobalKeyObserver
import com.stardust.auojs.inrt.pluginclient.AutoXKeepLiveService
import com.stardust.autojs.core.ui.inflater.ImageLoader
import com.stardust.autojs.core.ui.inflater.util.Drawables
import com.stardust.autojs.execution.ScriptExecuteActivity


/**
 * Created by Stardust on 2017/7/1.
 */

class App : Application() {

    var TAG = "inrt.application";
    override fun onCreate() {
        super.onCreate()
        GlobalAppContext.set(this)
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
                            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>) {
                                view.background = resource
                            }
                        })
            }

            override fun load(view: View, uri: Uri): Drawable {
                throw UnsupportedOperationException()
            }

            override fun load(view: View, uri: Uri, drawableCallback: ImageLoader.DrawableCallback) {
                Glide.with(this@App)
                        .load(uri)
                        .into(object : SimpleTarget<Drawable>() {
                            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>) {
                                drawableCallback.onLoaded(resource)
                            }
                        })
            }

            override fun load(view: View, uri: Uri, bitmapCallback: ImageLoader.BitmapCallback) {
                Glide.with(this@App)
                        .asBitmap()
                        .load(uri)
                        .into(object : SimpleTarget<Bitmap>() {
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>) {
                                bitmapCallback.onLoaded(resource)
                            }
                        })
            }
        })

        val foregroundNotification = ForegroundNotification(GlobalAppContext.getAppName(), "点击打开【" + GlobalAppContext.getAppName()+ "】", R.mipmap.ic_launcher,  //定义前台服务的通知点击事件
                object : ForegroundNotificationClickListener {
                    override fun foregroundNotificationClick(context: Context?, intent: Intent?) {
                        Log.d(TAG, "foregroundNotificationClick: ");
                        val splashActivityintent = Intent(context, ScriptExecuteActivity::class.java)
                        splashActivityintent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                        context!!.startActivity(splashActivityintent)

                    }
                })
          //启动保活服务
            KeepLive.useSilenceMusice = false;
            KeepLive.startWork(this, KeepLive.RunMode.ENERGY, foregroundNotification, AutoXKeepLiveService());
            if(BuildConfig.isMarket){
                showNotification(this);
           }
    }

    private fun showNotification(context: Context) {
        var manager: NotificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        var builder: Notification.Builder = Notification.Builder(context)
        builder.setWhen(System.currentTimeMillis())
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(GlobalAppContext.getAppName() + "保持运行中")
                .setContentText("点击打开【" + GlobalAppContext.getAppName()+ "】")
                .setDefaults(NotificationCompat.FLAG_ONGOING_EVENT)
                .setPriority(Notification.PRIORITY_MAX)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { //SDK版本>=21才能设置悬挂式通知栏
            builder.setCategory(Notification.FLAG_ONGOING_EVENT.toString())
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
            val intent = Intent(context, SplashActivity::class.java)
            val pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            builder.setContentIntent(pi)
            manager.notify(null, 0, builder.build())
        }
    }

}
