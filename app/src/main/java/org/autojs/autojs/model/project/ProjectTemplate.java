package org.autojs.autojs.model.project;

import android.annotation.SuppressLint;

import com.stardust.autojs.project.ProjectConfigKt;
import com.stardust.pio.PFiles;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ProjectTemplate {


    private final ProjectConfigKt mProjectConfig;
    private final File mProjectDir;

    public ProjectTemplate(ProjectConfigKt projectConfig, File projectDir) {
        mProjectConfig = projectConfig;
        mProjectDir = projectDir;
    }

    @SuppressLint("CheckResult")
    public Observable<File> newProject() {
        return Observable.fromCallable(() -> {
            mProjectDir.mkdirs();
            PFiles.write(ProjectConfigKt.Companion.configFileOfDir(mProjectDir.getPath()), mProjectConfig.toJson());
            new File(mProjectDir, mProjectConfig.getMainScriptFile()).createNewFile();
            return mProjectDir;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

}
