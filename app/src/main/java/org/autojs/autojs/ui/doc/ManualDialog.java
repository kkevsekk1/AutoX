package org.autojs.autojs.ui.doc;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.MaterialDialog;
import com.stardust.util.IntentUtil;

import org.autojs.autojs.ui.widget.EWebView;
import org.autojs.autoxjs.R;

/**
 * Created by Stardust on 2017/10/24.
 */

public class ManualDialog {

    private TextView mTitle;

    private EWebView mEWebView;

    private View mPinToLeft;

    Dialog mDialog;
    private final Context mContext;
    private View mClose;
    private View mFullscreen;

    public ManualDialog(Context context) {
        mContext = context;
        View view = View.inflate(context, R.layout.floating_manual_dialog, null);
        bindView(view);
        mDialog = new MaterialDialog.Builder(context)
                .customView(view, false)
                .build();
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }


    public ManualDialog title(String title) {
        mTitle.setText(title);
        return this;
    }

    public ManualDialog url(String url) {
        mEWebView.getWebView().loadUrl(url);
        return this;
    }

    public ManualDialog pinToLeft(View.OnClickListener listener) {
        mPinToLeft.setOnClickListener(v -> {
            mDialog.dismiss();
            listener.onClick(v);
        });
        return this;
    }

    public ManualDialog show() {
        mDialog.show();
        return this;
    }

    private void close() {
        mDialog.dismiss();
    }

    private void viewInNewActivity() {
        mDialog.dismiss();
        IntentUtil.browse(mContext,mEWebView.getWebView().getUrl());
    }

    private void bindView(@NonNull View bindSource) {
        mTitle = bindSource.findViewById(R.id.title);
        mEWebView = bindSource.findViewById(R.id.eweb_view);
        mPinToLeft = bindSource.findViewById(R.id.pin_to_left);
        mClose = bindSource.findViewById(R.id.close);
        mFullscreen = bindSource.findViewById(R.id.fullscreen);
        mClose.setOnClickListener(v -> {
            close();
        });
        mFullscreen.setOnClickListener(v -> {
            viewInNewActivity();
        });
    }
}
