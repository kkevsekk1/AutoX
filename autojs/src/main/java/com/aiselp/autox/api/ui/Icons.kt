package com.aiselp.autox.api.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.reflect.full.memberProperties


interface Icons {
    val Call: ImageVector
    val Add: ImageVector
    val ArrowBack: ImageVector
    val Clear: ImageVector
    val Edit: ImageVector
    val Menu: ImageVector
    val Search: ImageVector
    val Close: ImageVector
    val Star: ImageVector
    val Home: ImageVector
    val Notifications: ImageVector
    val Settings: ImageVector
    val MoreVert: ImageVector
    val MailOutline: ImageVector
    val Refresh: ImageVector
    val AccountBox: ImageVector
    val ArrowDropDown : ImageVector
}

object Default : com.aiselp.autox.api.ui.Icons {
    override val Call: ImageVector by lazy { Icons.Default.Call }
    override val Add by lazy { Icons.Default.Add }
    override val ArrowBack by lazy { Icons.AutoMirrored.Filled.ArrowBack }
    override val Clear by lazy{ Icons.Default.Clear }
    override val Edit by lazy { Icons.Default.Edit }
    override val Menu by lazy { Icons.Default.Menu }
    override val Search by lazy { Icons.Default.Search }
    override val Close by lazy { Icons.Default.Close }
    override val Star by lazy{ Icons.Default.Star }
    override val Home by lazy { Icons.Default.Home }
    override val Notifications by lazy { Icons.Default.Notifications }
    override val Settings by lazy { Icons.Default.Settings }
    override val MoreVert by lazy { Icons.Default.MoreVert }
    override val MailOutline by lazy { Icons.Default.MailOutline }
    override val Refresh by lazy { Icons.Default.Refresh }
    override val AccountBox by lazy { Icons.Default.AccountBox }
    override val ArrowDropDown: ImageVector by lazy { Icons.Default.ArrowDropDown }
}

object Filled : com.aiselp.autox.api.ui.Icons {
    override val Call: ImageVector by lazy { Icons.Filled.Call }
    override val Add by lazy { Icons.Filled.Add }
    override val ArrowBack by lazy { Icons.AutoMirrored.Filled.ArrowBack }
    override val Clear by lazy{ Icons.Filled.Clear }
    override val Edit by lazy { Icons.Filled.Edit }
    override val Menu by lazy { Icons.Filled.Menu }
    override val Search by lazy { Icons.Filled.Search }
    override val Close by lazy { Icons.Filled.Close }
    override val Star by lazy{ Icons.Filled.Star }
    override val Home by lazy { Icons.Filled.Home }
    override val Notifications by lazy { Icons.Filled.Notifications }
    override val Settings by lazy { Icons.Filled.Settings }
    override val MoreVert by lazy { Icons.Filled.MoreVert }
    override val MailOutline by lazy { Icons.Filled.MailOutline }
    override val Refresh by lazy { Icons.Filled.Refresh }
    override val AccountBox by lazy { Icons.Filled.AccountBox }
    override val ArrowDropDown: ImageVector by lazy { Icons.Filled.ArrowDropDown }
}

fun s() {
    com.aiselp.autox.api.ui.Icons::class.memberProperties.find { it.name == "Call" }
}