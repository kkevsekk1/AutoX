package org.autojs.autojs.ui.build

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.stardust.app.DialogUtils
import com.stardust.pio.PFiles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.autojs.autojs.Pref
import org.autojs.autoxjs.R
import org.autojs.autojs.build.ApkKeyStore
import org.autojs.autojs.build.ApkSigner
import org.autojs.autojs.build.ApkSigner.checkKeyStore
import org.autojs.autojs.build.ApkSigner.loadApkKeyStore
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder
import org.autojs.autojs.ui.compose.theme.AutoXJsTheme
import org.autojs.autojs.ui.compose.util.SetSystemUI
import org.autojs.autojs.ui.compose.widget.MySwipeRefresh

/**
 * @author wilinz
 * @date 2022/5/23
 */
class SignManageActivityKt : ComponentActivity() {

    companion object {
        fun getIntent(context: Context) = Intent(context, SignManageActivityKt::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutoXJsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    SetSystemUI()
                    val context = LocalContext.current
                    val model: SignManageViewModel = viewModel()
                    val scope = rememberCoroutineScope()
                    Scaffold(
                        topBar = {
                            TopAppBar(context, model)
                        }
                    ) {
                        val state = rememberSwipeRefreshState(isRefreshing = false)
                        MySwipeRefresh(
                            state = state,
                            onRefresh = {
                                scope.launch {
                                    state.isRefreshing = true
                                    model.refresh()
                                    delay(500)
                                    state.isRefreshing = false
                                }
                            },
                            modifier = Modifier.padding(it)
                        ) {
                            SignManagePage(model)
                        }

                    }

                }
            }
        }
    }

    @Composable
    private fun TopAppBar(
        context: Context,
        model: SignManageViewModel
    ) {
        TopAppBar(
            title = { Text(text = "签名管理") },
            navigationIcon = {
                IconButton(onClick = {
                    with(context) {
                        if (this is SignManageActivityKt) finish()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "back"
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    showKeyCreateDialog(
                        context = context,
                        onCreated = { path ->
                            loadApkKeyStore(path)
                                ?.let { model.keyStoreList.add(it) }
                        })
                }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "add"
                    )
                }
            }
        )
    }
}

@Composable
private fun SignManagePage(
    model: SignManageViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(model.keyStoreList) { index, item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!item.isVerified) {
                            showPasswordInputDialog(
                                context = context,
                                apkKeyStore = item,
                                onIsVerifiedChange = {
                                    /*model.keyStoreList[index] =
                                        model.keyStoreList[index].copy(isVerified = it)
*/
                                    model.refresh()
                                    Toast
                                        .makeText(context, "已验证", Toast.LENGTH_SHORT)
                                        .show()
                                })
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(text = item.path ?: "")
                        Text(text = item.alias ?: "", color = Color.Gray)
                    }
                    val isVerified by item::isVerified
                    Text(
                        text = if (isVerified) "已验证" else "未验证",
                        color = if (isVerified) Color.Green else Color.Red
                    )
                    var isShowDialog by remember {
                        mutableStateOf(false)
                    }
                    DeleteAlertDialog(
                        expanded = isShowDialog,
                        onDismissRequest = { isShowDialog = false },
                        onConfirm = {
                            item.path?.let {
                                ApkSigner.deleteKeyStore(it)
                                model.keyStoreList.removeAt(index)
                            }
                        }
                    )
                    IconButton(onClick = {
                        isShowDialog = true
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = "delete key"
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun DeleteAlertDialog(expanded: Boolean, onDismissRequest: () -> Unit, onConfirm: () -> Unit) {
    if (expanded) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(text = "确定")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = "取消")
                }
            },
            title = {
                Text(text = "删除")
            },
            text = {
                Text(text = "确定删除？")
            },
            shape = RoundedCornerShape(8.dp),
        )
    }

}

private fun showKeyCreateDialog(context: Context, onCreated: (path: String) -> Unit) {
    SignKeyCreateDialogBuilder(context)
        .title(R.string.text_sign_key_add)
        .whenCreated { path: String ->
            onCreated(path)
        }
        .autoDismiss(false)
        .show()
}

private fun showPasswordInputDialog(
    context: Context,
    apkKeyStore: ApkKeyStore,
    onIsVerifiedChange: (isVerified: Boolean) -> Unit
) {
    DialogUtils.showDialog(ThemeColorMaterialDialogBuilder(context).title(R.string.text_sign_password)
        .inputType(InputType.TYPE_TEXT_VARIATION_PASSWORD)
        .autoDismiss(false)
        .input(
            context.getString(R.string.text_sign_password_input), "", true
        ) { dialog, input -> }
        .onPositive { dialog: MaterialDialog, which: DialogAction? ->
            val password = dialog.inputEditText!!
                .text.toString()
            if (checkKeyStore(apkKeyStore.path, password)) {
                Pref.setKeyStorePassWord(apkKeyStore.path?.let { PFiles.getName(it) }, password)
                apkKeyStore.isVerified = true
                onIsVerifiedChange(true)
                dialog.dismiss()
            } else {
                dialog.inputEditText!!.error = "验证失败"
            }
        }
        .build())
}
