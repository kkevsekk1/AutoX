package org.autojs.autojs.ui.error;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
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
import java.util.Objects;

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
            setContentView(R.layout.activity_error_report);
        } catch (Throwable throwable) {
            Log.e(TAG, "", throwable);
            exit();
        }
    }

    protected void onStart() {
        super.onStart();
        setUpUI();
        handleIntent();
    }

    private String getCrashCountText() {
        int i = getPreferences(Context.MODE_PRIVATE).getInt(KEY_CRASH_COUNT, 0) + 1;
        getPreferences(Context.MODE_PRIVATE).edit().putInt(KEY_CRASH_COUNT, i).apply();
        if (i < 2) return "";
        return getString(CRASH_COUNT.get(Math.min(i, 5)));
    }

    private void handleIntent() {
        String message = getIntent().getStringExtra("message");
        String errorDetail = getIntent().getStringExtra("error");
        TextView errorTextView = $(R.id.error);
        if (errorTextView != null) {
            String crashInfo = String.format("%s错误信息:\n%s\n%s", getDeviceMessage(), message, errorDetail);
            errorTextView.setText(crashInfo);
            saveCrashLog(crashInfo);
        }
    }

    private String getDeviceMessage() {
        DeviceInfo deviceInfo = new DeviceInfo(this);
        return String.format(Locale.getDefault(), "设备信息: \n%s\n\n", deviceInfo);
    }

    private void copyToClip(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Debug", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(ErrorReportActivity.this, R.string.text_already_copy_to_clip, Toast.LENGTH_SHORT).show();
        }
    }

    private void setUpUI() {
        Toolbar toolbar = $(R.id.toolbar);
        Button copyButton = $(R.id.copy);
        Button restartButton = $(R.id.restart);
        Button exitButton = $(R.id.exit);

        String mTitle = String.format("%s%s", getCrashCountText(), getString(R.string.text_crash));
        toolbar.setTitle(mTitle);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setHomeButtonEnabled(false);

        copyButton.setOnClickListener(this::copy);

        restartButton.setOnClickListener(this::restar);

        exitButton.setOnClickListener(view -> exit());
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
            try (FileWriter writer = new FileWriter(logFile)) {
                writer.write(crashInfo);
            }
            TextView saveTextView = $(R.id.save_path);
            if (saveTextView != null) {
                String savedToText = getString(R.string.text_error_save, logFile.getAbsolutePath());
                saveTextView.setText(savedToText);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error saving crash log", e);
        }
    }

    private void copy(View view) {
        TextView errorTextView = $(R.id.error);
        if (errorTextView != null) {
            String crashInfo = errorTextView.getText().toString();
            copyToClip(crashInfo);
        }
    }

    private void restar(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
