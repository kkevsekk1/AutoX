package org.autojs.autojs.ui.settings;

import android.annotation.SuppressLint;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.stardust.util.ClipboardUtil;
import com.stardust.util.IntentUtil;
import com.stardust.util.IntentUtilKt;
import com.tencent.bugly.crashreport.CrashReport;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder;
import org.autojs.autojs.tool.IntentTool;
import org.autojs.autojs.ui.BaseActivity;
import org.autojs.autoxjs.BuildConfig;
import org.autojs.autoxjs.R;

/**
 * Created by Stardust on 2017/2/2.
 */
@EActivity(R.layout.activity_about)
public class AboutActivity extends BaseActivity {

    private static final String TAG = "AboutActivity";
    @ViewById(R.id.version)
    TextView mVersion;

    private int mLolClickCount = 0;


    @AfterViews
    void setUpViews() {
        setVersionName();
        setToolbarAsBack(getString(R.string.text_about));
    }

    @SuppressLint("SetTextI18n")
    private void setVersionName() {
        mVersion.setText("Version " + BuildConfig.VERSION_NAME);
    }

    @Click(R.id.github)
    void openGitHub() {
        IntentTool.browse(this, getString(R.string.my_github));
    }

    @Click(R.id.qq)
    void openQQToChatWithMe() {
        String qq = getString(R.string.qq);
        ClipboardUtil.setClip(this, qq);
        Toast.makeText(this, R.string.text_qq_already_copy_to_clip, Toast.LENGTH_SHORT).show();
        if (!IntentUtilKt.INSTANCE.launchQQ(this)) {
            Toast.makeText(this, R.string.text_mobile_qq_not_installed, Toast.LENGTH_SHORT).show();
        }
    }

    @Click(R.id.email)
    void openEmailToSendMe() {
        String email = getString(R.string.email);
        IntentUtil.sendMailTo(this, email);
    }

    @Click(R.id.share)
    void share() {
        IntentUtil.shareText(this, getString(R.string.share_app));
    }

    @Click(R.id.icon)
    void lol() {
        mLolClickCount++;
        if (mLolClickCount >= 5) {
            mLolClickCount = 0;
            crashTest();
        }
    }

    private void showEasterEgg() {
        new MaterialDialog.Builder(this).customView(R.layout.paint_layout, false).show();
    }

    private void crashTest() {
        new ThemeColorMaterialDialogBuilder(this).title("Crash Test").positiveText("Crash").onPositive((dialog, which) -> CrashReport.testJavaCrash()).show();
    }

    @Click(R.id.developer)
    void hhh() {
        Toast.makeText(this, R.string.text_it_is_the_developer_of_app, Toast.LENGTH_LONG).show();
    }

}
