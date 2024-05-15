package org.autojs.autojs.ui.edit;

import android.content.Context;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.MaterialDialog;

import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder;
import org.autojs.autoxjs.R;

/**
 * Created by Stardust on 2018/2/24.
 */

public class TextSizeSettingDialogBuilder extends ThemeColorMaterialDialogBuilder implements SeekBar.OnSeekBarChangeListener {


    public interface PositiveCallback {

        void onPositive(int value);
    }

    private static final int MIN = 2;
    SeekBar mSeekBar;
    TextView mPreviewText;

    private int mTextSize;
    private MaterialDialog mMaterialDialog;

    public TextSizeSettingDialogBuilder(@NonNull Context context) {
        super(context);
        View view = View.inflate(context, R.layout.dialog_text_size_setting, null);
        customView(view, false);
        title(R.string.text_text_size);
        positiveText(R.string.ok);
        negativeText(R.string.cancel);
        mSeekBar = view.findViewById(R.id.seekbar);
        mPreviewText = view.findViewById(R.id.preview_text);
        mSeekBar.setOnSeekBarChangeListener(this);
    }

    private void setTextSize(int textSize) {
        mTextSize = textSize;
        String title = getContext().getString(R.string.text_size_current_value, textSize);
        if (mMaterialDialog != null) {
            mMaterialDialog.setTitle(title);
        } else {
            title(title);
        }
        mPreviewText.setTextSize(textSize);
    }

    public TextSizeSettingDialogBuilder initialValue(int value) {
        mSeekBar.setProgress(value - MIN);
        return this;
    }

    public TextSizeSettingDialogBuilder callback(PositiveCallback callback) {
        onPositive((dialog, which) -> callback.onPositive(mTextSize));
        return this;
    }

    @Override
    public MaterialDialog build() {
        mMaterialDialog = super.build();
        return mMaterialDialog;
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        setTextSize(progress + MIN);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
