package org.autojs.autojs.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.autojs.autojs.ui.main.scripts.ScriptListFragment
import org.autojs.autojs.ui.main.task.TaskManagerFragmentKt
import org.autojs.autojs.ui.main.web.WebViewFragment

class ViewPager2Adapter : FragmentStateAdapter {
    constructor(
        fragmentActivity: FragmentActivity,
        scriptListFragment: ScriptListFragment
    ) : super(fragmentActivity) {
        this.scriptListFragment = scriptListFragment
    }

    constructor(fragment: Fragment) : super(fragment) {}
    constructor(fragmentManager: FragmentManager, lifecycle: Lifecycle) : super(
        fragmentManager,
        lifecycle
    ) {
    }

    private lateinit var scriptListFragment: ScriptListFragment

    override fun createFragment(position: Int): Fragment {
        val fragment = when (position) {
            0 -> {
                scriptListFragment
            }
            1 -> {
                TaskManagerFragmentKt()
            }
            else -> {
                WebViewFragment()
            }
        }
        return fragment
    }

    override fun getItemCount(): Int {
        return 3
    }
}