package org.autojs.autojs.ui.build;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatEditText;

import com.google.android.material.textfield.TextInputLayout;
import com.stardust.autojs.core.image.Colors;
import com.stardust.pio.PFile;
import com.stardust.pio.PFiles;

import org.autojs.autojs.Pref;
import org.autojs.autoxjs.R;
import org.autojs.autojs.build.ApkSigner;

import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder;

import static com.stardust.app.GlobalAppContext.getString;

public class SignKeyCreateDialogBuilder extends ThemeColorMaterialDialogBuilder {

    public interface SignKeyCreatedCallback {
        void onCreated(String path);
    }

    TextInputLayout mPathTip;
    AppCompatEditText mPath;
    AppCompatEditText mAlias;
    AppCompatEditText mPassword;
    AppCompatEditText mYear;
    AppCompatEditText mCountry;
    AppCompatEditText mName;
    AppCompatEditText mOrg;
    AppCompatEditText mUnit;
    AppCompatEditText mProvince;
    AppCompatEditText mCity;

    private String mKeySavePath;
    private String mKeyStoreDir;
    private SignKeyCreatedCallback mCallBack;

    public SignKeyCreateDialogBuilder(@NonNull Context context) {
        super(context);
        mKeyStoreDir = Pref.getKeyStorePath();
        autoDismiss(false);
        setupViews();
        setDefaultValue();
    }

    private void setupViews() {
        View view = View.inflate(context, R.layout.dialog_sign_key_create, null);
        mPathTip = view.findViewById(R.id.til_sign_key_pat);
        mPath = view.findViewById(R.id.ed_sign_key_path);
        mAlias = view.findViewById(R.id.ed_sign_key_alias);
        mPassword = view.findViewById(R.id.ed_sign_key_password);
        mYear = view.findViewById(R.id.ed_sign_key_year);
        mCountry = view.findViewById(R.id.ed_sign_key_country);
        mName = view.findViewById(R.id.ed_sign_key_name);
        mOrg = view.findViewById(R.id.ed_sign_key_org);
        mUnit = view.findViewById(R.id.ed_sign_key_unit);
        mProvince = view.findViewById(R.id.ed_sign_key_province);
        mCity = view.findViewById(R.id.ed_sign_key_city);

        customView(view, true);
        title(R.string.text_sign_key_add);
        negativeText(R.string.cancel);
        negativeColor(Colors.GRAY);
        positiveText(R.string.ok);
        onNegative((dialog, which) -> dialog.dismiss());
        onPositive((dialog, which) -> {
            if (createSignKey()) {
                dialog.dismiss();
            }
        });

    }

    private void setDefaultValue() {
        String keyPath = PFiles.getSimplifiedPath(mKeyStoreDir);
        mPathTip.setHint(getString(R.string.text_sign_hint_key_path).concat("(" + keyPath + ")"));
        mPath.setText("AutoX.keystore");
    }

    public SignKeyCreateDialogBuilder whenCreated(SignKeyCreatedCallback callback) {
        mCallBack = callback;
        return this;
    }

    private boolean createSignKey() {
        if (!checkInputs()) {
            return false;
        }
        String simplePath = mPath.getText().toString();
        mKeySavePath = mKeyStoreDir.concat(simplePath);
        PFile keyStore = new PFile(mKeySavePath);
        if (keyStore.exists()) {
            mPath.setError(getString(R.string.text_file_exists));
            return false;
        }
        String alias = mAlias.getText().toString();
        String password = mPassword.getText().toString();
        int year = Integer.parseInt(mYear.getText().toString());
        String country = mCountry.getText().toString();
        String name = mName.getText().toString();
        String org = mOrg.getText().toString();
        String unit = mUnit.getText().toString();
        String province = mProvince.getText().toString();
        String city = mCity.getText().toString();
        boolean success = ApkSigner.createKeyStore(mKeySavePath, alias, password, year, country, name, org, unit, province, city);
        if (success) {
            Toast.makeText(context, getString(R.string.text_sign_create_success), Toast.LENGTH_SHORT).show();
            Pref.setKeyStorePassWord(PFiles.getName(mKeySavePath), password);
            if (mCallBack != null) {
                mCallBack.onCreated(mKeySavePath);
                this.build().dismiss();
            }
        } else {
            Toast.makeText(context, getString(R.string.text_sign_create_fail), Toast.LENGTH_SHORT).show();
        }
        return success;
    }


    private boolean checkInputs() {
        boolean inputValid = true;
        inputValid &= checkNotEmpty(mPath);
        inputValid &= checkNotEmpty(mAlias);
        inputValid &= checkNotEmpty(mPassword);
        inputValid &= checkNotEmpty(mYear);
        inputValid &= checkNotEmpty(mCountry);
        return inputValid;
    }

    private boolean checkNotEmpty(AppCompatEditText editText) {
        if (!TextUtils.isEmpty(editText.getText()))
            return true;
        String hint = ((TextInputLayout) editText.getParent().getParent()).getHint().toString();
        editText.setError(hint + getString(R.string.text_should_not_be_empty));
        return false;
    }


    @Override
    public SignKeyCreateDialogBuilder title(@NonNull CharSequence title) {
        super.title(title);
        return this;
    }


    @Override
    public SignKeyCreateDialogBuilder title(@StringRes int titleRes) {
        super.title(titleRes);
        return this;
    }
}
