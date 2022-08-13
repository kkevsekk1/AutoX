package org.autojs.autoxjs.ui.update;

import android.app.Activity;

import org.autojs.autoxjs.BuildConfig;
import org.autojs.autoxjs.network.VersionService;
import org.autojs.autoxjs.network.entity.VersionInfo;
import org.autojs.autoxjs.tool.SimpleObserver;

import io.reactivex.android.schedulers.AndroidSchedulers;
/**
 * Created by Stardust on 2017/4/12.
 */

public class VersionGuard {
    private Activity mActivity;
    private VersionService mVersionService = VersionService.getInstance();
    public VersionGuard(Activity activity) {
        mActivity = activity;
    }

    public void checkForDeprecatesAndUpdates() {
            checkForUpdatesIfNeeded();
    }
    private void checkForUpdatesIfNeeded() {
        mVersionService.checkForUpdatesIfNeededAndUsingWifi(mActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<VersionInfo>() {
                    @Override
                    public void onNext(@io.reactivex.annotations.NonNull VersionInfo versionInfo) {
                            showUpdateInfoIfNeeded(versionInfo);
                    }
                });
    }
    private void showUpdateInfoIfNeeded(VersionInfo info) {
        if (BuildConfig.VERSION_CODE < info.versionCode) {
            new UpdateInfoDialogBuilder(mActivity, info)
                    .showDoNotAskAgain()
                    .show();
        }
    }

}
