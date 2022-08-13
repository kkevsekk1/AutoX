package org.autojs.autojs.network

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import io.reactivex.Observable
import okhttp3.OkHttpClient
import org.autojs.autoxjs.R
import org.autojs.autojs.network.api.ConfigApi
import org.autojs.autojs.network.util.WebkitCookieManagerProxy
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

/**
 * Created by Stardust on 2017/9/20.
 */
class NodeBB internal constructor() {
    private var mXCsrfToken: Map<String, String>? = null
    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(
            GsonConverterFactory.create(
                GsonBuilder()
                    .setLenient()
                    .create()
            )
        )
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory.invoke())
        .client(
            OkHttpClient.Builder()
                .cookieJar(WebkitCookieManagerProxy())
                .build()
        )
        .build()
    val xCsrfToken: Observable<Map<String, String>>
        get() = if (mXCsrfToken != null) {
            Observable.just(mXCsrfToken)
        } else {
            retrofit.create(ConfigApi::class.java)
                .config
                .map { config ->
                    mapOf("x-csrf-token" to  config.csrfToken)
                        .also {
                            mXCsrfToken = it
                        }
                }
        }

    fun invalidateXCsrfToken() {
        mXCsrfToken = null
    }

    companion object {
        const val BASE_URL = "http://www.autoxjs.com/"
        @JvmField
        val instance = NodeBB()
        private const val LOG_TAG = "NodeBB"
        fun getErrorMessage(e: Throwable?, context: Context, defaultMsg: String): String {
            if (e !is HttpException) {
                return defaultMsg
            }
            val httpException = e
            val body = httpException.response().errorBody() ?: return defaultMsg
            return try {
                val errorMessage = getErrorMessage(context, httpException, body.string())
                errorMessage ?: defaultMsg
            } catch (e1: IOException) {
                e1.printStackTrace()
                defaultMsg
            }
        }

        private fun getErrorMessage(
            context: Context,
            error: HttpException,
            errorBody: String?
        ): String? {
            if (errorBody == null) return null
            if (errorBody.contains("invalid-login-credentials")) {
                return context.getString(R.string.nodebb_error_invalid_login_credentials)
            }
            if (errorBody.contains("change_password_error_match")) {
                return context.getString(R.string.nodebb_error_change_password_error_match)
            }
            if (errorBody.contains("change_password_error_length")) {
                return context.getString(R.string.nodebb_error_change_password_error_length)
            }
            if (errorBody.contains("email-taken")) {
                return context.getString(R.string.nodebb_error_email_taken)
            }
            if (error.code() == 403) {
                return context.getString(R.string.nodebb_error_forbidden)
            }
            Log.d(LOG_TAG, "unknown error: $errorBody", error)
            return null
        }

        @JvmStatic
        fun url(relativePath: String): String {
            return BASE_URL + relativePath
        }

        @JvmStatic
        fun getErrorMessage(error: Throwable?, context: Context, defaultMsg: Int): CharSequence {
            return getErrorMessage(error, context, context.getString(defaultMsg))
        }
    }

}