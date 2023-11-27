package org.autojs.autojs.ui.error;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.heinrichreimersoftware.androidissuereporter.model.DeviceInfo;

import org.autojs.autojs.ui.BaseActivity;
import org.autojs.autojs.ui.main.MainActivity;
import org.autojs.autoxjs.R;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Stardust on 2017/2/2.
 */

public class ErrorReportActivity extends BaseActivity {
    private static final String TAG = "ErrorReportActivity";
    private static final SparseIntArray CRASH_COUNT = new SparseIntArray();
    private static final String KEY_CRASH_COUNT = "crash_count";

    static {
        CRASH_COUNT.put(2, R.string.text_again);
        CRASH_COUNT.put(3, R.string.text_again_and_again);
        CRASH_COUNT.put(4, R.string.text_again_and_again_again);
        CRASH_COUNT.put(5, R.string.text_again_and_again_again_again);
    }

    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setUpUI();
            handleIntent();
        } catch (Throwable throwable) {
            Log.e(TAG, "", throwable);
            exit();
        }
    }

    private String getCrashCountText() {
        int i = PreferenceManager.getDefaultSharedPreferences(this).getInt(KEY_CRASH_COUNT, 0);
        i++;
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(KEY_CRASH_COUNT, i).apply();
        if (i < 2) return "";
        if (i > 5) i = 5;
        return getString(CRASH_COUNT.get(i));
    }

    private void handleIntent() {
        String message = getIntent().getStringExtra("message");
        final String errorDetail = getIntent().getStringExtra("error");
        TextView errorTextView = findViewById(R.id.error);
        if (errorTextView != null) {
            String crashInfo = String.format("%s错误信息:\n%s\n%s", getDeviceMessage(), message, errorDetail);
            errorTextView.setText(crashInfo);
            saveCrashLog(crashInfo);
        }
    }

    private String getDeviceMessage() {
        DeviceInfo deviceInfo = new DeviceInfo(this);
        return String.format(Locale.getDefault(), "设备信息: \n%s\n\n", deviceInfo.toString());
    }

    private void copyToClip(String text) {
        ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Debug", text));
        Toast.makeText(ErrorReportActivity.this, R.string.text_already_copy_to_clip, Toast.LENGTH_SHORT).show();
    }

    private void setUpUI() {
        setContentView(R.layout.activity_error_report);
        Button copyButton = findViewById(R.id.copy);
        Button restartButton = findViewById(R.id.restart);
        Button exitButton = findViewById(R.id.exit);
        String mTitle = getCrashCountText() + getString(R.string.text_crash);
        setUpToolbar(mTitle);
        copyButton.setOnClickListener(view -> {
            TextView errorTextView = findViewById(R.id.error);
            if (errorTextView != null) {
                String crashInfo = errorTextView.getText().toString();
                copyToClip(crashInfo);
            }
        });

        restartButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // 关闭当前 Activity
        });

        exitButton.setOnClickListener(view -> exit());
    }

    private void setUpToolbar(String str) {
        Toolbar toolbar;
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(str);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(false);
    }

    @Override
    public void onBackPressed() {
        exit();
    }

    private void exit() {
        finishAffinity();
    }

    private void saveCrashLog(String crashInfo) {
        try {
            File logDir = new File(getExternalFilesDir(null), "AutoJs_Log");
            if (!logDir.exists() && !logDir.mkdirs()) {
                Log.e(TAG, "Error creating directory: " + logDir.getAbsolutePath());
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "Log_" + timeStamp + ".txt";
            File logFile = new File(logDir, fileName);
            FileWriter writer = new FileWriter(logFile);
            writer.write(crashInfo);
            writer.flush();
            writer.close();

            TextView saveTextView = findViewById(R.id.save);
            if (saveTextView != null) {
                String savedToText = getString(R.string.text_error_save, logFile.getAbsolutePath());
                saveTextView.setText(savedToText);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error saving crash log", e);
        }
    }
}
