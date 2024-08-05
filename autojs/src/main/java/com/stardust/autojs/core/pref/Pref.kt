package com.stardust.autojs.core.pref

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager;
import com.stardust.app.GlobalAppContext

object Pref {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(GlobalAppContext.get())
    val isStableModeEnabled: Boolean
        get() {
            return preferences.getBoolean("key_stable_mode", false)
        }

    val isGestureObservingEnabled: Boolean
        get() {
            return preferences.getBoolean("key_gesture_observing", false)
        }

    fun getDefault(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
}

object PrefKey {
    const val KEY_STABLE_MODE = "key_stable_mode"
    const val KEY_GESTURE_OBSERVING = "key_gesture_observing"
    const val KEY_AUTO_BACKUP = "key_auto_backup"
    const val KEY_FOREGROUND_SERVIE = "key_foreground_servie"
    const val KEY_USE_VOLUME_CONTROL_RECORD = "key_use_volume_control_record"
}