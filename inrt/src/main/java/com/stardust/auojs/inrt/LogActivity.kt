package com.stardust.auojs.inrt

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.iterator
import com.stardust.auojs.inrt.autojs.AutoJs
import com.stardust.auojs.inrt.launch.GlobalProjectLauncher
import com.stardust.autojs.core.console.ConsoleImpl
import com.stardust.autojs.core.console.ConsoleView
import org.autojs.autoxjs.inrt.R


class LogActivity : AppCompatActivity() {

    lateinit var consoleImpl: ConsoleImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupView()
        if (intent.getBooleanExtra(EXTRA_LAUNCH_SCRIPT, false)) {
            GlobalProjectLauncher.launch(this)
        }
    }

    private fun setupView() {
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val consoleView = findViewById<ConsoleView>(R.id.console)
        consoleImpl = AutoJs.instance.globalConsole
        consoleView.setConsole(consoleImpl)
        consoleView.findViewById<View>(R.id.input_container).visibility = View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.rerun -> {
                GlobalProjectLauncher.stop()
                GlobalProjectLauncher.launch(this)
            }
            R.id.clear -> consoleImpl.clear()
            R.id.stop -> GlobalProjectLauncher.stop()
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.iterator().forEach { item ->
            item.icon?.let {
                val drawable = DrawableCompat.wrap(it)
                DrawableCompat.setTint(
                    drawable,
                    ContextCompat.getColor(this, android.R.color.white)
                )
                item.icon = drawable
            }
        }
        return true
    }

    companion object {


        val EXTRA_LAUNCH_SCRIPT = "launch_script"
    }
}
