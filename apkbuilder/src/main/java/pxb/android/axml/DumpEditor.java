package pxb.android.axml;

import java.util.Map;

/**
 * Created by Stardust on 2017/10/23.
 */

public class DumpEditor extends DumpAdapter {

    private Map<String, String> mValueModifications;

    public DumpEditor(Map<String, String> valueModifications) {
        mValueModifications = valueModifications;
    }

    public DumpEditor(NodeVisitor nv, Map<String, String> valueModifications) {
        super(nv);
        mValueModifications = valueModifications;
    }

    @Override
    public void attr(String ns, String name, int resourceId, int type, Object obj) {
        if (ns != null) {
            String fullName = getPrefix(ns) + ":" + name;
            String newValue = mValueModifications.get(fullName);
            if (newValue != null) {
                super.attr(ns, name, -1, TYPE_STRING, newValue);
                return;
            }
        } else {
            String newValue = mValueModifications.get(name);
            if (newValue != null) {
                super.attr(ns, name, resourceId, TYPE_STRING, newValue);
                return;
            }
        }
        super.attr(ns, name, resourceId, type, obj);

    }

    @Override
    public NodeVisitor child(String ns, String name) {
        NodeVisitor child = super.child(ns, name);
        if (!(child instanceof DumpEditor)) {
            return new DumpEditor(child, mValueModifications);
        }
        return child;
    }
}
