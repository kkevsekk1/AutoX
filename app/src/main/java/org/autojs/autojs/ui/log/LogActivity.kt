package org.autojs.autojs.ui.log

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.shape.MaterialShapeDrawable
import com.stardust.autojs.core.console.ConsoleImpl
import com.stardust.autojs.core.console.ConsoleView
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autoxjs.R
import org.autojs.autoxjs.databinding.ActivityLogBinding


open class LogActivity : AppCompatActivity() {
    private val consoleView: ConsoleView by lazy { binding.console }
    private val consoleImpl: ConsoleImpl by lazy { AutoJs.getInstance().globalConsole }
    private lateinit var binding: ActivityLogBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpToolbar()
        setupViews()
    }

    private fun setUpToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    fun setupViews() {
        findViewById<AppBarLayout>(R.id.app_bar).statusBarForeground =
            MaterialShapeDrawable.createWithElevationOverlay(this)
        consoleView.setConsole(consoleImpl)
        consoleView.findViewById<View>(R.id.input_container).apply {
            visibility = View.GONE
        }
        findViewById<View>(R.id.fab).setOnClickListener {
            consoleImpl.clear()
        }
    }
}
