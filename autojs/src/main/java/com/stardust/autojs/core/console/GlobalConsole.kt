package com.stardust.autojs.core.console

import android.util.Log
import com.stardust.util.UiHandler
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Created by Stardust on 2017/10/22.
 */
open class GlobalConsole(uiHandler: UiHandler) : ConsoleImpl(uiHandler) {
    val publish = PublishSubject.create<String>()

    override fun println(level: Int, charSequence: CharSequence?): String {
        val log = "${DATE_FORMAT.format(Date())}/${getLevelChar(level)}: $charSequence"
        Log.d(TAG, log)
        super.println(level, log)
        publish.onNext(log)
        return log
    }

    fun createObservable(): Observable<String> {
        return publish.subscribeOn(Schedulers.single())
    }

    private fun getLevelChar(level: Int): String {
        return when (level) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.ASSERT -> "A"
            else -> ""
        }
    }

    companion object {

        private const val TAG = "GlobalConsole"
        private val DATE_FORMAT = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    }

    class ConsoleListener {

    }
}
