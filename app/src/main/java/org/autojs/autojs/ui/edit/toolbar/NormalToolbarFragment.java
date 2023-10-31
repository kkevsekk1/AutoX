package org.autojs.autojs.ui.edit.toolbar;

import org.androidannotations.annotations.EFragment;
import org.autojs.autoxjs.R;

import java.util.Arrays;
import java.util.List;

@EFragment(R.layout.fragment_normal_toolbar)
public class NormalToolbarFragment extends ToolbarFragment {

    @Override
    public List<Integer> getMenuItemIds() {
        return Arrays.asList(R.id.edit, R.id.jump, R.id.debug, R.id.others, R.id.action_log, R.id.run, R.id.undo, R.id.redo, R.id.save);
    }
}
