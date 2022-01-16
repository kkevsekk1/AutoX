package org.autojs.autojs.ui.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.preference.EditTextPreferenceDialogFragmentCompat;

import com.afollestad.materialdialogs.MaterialDialog;

import org.autojs.autojs.Pref;
import org.autojs.autojs.R;
import org.autojs.autojs.model.explorer.Explorers;
import org.autojs.autojs.storage.file.FileObservable;
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder;
import org.autojs.autojs.tool.SimpleObserver;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * create by aaron 2022年1月16日
 */

public class ScriptDirPathPreferenceFragmentCompat extends EditTextPreferenceDialogFragmentCompat {


    private static final String TAG ="ScriptDirPathPreference" ;
    private RadioGroup mRadioGroup;

    public static ScriptDirPathPreferenceFragmentCompat newInstance(
            String key) {
        final ScriptDirPathPreferenceFragmentCompat
                fragment = new ScriptDirPathPreferenceFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mRadioGroup = view.findViewById(R.id.script_operate);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        String oldPath = Pref.getScriptDirPath();
        super.onDialogClosed(positiveResult);
        Log.e(TAG, "onDialogClosed: "+positiveResult);
        if (!positiveResult) {
            return;
        }
        String newPath = Pref.getScriptDirPath();
        if (TextUtils.equals(oldPath, newPath)) {
            return;
        }
        int id = mRadioGroup.getCheckedRadioButtonId();
        if (id == R.id.none) {
            Explorers.workspace().refreshAll();
            return;
        }
        Observable<File> fileObservable;
        if (id == R.id.copy) {
            fileObservable = FileObservable.copy(oldPath, newPath);
        } else {
            fileObservable = FileObservable.move(oldPath, newPath);
        }
        showFileProgressDialog(fileObservable);
    }

    private void showFileProgressDialog(Observable<File> observable) {
        MaterialDialog dialog = new ThemeColorMaterialDialogBuilder(getContext())
                .progress(true, 0)
                .progressIndeterminateStyle(true)
                .title(R.string.text_on_progress)
                .cancelable(false)
                .content("")
                .show();
        observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<File>() {

                    @Override
                    public void onNext(File file) {
                        dialog.setContent(file.getPath());
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        dialog.dismiss();
                        Explorers.workspace().refreshAll();
                        Toast.makeText(getContext(), getContext().getString(R.string.text_error_copy_file,
                                e.getMessage()), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onComplete() {
                        dialog.dismiss();
                        Explorers.workspace().refreshAll();
                    }
                });
    }

}
