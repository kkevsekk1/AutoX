package org.autojs.autojs.model.explorer;

import com.stardust.autojs.project.ProjectConfigKt;
import com.stardust.pio.PFile;

import java.io.File;

public class ExplorerProjectPage extends ExplorerDirPage {

    private ProjectConfigKt mProjectConfig;

    public ExplorerProjectPage(PFile file, ExplorerPage parent, ProjectConfigKt projectConfig) {
        super(file, parent);
        mProjectConfig = projectConfig;
    }

    public ExplorerProjectPage(String path, ExplorerPage parent, ProjectConfigKt projectConfig) {
        super(path, parent);
        mProjectConfig = projectConfig;
    }

    public ExplorerProjectPage(File file, ExplorerPage parent, ProjectConfigKt projectConfig) {
        super(file, parent);
        mProjectConfig = projectConfig;
    }

    public ProjectConfigKt getProjectConfig() {
        return mProjectConfig;
    }

    @Override
    public ExplorerFileItem rename(String newName) {
        return new ExplorerProjectPage(getFile().renameTo(newName), getParent(), mProjectConfig);
    }
}
