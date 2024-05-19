package org.autojs.autojs.ui.edit.toolbar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.autojs.autoxjs.R;

import java.util.Arrays;
import java.util.List;

public class NormalToolbarFragment extends ToolbarFragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_normal_toolbar, container, false);
    }

    @Override
    public List<Integer> getMenuItemIds() {
        return Arrays.asList(R.id.edit, R.id.jump, R.id.debug, R.id.others, R.id.action_log, R.id.run, R.id.undo, R.id.redo, R.id.save);
    }
}
