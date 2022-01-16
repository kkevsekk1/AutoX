package org.autojs.autojs.ui.update;

import android.app.Activity;

import org.autojs.autojs.BuildConfig;
import org.autojs.autojs.network.VersionService;
import org.autojs.autojs.network.entity.VersionInfo;
import org.autojs.autojs.tool.SimpleObserver;

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
