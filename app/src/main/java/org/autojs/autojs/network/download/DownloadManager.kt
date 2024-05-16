package org.autojs.autojs.network.download

import android.content.Context
import android.util.Log
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.stardust.concurrent.VolatileBox
import com.stardust.pio.PFiles.ensureDir
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.autojs.autojs.network.NodeBB
import org.autojs.autojs.network.api.DownloadApi
import org.autojs.autojs.tool.SimpleObserver
import org.autojs.autoxjs.R
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by Stardust on 2017/10/20.
 */
class DownloadManager {
    private val mRetrofit: Retrofit
    private val mDownloadApi: DownloadApi
    private val mDownloadStatuses = ConcurrentHashMap<String, VolatileBox<Boolean>>()

    init {
        mRetrofit = Retrofit.Builder()
            .baseUrl(NodeBB.BASE_URL)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                        val request = chain.request()
                        var response = chain.proceed(request)
                        var tryCount = 0
                        while (!response.isSuccessful && tryCount < RETRY_COUNT) {
                            tryCount++
                            response = chain.proceed(request)
                        }
                        response
                    })
                    .build()
            )
            .build()
        mDownloadApi = mRetrofit.create(DownloadApi::class.java)
    }


    fun download(url: String, path: String?): Observable<Int> {
        val task = DownloadTask(url, path)
        mDownloadApi.download(url)
            .subscribeOn(Schedulers.io())
            .subscribe({ body: ResponseBody -> task.start(body) }, { error: Throwable? ->
                task.progress().onError(
                    error!!
                )
            })
        return task.progress()
    }

    fun downloadWithProgress(context: Context, url: String, path: String): Observable<File> {
        val fileName = parseFileNameLocally(url)
        return download(url, path, createDownloadProgressDialog(context, url, fileName))
    }

    private fun createDownloadProgressDialog(
        context: Context,
        url: String,
        fileName: String
    ): MaterialDialog {
        return MaterialDialog.Builder(context)
            .progress(false, 100)
            .title(fileName)
            .cancelable(false)
            .positiveText(R.string.text_cancel_download)
            .onPositive { dialog: MaterialDialog?, which: DialogAction? ->
                instance.cancelDownload(
                    url
                )
            }
            .show()
    }

    private fun download(
        url: String,
        path: String,
        progressDialog: MaterialDialog
    ): Observable<File> {
        val subject = PublishSubject.create<File>()
        instance.download(url, path)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { progress: Int? ->
                progressDialog.setProgress(
                    progress!!
                )
            }
            .subscribe(object : SimpleObserver<Int?>() {
                override fun onComplete() {
                    progressDialog.dismiss()
                    subject.onNext(File(path))
                    subject.onComplete()
                }

                override fun onError(error: Throwable) {
                    Log.e(LOG_TAG, "Download failed", error)
                    progressDialog.dismiss()
                    subject.onError(error)
                }
            })
        return subject
    }

    fun cancelDownload(url: String) {
        val status = mDownloadStatuses[url]
        status?.set(false)
    }

    private inner class DownloadTask(private val mUrl: String, private val mPath: String?) {
        private val mStatus = VolatileBox(true)
        private var mInputStream: InputStream? = null
        private var mFileOutputStream: FileOutputStream? = null
        private val mProgress: PublishSubject<Int>

        init {
            val previous = mDownloadStatuses.put(mUrl, mStatus)
            previous?.set(false)
            mProgress = PublishSubject.create()
        }

        @Throws(IOException::class)
        private fun startImpl(body: ResponseBody) {
            val buffer = ByteArray(4096)
            mFileOutputStream = FileOutputStream(mPath)
            mInputStream = body.byteStream()
            val total = body.contentLength()
            var read: Long = 0
            while (true) {
                if (!mStatus.get()) {
                    onCancel()
                    return
                }
                val len = mInputStream!!.read(buffer)
                if (len == -1) {
                    break
                }
                read += len.toLong()
                mFileOutputStream!!.write(buffer, 0, len)
                if (total > 0) {
                    mProgress.onNext((100 * read / total).toInt())
                }
            }
            mProgress.onComplete()
            recycle()
        }

        fun start(body: ResponseBody) {
            try {
                ensureDir(mPath!!)
                startImpl(body)
            } catch (e: Exception) {
                mProgress.onError(e)
            }
        }

        @Throws(IOException::class)
        private fun onCancel() {
            recycle()
        }

        fun recycle() {
            mDownloadStatuses.remove(mUrl)
            if (mInputStream != null) {
                try {
                    mInputStream!!.close()
                } catch (ignored: IOException) {
                }
            }
            if (mFileOutputStream != null) {
                try {
                    mFileOutputStream!!.close()
                } catch (ignored: IOException) {
                }
            }
        }

        fun progress(): PublishSubject<Int> {
            return mProgress
        }
    }

    companion object {
        private const val LOG_TAG = "DownloadManager"
        private var sInstance: DownloadManager? = null

        private const val RETRY_COUNT = 3
        val instance: DownloadManager
            get() {
                if (sInstance == null) {
                    sInstance = DownloadManager()
                }
                return sInstance!!
            }


        fun parseFileNameLocally(url: String): String {
            var i = url.lastIndexOf('-')
            if (i < 0) {
                i = url.lastIndexOf('/')
            }
            return URLDecoder.decode(url.substring(i + 1))
        }
    }
}
