package org.autojs.autojs.ui.widget

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.text.InputFilter
import android.util.AttributeSet
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.autojs.autojs.ui.widget.DownloadManagerUtil
import com.stardust.app.GlobalAppContext
import com.stardust.app.OnActivityResultDelegate
import com.stardust.app.OnActivityResultDelegate.DelegateHost
import com.stardust.autojs.execution.ScriptExecution
import com.stardust.autojs.script.StringScriptSource
import com.tencent.smtt.sdk.WebView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import org.autojs.autojs.R
import org.autojs.autojs.model.script.Scripts.run
import org.autojs.autojs.tool.ImageSelector
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by Stardust on 2017/8/22.
 */
open class EWebView : FrameLayout, SwipeRefreshLayout.OnRefreshListener, OnActivityResultDelegate {
    private lateinit var mWebView: com.tencent.smtt.sdk.WebView
    private lateinit var mProgressBar: ProgressBar
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    private lateinit var downloadManagerUtil: DownloadManagerUtil
    private var downloadId = 0L

    constructor(context: Context?) : super(context!!) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {
        init()
    }

    private fun init() {
        inflate(context, R.layout.ewebview, this)
        // 在调用TBS初始化、创建WebView之前进行如下配置
        val map: java.util.HashMap<String, Any> = HashMap<String, Any>()
        map[com.tencent.smtt.export.external.TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER] =
            true
        map[com.tencent.smtt.export.external.TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE] =
            true
        com.tencent.smtt.sdk.QbSdk.initTbsSettings(map)
        // 设置可调试
//        com.tencent.smtt.sdk.WebView.setWebContentsDebuggingEnabled(true)
        mWebView = com.tencent.smtt.sdk.WebView(context)
        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        mSwipeRefreshLayout.addView(
            mWebView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        mProgressBar = findViewById(R.id.progress_bar)
        mSwipeRefreshLayout.setOnRefreshListener(this)
        downloadManagerUtil = DownloadManagerUtil(GlobalAppContext.get())
        webInit(mWebView)
    }

    private fun webInit(mWebView: com.tencent.smtt.sdk.WebView) {
        if (Build.VERSION.SDK_INT >= 26) {
            mWebView.settings.safeBrowsingEnabled = false
        }
        with(mWebView.settings) {
            // JS相关
            javaScriptEnabled = true  //设置支持Javascript交互
            javaScriptCanOpenWindowsAutomatically = true //支持通过JS打开新窗口
            allowFileAccess = true //设置可以访问文件
            defaultTextEncodingName = "utf-8"//设置编码格式
            // 视图设置
            setSupportMultipleWindows(false)
            layoutAlgorithm = com.tencent.smtt.sdk.WebSettings.LayoutAlgorithm.NORMAL
            loadWithOverviewMode = true // 缩放至屏幕的大小
            setSupportZoom(true) //支持缩放，默认为true。是下面那个的前提。
            builtInZoomControls = true //设置内置的缩放控件。若为false，则该WebView不可缩放
            displayZoomControls = false //设置原生的缩放控件，启用时被leakcanary检测到内存泄露
            useWideViewPort = true //让WebView读取网页设置的viewport，pc版网页
            loadsImagesAutomatically = true //设置自动加载图片
            blockNetworkImage = false // 阻止网络图片加載, 测试发现此项(TBS045947)在启用TBS内核时失效
            // cache设置
            cacheMode = android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK //使用缓存
            domStorageEnabled = true // 开启 DOM storage API 功能
            databaseEnabled = true   //开启 database storage API 功能
        }
        mWebView!!.webViewClient = MyWebViewClient()
        mWebView!!.webChromeClient = MyWebChromeClient()
        mWebView.setDownloadListener { url: String, userAgent: String, contentDisposition: String, mimeType: String, contentLength: Long ->
            run {
                // 通过系统下载器下载（会在通知栏显示下载进度条）
                val fileName: String = URLUtil.guessFileName(
                    url, contentDisposition,
                    mimeType
                )
                //先移除上一个下载任务，防止重复下载
                // 此处为单个文件下载，downloadId为下载任务id,可根据业务调整
                if (downloadId != 0L) {
                    downloadManagerUtil.clearCurrentTask(downloadId)
                }
                downloadId = downloadManagerUtil.download(url, fileName, fileName)
                Toast.makeText(GlobalAppContext.get(), "正在后台下载：$fileName", Toast.LENGTH_LONG)
                    .show()
            }
        }
        mWebView.addJavascriptInterface(JsAPI(), "android")
    }

    fun evalJavaScript(script: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWebView!!.evaluateJavascript(script, null)
        } else {
            mWebView!!.loadUrl("javascript:$script")
        }
    }

    @SuppressLint("CheckResult")
    override fun onRefresh() {
        mSwipeRefreshLayout!!.isRefreshing = false
        mWebView!!.reload()
        Observable.timer(2, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t: Long? -> mSwipeRefreshLayout!!.isRefreshing = false }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {}

    protected open inner class MyWebViewClient() : com.tencent.smtt.sdk.WebViewClient() {
        //如果只是为了获取网页源代码的话，可以重写onPageFinished方法，在onPageFinished方法里执行相应的逻辑就好。但是当框架里显示的内容发生变化时，onPageFinished方法不会再掉用，只会调用onLoadResource方法，所以此处需要重写此方法。
        override fun onLoadResource(view: com.tencent.smtt.sdk.WebView, url: String?) {
            super.onLoadResource(view, url)
        }

        override fun onPageStarted(
            view: com.tencent.smtt.sdk.WebView,
            url: String,
            favicon: Bitmap?
        ) {
            super.onPageStarted(view, url, favicon)
//            view.settings.blockNetworkImage = true
            mProgressBar!!.progress = 0
            mProgressBar!!.visibility = VISIBLE
            if (url != null) {
                if ((url.startsWith("http") && url.indexOf("autoxjs.com") < 0) || (url.startsWith("file") && !url.startsWith(
                        "file:///android_asset"
                    ))
                ) {
                    var jsCode =
                        "javascript: " + readAssetsTxt(context, "modules/vconsole.min.js")
                    Log.i("onPageStarted", jsCode)
                    view.evaluateJavascript(
                        jsCode,
                        com.tencent.smtt.sdk.ValueCallback<String> {
                            Log.i("evaluateJavascript", "JS　return:  $it")
                        })
                }
            }
        }

        override fun onPageFinished(view: com.tencent.smtt.sdk.WebView, url: String) {
            view.settings.blockNetworkImage = false
            super.onPageFinished(view, url)
            mProgressBar!!.visibility = GONE
            mSwipeRefreshLayout!!.isRefreshing = false
        }

        override fun shouldOverrideUrlLoading(
            view: com.tencent.smtt.sdk.WebView,
            request: com.tencent.smtt.export.external.interfaces.WebResourceRequest
        ): Boolean {
            return shouldOverrideUrlLoading(view, request.url.toString())
        }

        override fun shouldOverrideUrlLoading(
            view: com.tencent.smtt.sdk.WebView,
            url: String
        ): Boolean {
            // 区分正规scheme和其它APP自定义的scheme
            if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://")) {
                view.loadUrl(url)
            } else if (url.indexOf("mobile_web") < 0) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            }
            return true
        }
    }

    protected open inner class MyWebChromeClient : com.tencent.smtt.sdk.WebChromeClient() {
        //设置响应js 的Alert()函数
        override fun onJsAlert(
            p0: com.tencent.smtt.sdk.WebView?,
            url: String?,
            message: String?,
            result: com.tencent.smtt.export.external.interfaces.JsResult?
        ): Boolean {
            val b: android.app.AlertDialog.Builder =
                android.app.AlertDialog.Builder(GlobalAppContext.get())
            b.setTitle("Alert")
            b.setMessage(message)
            b.setPositiveButton(
                R.string.ok,
                DialogInterface.OnClickListener { _, _ -> result?.confirm() })
            b.setCancelable(false)
            b.create().show()
            return true
        }

        //设置响应js 的Confirm()函数
        override fun onJsConfirm(
            p0: com.tencent.smtt.sdk.WebView?,
            url: String?,
            message: String?,
            result: com.tencent.smtt.export.external.interfaces.JsResult?
        ): Boolean {
            val b: android.app.AlertDialog.Builder =
                android.app.AlertDialog.Builder(GlobalAppContext.get())
            b.setTitle("Confirm")
            b.setMessage(message)
            b.setPositiveButton(
                R.string.ok,
                DialogInterface.OnClickListener { _, _ -> result?.confirm() })
            b.setNegativeButton(
                R.string.cancel,
                DialogInterface.OnClickListener { _, _ -> result?.cancel() })
            b.create().show()
            return true
        }

        //设置响应js 的Prompt()函数
        override fun onJsPrompt(
            p0: com.tencent.smtt.sdk.WebView?,
            url: String?,
            message: String?,
            defaultValue: String?,
            result: com.tencent.smtt.export.external.interfaces.JsPromptResult
        ): Boolean {
            val b: android.app.AlertDialog.Builder =
                android.app.AlertDialog.Builder(GlobalAppContext.get())
            val inputServer = EditText(GlobalAppContext.get())
            inputServer.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(255))
            inputServer.setText(defaultValue)
            b.setTitle("Prompt")
            b.setMessage(message)
            b.setView(inputServer)
            b.setPositiveButton(
                R.string.ok,
                DialogInterface.OnClickListener { _, _ ->
                    val value = inputServer.text.toString()
                    result?.confirm(value)
                })
            b.setNegativeButton(
                R.string.cancel,
                DialogInterface.OnClickListener { _, _ -> result.cancel() })
            b.create().show()
            return true
        }

        override fun onProgressChanged(view: com.tencent.smtt.sdk.WebView, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            mProgressBar!!.progress = newProgress
        }

        //For Android  >= 4.1
        fun openFileChooser(
            valueCallback: ValueCallback<Uri?>,
            acceptType: String?, capture: String?
        ) {
            if (acceptType == null) {
                openFileChooser(valueCallback, null)
            } else {
                openFileChooser(valueCallback, acceptType.split(",").toTypedArray())
            }
        }

        open fun openFileChooser(
            valueCallback: ValueCallback<Uri?>,
            acceptType: Array<String>?
        ): Boolean {
            if (context is DelegateHost &&
                context is Activity && isImageType(acceptType)
            ) {
                chooseImage(valueCallback)
                return true
            }
            return false
        }

        // For Android >= 5.0
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        override fun onShowFileChooser(
            webView: WebView?,
            p1: com.tencent.smtt.sdk.ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            if (fileChooserParams != null) {
                openFileChooser({ value: Uri? ->
                    if (value == null) {
                        p1?.onReceiveValue(null)
                    } else {
                        p1?.onReceiveValue(arrayOf(value))
                    }
                }, fileChooserParams.acceptTypes)
            }
            return true
        }
    }

    private fun chooseImage(valueCallback: ValueCallback<Uri?>) {
        val delegateHost = context as DelegateHost
        val mediator = delegateHost.onActivityResultDelegateMediator
        val activity = context as Activity
        ImageSelector(
            activity,
            mediator
        ) { selector: ImageSelector?, uri: Uri? -> valueCallback.onReceiveValue(uri) }
            .disposable()
            .select()
    }

    private fun isImageType(acceptTypes: Array<String>?): Boolean {
        if (acceptTypes == null) {
            return false
        }
        for (acceptType in acceptTypes) {
            for (imageType in IMAGE_TYPES) {
                if (acceptType.contains(imageType)) {
                    return true
                }
            }
        }
        return false
    }

    companion object {
        private val IMAGE_TYPES = Arrays.asList("png", "jpg", "bmp")
        private const val CHOOSE_IMAGE = 42222
    }

    fun getWebView(): com.tencent.smtt.sdk.WebView {
        return mWebView
    }

    fun getSwipeRefreshLayout(): SwipeRefreshLayout {
        return mSwipeRefreshLayout
    }

    internal class JsAPI {
        private var execution: ScriptExecution? = null

        @JavascriptInterface
        fun run(code: String?, name: String?) {
            stop(execution)
            execution = run(StringScriptSource(name, code))
        }

        @JavascriptInterface
        fun run(code: String?) {
            stop(execution)
            execution = run(StringScriptSource("", code))
        }

        @JavascriptInterface
        fun stop(execution: ScriptExecution?) {
            execution?.engine?.forceStop()
        }
    }

    fun readAssetsTxt(context: Context, fileName: String): String? {
        try {
            //Return an AssetManager instance for your application's package
            val `is`: InputStream = context.assets.open("$fileName")
            val size: Int = `is`.available()
            // Read the entire asset into a local byte buffer.
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            // Convert the buffer into a string.
            // Finally stick the string into the text view.
            return String(buffer, Charsets.UTF_8)
        } catch (e: IOException) {
            // Should never happen!
//            throw new RuntimeException(e);
            e.message?.let { Log.e("", it) }
        }
        return "读取错误，请检查文件名"
    }
}