package org.autojs.autojs.ui.settings;

import android.annotation.SuppressLint;
import android.widget.TextView;
import android.widget.Toast;

import com.stardust.util.ClipboardUtil;
import com.stardust.util.IntentUtil;
import com.stardust.util.IntentUtilKt;
import com.tencent.bugly.crashreport.CrashReport;

import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder;
import org.autojs.autojs.ui.BaseActivity;
import org.autojs.autoxjs.BuildConfig;
import org.autojs.autoxjs.R;

/**
 * Created by Stardust on 2017/2/2.
 */
public class AboutActivity extends BaseActivity {

    private static final String TAG = "AboutActivity";
    TextView mVersion;

    private int mLolClickCount = 0;

    @Override
    protected void initView() {
        mVersion = findViewById(R.id.version);

        findViewById(R.id.developer).setOnClickListener(view -> {
            hhh();
        });
        findViewById(R.id.github).setOnClickListener(view -> {
            openGitHub();
        });

        findViewById(R.id.qq).setOnClickListener(view -> {
            openQQToChatWithMe();
        });

        findViewById(R.id.email).setOnClickListener(view -> {
            openEmailToSendMe();
        });

        findViewById(R.id.share).setOnClickListener(view -> {
            share();
        });

        findViewById(R.id.icon).setOnClickListener(view -> {
            lol();
        });

        setUpViews();
    }

    void setUpViews() {
        setVersionName();
        setToolbarAsBack(getString(R.string.text_about));
    }

    @SuppressLint("SetTextI18n")
    private void setVersionName() {
        mVersion.setText("Version " + BuildConfig.VERSION_NAME);
    }

    void openGitHub() {
        if (!IntentUtil.browse(this, getString(R.string.my_github))) {
            Toast.makeText(this, R.string.text_no_brower, Toast.LENGTH_SHORT).show();
        }
    }

    void openQQToChatWithMe() {
        String qq = getString(R.string.qq);
        ClipboardUtil.setClip(this, qq);
        Toast.makeText(this, R.string.text_qq_already_copy_to_clip, Toast.LENGTH_SHORT).show();
        if (!IntentUtilKt.INSTANCE.launchQQ(this)) {
            Toast.makeText(this, R.string.text_mobile_qq_not_installed, Toast.LENGTH_SHORT).show();
        }
    }

    void openEmailToSendMe() {
        String email = getString(R.string.email);
        IntentUtil.sendMailTo(this, email);
    }

    void share() {
        IntentUtil.shareText(this, getString(R.string.share_app));
    }

    void lol() {
        mLolClickCount++;
        if (mLolClickCount >= 5) {
            mLolClickCount = 0;
            crashTest();
        }
    }

    private void crashTest() {
        new ThemeColorMaterialDialogBuilder(this).title("Crash Test").positiveText("Crash").onPositive((dialog, which) -> CrashReport.testJavaCrash()).show();
    }

    void hhh() {
        Toast.makeText(this, R.string.text_it_is_the_developer_of_app, Toast.LENGTH_LONG).show();
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_about;
    }
}
