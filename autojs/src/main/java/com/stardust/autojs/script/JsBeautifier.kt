package com.stardust.autojs.script

import android.content.Context
import android.view.View
import com.stardust.autojs.engine.module.AssetAndUrlModuleSourceProvider
import com.stardust.pio.PFiles.join
import com.stardust.pio.PFiles.read
import com.stardust.pio.UncheckedIOException
import org.mozilla.javascript.Function
import org.mozilla.javascript.ImporterTopLevel
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.commonjs.module.RequireBuilder
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors

/**
 * Created by Stardust on 2017/4/12.
 */
class JsBeautifier(view: View, beautifyJsDirPath: String) {
    interface Callback {
        fun onSuccess(beautifiedCode: String?)
        fun onException(e: Exception?)
    }

    private val mExecutor = Executors.newSingleThreadExecutor()
    private val mContext: Context
    private var mJsBeautifyFunction: Function? = null
    private var mScriptContext: org.mozilla.javascript.Context? = null
    private var mScriptable: Scriptable? = null
    private val mBeautifyJsPath: String
    private val mBeautifyJsDir: String
    private var mView: View?
    fun beautify(code: String, callback: Callback) {
        mExecutor.execute {
            try {
                prepareIfNeeded()
                enterContext()
                val beautifiedCode = mJsBeautifyFunction!!.call(
                    mScriptContext,
                    mScriptable,
                    mScriptable,
                    arrayOf<Any>(code)
                )
                mView!!.post { callback.onSuccess(beautifiedCode.toString()) }
            } catch (e: Exception) {
                mView!!.post { callback.onException(e) }
            } finally {
                exitContext()
            }
        }
    }

    private fun exitContext() {
        if (mScriptContext != null) {
            org.mozilla.javascript.Context.exit()
            mScriptContext = null
        }
    }

    private fun enterContext() {
        if (mScriptContext != null) {
            return
        }
        mScriptContext = org.mozilla.javascript.Context.enter()
        mScriptContext!!.languageVersion = org.mozilla.javascript.Context.VERSION_1_8
        mScriptContext!!.optimizationLevel = -1
        if (mScriptable == null) {
            val importerTopLevel = ImporterTopLevel()
            importerTopLevel.initStandardObjects(mScriptContext, false)
            mScriptable = importerTopLevel
        }
        val provider =
            AssetAndUrlModuleSourceProvider(mContext, mBeautifyJsDir, listOf(File("/").toURI()))
        RequireBuilder()
            .setModuleScriptProvider(SoftCachingModuleScriptProvider(provider))
            .setSandboxed(false)
            .createRequire(mScriptContext, mScriptable)
            .install(mScriptable)
    }

    fun prepare() {
        mExecutor.execute {
            try {
                prepareIfNeeded()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun prepareIfNeeded() {
        if (mJsBeautifyFunction != null) return
        compile()
    }

    private fun compile() {
        mJsBeautifyFunction = try {
            enterContext()
            val `is` = mContext.assets.open(mBeautifyJsPath)
            mScriptContext!!.evaluateString(
                mScriptable,
                read(`is`),
                "<js_beautify>",
                1,
                null
            ) as Function
        } catch (e: IOException) {
            exitContext()
            throw UncheckedIOException(e)
        }
    }

    fun shutdown() {
        mExecutor.shutdownNow()
        mView = null
    }

    init {
        mContext = view.context
        mView = view
        mBeautifyJsDir = beautifyJsDirPath
        mBeautifyJsPath = join(beautifyJsDirPath, "beautify.js")
    }
}