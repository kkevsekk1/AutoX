package org.autojs.autojs.ui.build

import android.app.Application
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiselp.autox.apkbuilder.ApkKeyStore
import com.aiselp.autox.apkbuilder.ApkSignUtil
import com.aiselp.autox.ui.material3.components.BackTopAppBar
import com.aiselp.autox.ui.material3.components.DialogController
import com.aiselp.autox.ui.material3.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.autojs.autojs.Pref
import org.autojs.autoxjs.R
import java.io.File

class SignManageActivity : AppCompatActivity() {
    private val addSignDialog = DialogController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                Scaffold(
                    topBar = { BackTopAppBar(title = "签名管理") },
                    floatingActionButton = { AddButton() }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        addSignDialog.ApkSignCreateDialog()
                        val model = viewModel(SignManageModel::class.java)
                        LazyColumn {
                            val cardModifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp)
                            items(model.data) {
                                SignFileCard(
                                    modifier = cardModifier,
                                    apkKeyStore = it,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SignFileCard(modifier: Modifier = Modifier, apkKeyStore: ApkKeyStore) {
        val scope = rememberCoroutineScope()
        val deleteSignDialog = DialogController()
        deleteSignDialog.ApkSignDeleteDialog(apkKeyStore = apkKeyStore)
        val reviseSignKeyDialog = DialogController()
        reviseSignKeyDialog.ReviseSignKeyDialog(apkKeyStore)
        Card(
            onClick = { scope.launch { reviseSignKeyDialog.show() } },
            modifier = modifier
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row {
                        Text(
                            text = "${stringResource(R.string.text_sign_keystore)}: ",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "${apkKeyStore.name}",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        Text(
                            text = "${stringResource(R.string.text_sign_alias)}: ",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${apkKeyStore.alias}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { scope.launch { reviseSignKeyDialog.show() } }, label = {
                            if (apkKeyStore.isVerified) {
                                Text(text = "已配置", color = MaterialTheme.colorScheme.secondary)
                            } else {
                                Text(text = "未配置")
                            }
                        }, enabled = !apkKeyStore.isVerified
                    )
                    IconButton(onClick = { scope.launch { deleteSignDialog.show() } }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.text_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun AddButton() {
        val scope = rememberCoroutineScope()
        FloatingActionButton(
            onClick = { scope.launch { addSignDialog.show() } },
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.text_sign_key_add)
            )
        }
    }

    class SignManageModel(application: Application) : AndroidViewModel(application) {
        var data by mutableStateOf<List<ApkKeyStore>>(emptyList())
        private val apkSignUtil = ApkSignUtil(application)

        init {
            initFileData()
            addCloseable { apkSignUtil.close() }
        }

        private fun initFileData() = viewModelScope.launch {
            data = withContext(Dispatchers.Default) {
                val keyStoreDir = File(Pref.getKeyStorePath())
                apkSignUtil.loadApkKeyStore(keyStoreDir)
            }
        }

        fun deleteKeyStore(apkKeyStore: ApkKeyStore) = viewModelScope.launch {
            apkKeyStore.path?.let { File(it).delete() }
            data = data - apkKeyStore
            apkSignUtil.signDatabaseHelper.deleteData(apkKeyStore)
        }

        fun addKeyStore(apkKeyStore: ApkKeyStore) = viewModelScope.launch {
            apkKeyStore.isVerified = true
            data = data + apkKeyStore
            apkSignUtil.signDatabaseHelper.insertData(apkKeyStore)
        }

        fun updateKeyStore(apkKeyStore: ApkKeyStore) = viewModelScope.launch {
            apkKeyStore.isVerified = true
            val path = apkKeyStore.path ?: return@launch
            if (apkSignUtil.signDatabaseHelper.queryPath(path) == null) {
                apkSignUtil.signDatabaseHelper.insertData(apkKeyStore)
            } else apkSignUtil.signDatabaseHelper.updateData(apkKeyStore)
            val e = data
            data = emptyList()
            data = e
        }

        fun loadKeyStore(path: File, password: String) = apkSignUtil.loadKeyStore(path, password)
    }
}