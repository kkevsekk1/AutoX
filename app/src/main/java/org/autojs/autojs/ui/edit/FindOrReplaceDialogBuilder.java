package org.autojs.autojs.ui.edit;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.MaterialDialog;

import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder;
import org.autojs.autojs.ui.edit.editor.CodeEditor;
import org.autojs.autoxjs.R;

/**
 * Created by Stardust on 2017/9/28.
 */

public class FindOrReplaceDialogBuilder extends ThemeColorMaterialDialogBuilder {

    private static final String KEY_KEYWORDS = "...";

    private CheckBox mRegexCheckBox;

    private CheckBox mReplaceCheckBox;

    private CheckBox mReplaceAllCheckBox;

    private EditText mKeywordsEditText;

    private EditText mReplacementEditText;

    private final EditorView mEditorView;

    public FindOrReplaceDialogBuilder(@NonNull Context context, EditorView editorView) {
        super(context);
        mEditorView = editorView;
        setupViews();
        restoreState();
        autoDismiss(false);
        onNegative((dialog, which) -> dialog.dismiss());
        onPositive((dialog, which) -> {
            storeState();
            findOrReplace(dialog);
        });
    }

    private void setupViews() {
        View view = View.inflate(context, R.layout.dialog_find_or_replace, null);
        bindView(view);
        customView(view, true);
        positiveText(R.string.ok);
        negativeText(R.string.cancel);
        title(R.string.text_find_or_replace);
    }


    private void storeState() {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                .putString(KEY_KEYWORDS, mKeywordsEditText.getText().toString())
                .apply();
    }


    private void restoreState() {
        mKeywordsEditText.setText(PreferenceManager.getDefaultSharedPreferences(getContext())
                .getString(KEY_KEYWORDS, ""));
    }

    void syncWithReplaceCheckBox() {
        if (mReplaceAllCheckBox.isChecked() && !mReplaceCheckBox.isChecked()) {
            mReplaceCheckBox.setChecked(true);
        }
    }

    void onTextChanged() {
        if (mReplacementEditText.getText().length() > 0) {
            mReplaceCheckBox.setChecked(true);
        }
    }

    private void findOrReplace(MaterialDialog dialog) {
        String keywords = mKeywordsEditText.getText().toString();
        if (keywords.isEmpty()) {
            return;
        }
        try {
            boolean usingRegex = mRegexCheckBox.isChecked();
            if (!mReplaceCheckBox.isChecked()) {
                mEditorView.find(keywords, usingRegex);
            } else {
                String replacement = mReplacementEditText.getText().toString();
                if (mReplaceAllCheckBox.isChecked()) {
                    mEditorView.replaceAll(keywords, replacement, usingRegex);
                } else {
                    mEditorView.replace(keywords, replacement, usingRegex);
                }
            }
            dialog.dismiss();
        } catch (CodeEditor.CheckedPatternSyntaxException e) {
            e.printStackTrace();
            mKeywordsEditText.setError(getContext().getString(R.string.error_pattern_syntax));
        }

    }

    public FindOrReplaceDialogBuilder setQueryIfNotEmpty(String s) {
        if (!TextUtils.isEmpty(s))
            mKeywordsEditText.setText(s);
        return this;
    }

    private void bindView(@NonNull View bindSource) {
        mRegexCheckBox = bindSource.findViewById(R.id.checkbox_regex);
        mReplaceCheckBox = bindSource.findViewById(R.id.checkbox_replace);
        mReplaceAllCheckBox = bindSource.findViewById(R.id.checkbox_replace_all);
        mKeywordsEditText = bindSource.findViewById(R.id.keywords);
        mReplacementEditText = bindSource.findViewById(R.id.replacement);
        mReplacementEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mReplacementEditText.setText(s.toString());
                FindOrReplaceDialogBuilder.this.onTextChanged();
            }
        });
        mReplaceAllCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mReplaceAllCheckBox.setChecked(isChecked);
                syncWithReplaceCheckBox();
            }
        });
    }
}
