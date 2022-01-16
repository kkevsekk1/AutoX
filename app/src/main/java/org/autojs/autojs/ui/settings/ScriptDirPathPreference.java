package org.autojs.autojs.ui.settings;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.EditTextPreference;

import org.autojs.autojs.R;

/**
 * create by aaron 2022年1月16日
 */
public class ScriptDirPathPreference extends EditTextPreference {
    public ScriptDirPathPreference(Context context) {
        super(context);
    }
    public ScriptDirPathPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public ScriptDirPathPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public ScriptDirPathPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
    @Override
    public int getDialogLayoutResource() {
        return  R.layout.script_dir_pref_radio_group;
    }

}

