package org.autojs.autojs.ui.util

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

fun AppCompatActivity.setupToolbar(toolbar: Toolbar) {
    setSupportActionBar(toolbar)
    supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    toolbar.setNavigationOnClickListener { finish() }
}