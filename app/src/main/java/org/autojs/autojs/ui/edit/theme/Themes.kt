package org.autojs.autojs.ui.edit.theme

import android.annotation.SuppressLint
import android.content.Context
import com.stardust.pio.UncheckedIOException
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.autojs.autojs.Pref
import org.autojs.autojs.ui.util.isSystemNightMode
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*

/**
 * Created by Stardust on 2018/2/22.
 */
object Themes {
    private const val ASSETS_THEMES_PATH = "editor/theme"
    private const val DEFAULT_THEME = "Quiet Light"
    private const val DARK_THEME = "Dark (Visual Studio)"
    private var sThemes: MutableList<Theme>? = null
    private var sDefaultTheme: Theme? = null

    @SuppressLint("CheckResult")
    @JvmStatic
    fun getAllThemes(context: Context): Observable<List<Theme>?> {
        if (sThemes != null) {
            return Observable.just(sThemes)
        }
        val subject = PublishSubject.create<List<Theme>?>()
        getAllThemesInner(context)
            .subscribeOn(Schedulers.io())
            .subscribe({ themes: List<Theme>? ->
                themes?.let {
                    setThemes(it)
                    subject.onNext(sThemes!!)
                    subject.onComplete()
                }
            }) { obj: Throwable -> obj.printStackTrace() }
        return subject
    }

    fun getDefault(context: Context): Observable<Theme?> {
        return if (sDefaultTheme != null) Observable.just(sDefaultTheme) else getAllThemes(context)
            .map { sDefaultTheme }
    }

    @Synchronized
    private fun setThemes(themes: List<Theme>) {
        if (sThemes != null) return
        sThemes = Collections.unmodifiableList(themes)
        sThemes?.let {
            for (theme in it) {
                if (DEFAULT_THEME == theme.name) {
                    sDefaultTheme = theme
                    return
                }
            }
            sDefaultTheme = it[0]
        }
    }

    private fun getAllThemesInner(context: Context): Observable<MutableList<Theme>?> {
        return if (sThemes != null) {
            Observable.just(sThemes)
        } else try {
            Observable.fromArray(*context.assets.list(ASSETS_THEMES_PATH))
                .map { file: String? -> context.assets.open("$ASSETS_THEMES_PATH/$file") }
                .map { stream: InputStream? -> Theme.fromJson(InputStreamReader(stream)) }
                .collectInto(ArrayList<Theme>() as MutableList<Theme>) { obj: MutableList<Theme>?, e: Theme ->
                    obj!!.add(
                        e
                    )
                }
                .toObservable()
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }

    @JvmStatic
    fun getCurrent(context: Context): Observable<Theme?> {
        val currentTheme =
            (if (Pref.isNightModeEnabled() || context.isSystemNightMode()) DARK_THEME else Pref.getCurrentTheme())
                ?: return getDefault(context)
        return getAllThemes(context)
            .map { themes: List<Theme>? ->
                themes?.let {
                    for (theme in themes) {
                        if (currentTheme == theme.name) return@map theme
                    }
                    return@map themes[0]
                }
            }
    }

    @JvmStatic
    fun setCurrent(name: String?) {
        Pref.setCurrentTheme(name)
    }
}