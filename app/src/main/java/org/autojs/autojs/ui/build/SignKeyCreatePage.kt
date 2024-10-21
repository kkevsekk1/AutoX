package org.autojs.autojs.ui.build

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiselp.autox.apkbuilder.ApkKeyStore
import com.aiselp.autox.ui.material3.components.BaseDialog
import com.aiselp.autox.ui.material3.components.DialogController
import com.aiselp.autox.ui.material3.components.DialogTitle
import com.aiselp.autox.ui.material3.components.DropdownSingleChoiceInputBox
import com.aiselp.autox.ui.material3.components.PasswordInputBox
import com.mcal.apksigner.CertCreator
import com.mcal.apksigner.utils.DistinguishedNameValues
import com.stardust.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.autojs.autojs.Pref
import org.autojs.autojs.ui.build.SignManageActivity.SignManageModel
import org.autojs.autoxjs.R
import java.io.File
import java.nio.file.FileAlreadyExistsException
import java.security.KeyStore


@Composable
fun DialogController.ApkSignDeleteDialog(apkKeyStore: ApkKeyStore) {
    val scope = rememberCoroutineScope()
    val signManageModel = viewModel(SignManageModel::class.java)

    BaseDialog(onDismissRequest = { scope.launch { dismiss() } }, title = {
        DialogTitle(title = "删除签名")
    }, content = {
        Column {
            Text(text = "你确定要删除该文件？")
            Text(text = "${apkKeyStore.path}")
        }
    }, positiveText = "确定", onPositiveClick = {
        scope.launch { dismiss();signManageModel.deleteKeyStore(apkKeyStore) }
    }, negativeText = "取消", onNegativeClick = {
        scope.launch { dismiss() }
    })
}

@Composable
fun DialogController.ReviseSignKeyDialog(apkKeyStore: ApkKeyStore) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val signManageModel = viewModel(SignManageModel::class.java)
    var alias by remember { mutableStateOf(apkKeyStore.alias) }
    var keyStorePassword by remember { mutableStateOf(apkKeyStore.keyStorePassword) }
    var password by remember { mutableStateOf(apkKeyStore.password) }
    var aliasList: List<String>? by remember {
        mutableStateOf(null)
    }
    var keyStore: KeyStore? = null

    BaseDialog(onDismissRequest = { scope.launch { dismiss() } }, title = {
        DialogTitle(title =  "修改签名文件")
    }, positiveText = "确定", onPositiveClick = {
        scope.launch {
            if (alias.isNullOrEmpty() || keyStorePassword.isNullOrEmpty() || password.isNullOrEmpty()) {
                "参数不全".toast(context)
                return@launch
            }
            dismiss();
            apkKeyStore.apply {
                this.alias = alias
                this.keyStorePassword = keyStorePassword
                this.password = password
                signManageModel.updateKeyStore(this)
            }
        }
    }, negativeText = "取消", onNegativeClick = {
        scope.launch { dismiss() }
    }) {
        Column {
            Text(text = "文件路径: ")
            Text(text = apkKeyStore.path.toString())
            Spacer(modifier = Modifier.widthIn(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PasswordInputBox(
                    modifier = Modifier.weight(1f),
                    value = keyStorePassword ?: "",
                    onValueChange = { keyStorePassword = it },
                    enabled = aliasList == null,
                    isError = keyStorePassword.isNullOrEmpty(),
                    label = { Text(text = "密钥库密码") },
                )
                Spacer(modifier = Modifier.widthIn(10.dp))
                Button(
                    onClick = {
                        val file = apkKeyStore.path?.let { File(it) }
                        if (file == null || keyStorePassword == null) {
                            return@Button
                        }
                        scope.launch {
                            keyStore = signManageModel.loadKeyStore(file, keyStorePassword!!)
                            if (keyStore != null) {
                                "验证成功".toast(context)
                                aliasList = keyStore!!.aliases().toList()
                            } else {
                                "验证失败".toast(context)
                            }
                        }
                    },
                    enabled = aliasList == null,
                ) { Text(text = "验证") }
            }
            if (aliasList == null) {
                OutlinedTextField(
                    value = alias ?: "",
                    onValueChange = { alias = it },
                    singleLine = true,
                    isError = alias.isNullOrEmpty(),
                    label = { Text(text = "别名") },
                )
            } else {
                val l = aliasList!!
                var index = l.indexOf(alias)
                if (index == -1) {
                    index = 0
                    alias = l[index]
                }
                DropdownSingleChoiceInputBox("别名",
                    options = l,
                    selected = index,
                    onSelected = {
                        alias = l[it]
                    })
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                var enabled by remember { mutableStateOf(true) }
                PasswordInputBox(
                    modifier = Modifier.weight(1f),
                    value = password ?: "",
                    enabled = enabled,
                    onValueChange = { password = it },
                    isError = password.isNullOrEmpty(),
                    label = { Text(text = "别名密码") },
                )
                Spacer(modifier = Modifier.widthIn(10.dp))
                Button(onClick = {
                    scope.launch {
                        try {
                            if (keyStore == null) {
                                keyStore = signManageModel.loadKeyStore(
                                    File(apkKeyStore.path!!),
                                    keyStorePassword!!
                                )
                            }
                            keyStore!!.getKey(alias, password!!.toCharArray())
                            enabled = false
                            "验证成功".toast(context)
                        } catch (e: Exception) {
                            "验证失败".toast(context)
                        }
                    }
                }, enabled = enabled) {
                    Text(text = "验证")
                }
            }
        }
    }
}

@Composable
fun DialogController.ApkSignCreateDialog() {
    val scope = rememberCoroutineScope()
    val model = viewModel(SignKeyCreateModel::class.java)
    val signManageModel = viewModel(SignManageModel::class.java)
    val context = LocalContext.current

    BaseDialog(onDismissRequest = { scope.launch { dismiss() } }, title = {
        DialogTitle(title = "创建签名文件")
    }, content = { SignKeyCreatePage() }, positiveText = "确定", onPositiveClick = {
        if (!model.checkAll()) {
            return@BaseDialog
        }
        scope.launch {
            dismiss()
            try {
                val apkKeyStore = model.createSignKeyFile()
                signManageModel.addKeyStore(apkKeyStore)
                "创建成功".toast(context)
            } catch (e: Exception) {
                e.message.toString().toast(context)
            }
        }
    }, negativeText = "取消", onNegativeClick = {
        scope.launch { dismiss() }
    })
}

@Composable
fun SignKeyCreatePage() {
    val model = viewModel(modelClass = SignKeyCreateModel::class.java)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        val list = KeystoreType.values().map { it.name }
        DropdownSingleChoiceInputBox(stringResource(R.string.text_sign_keystore),
            options = list,
            selected = list.indexOf(model.keystoreType.name),
            onSelected = {
                model.keystoreType = KeystoreType.valueOf(list[it])
            })
        val signatureAlgorithms = SignatureAlgorithm.values().map { it.name }
        DropdownSingleChoiceInputBox("签名算法",
            options = signatureAlgorithms,
            selected = signatureAlgorithms.indexOf(model.signatureAlgorithm.value),
            onSelected = {
                model.signatureAlgorithm = SignatureAlgorithm.valueOf(signatureAlgorithms[it])
            })
        OutlinedTextField(
            value = model.name,
            onValueChange = { model.name = it },
            singleLine = true,
            isError = !model.nameCheck,
            label = { Text(text = "文件名") },
            suffix = { Text(text = ".${model.keystoreType.value}") }
        )
        PasswordInputBox(
            value = model.keystorePassword,
            onValueChange = { model.keystorePassword = it },
            isError = !model.keystorePasswordCheck,
            label = { Text(text = "密钥库密码") },
        )
        OutlinedTextField(
            value = model.alias,
            onValueChange = { model.alias = it },
            singleLine = true,
            isError = !model.aliasCheck,
            label = { Text(text = "别名") },
        )
        PasswordInputBox(
            value = model.aliasPassword,
            onValueChange = { model.aliasPassword = it },
            isError = !model.aliasPasswordCheck,
            label = { Text(text = "别名密码") },
        )
        OutlinedTextField(
            value = model.validity.toString(),
            onValueChange = { model.validity = it.toIntOrNull() ?: 30 },
            singleLine = true,
            isError = !model.validityCheck,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text(text = "有效期") },
        )
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.text_sign_hint_not_must),
            style = MaterialTheme.typography.labelLarge
        )
        OutlinedTextField(
            value = model.commonName,
            onValueChange = { model.commonName = it },
            singleLine = true,
            label = { Text(text = stringResource(R.string.text_sign_hint_key_name)) },
        )
        OutlinedTextField(
            value = model.country,
            onValueChange = { model.country = it },
            singleLine = true,
            label = { Text(text = stringResource(R.string.text_sign_hint_key_country)) },
        )
        OutlinedTextField(
            value = model.organization,
            onValueChange = { model.organization = it },
            singleLine = true,
            label = { Text(text = stringResource(R.string.text_sign_hint_key_org)) },
        )
        OutlinedTextField(
            value = model.organizationalUnit,
            onValueChange = { model.organizationalUnit = it },
            singleLine = true,
            label = { Text(text = stringResource(R.string.text_sign_hint_key_org_unit)) },
        )
        OutlinedTextField(
            value = model.state,
            onValueChange = { model.state = it },
            singleLine = true,
            label = { Text(text = stringResource(R.string.text_sign_hint_key_province)) },
        )
        OutlinedTextField(
            value = model.locality,
            onValueChange = { model.locality = it },
            singleLine = true,
            label = { Text(text = stringResource(R.string.text_sign_hint_key_city)) },
        )
        OutlinedTextField(
            value = model.street,
            onValueChange = { model.street = it },
            singleLine = true,
            label = { Text(text = "街道") },
        )
    }
}

class SignKeyCreateModel : ViewModel() {
    var signatureAlgorithm: SignatureAlgorithm by mutableStateOf(SignatureAlgorithm.SHA256withRSA)
    var keystoreType: KeystoreType by mutableStateOf(KeystoreType.BKS)
    var fileName: String by mutableStateOf("")
    var keystorePassword: String by mutableStateOf("")
    var alias: String by mutableStateOf("")
    var aliasPassword: String by mutableStateOf("")
    var validity: Int by mutableIntStateOf(30)
    var name: String by mutableStateOf("")

    var country: String by mutableStateOf("")
    var state: String by mutableStateOf("")
    var locality: String by mutableStateOf("")
    var street: String by mutableStateOf("")
    var organization: String by mutableStateOf("")
    var organizationalUnit: String by mutableStateOf("")
    var commonName: String by mutableStateOf("")

    //数据校验
    var enableCheck by mutableStateOf(false)
    val nameCheck: Boolean
        get() = !enableCheck || name.isNotEmpty()
    val keystorePasswordCheck: Boolean
        get() = !enableCheck || keystorePassword.isNotEmpty()
    val aliasCheck: Boolean
        get() = !enableCheck || alias.isNotEmpty()
    val aliasPasswordCheck: Boolean
        get() = !enableCheck || aliasPassword.isNotEmpty()
    val validityCheck: Boolean
        get() = !enableCheck || validity > 0

    fun checkAll(): Boolean {
        enableCheck = true
        return nameCheck and keystorePasswordCheck and aliasCheck and aliasPasswordCheck and validityCheck
    }

    suspend fun createSignKeyFile(override: Boolean = false) = withContext(Dispatchers.Default) {
        val keyStorePath = File(Pref.getKeyStorePath())
        keyStorePath.mkdirs()
        val fileName = "$name.${keystoreType.value}"
        val file = File(keyStorePath, fileName)
        if (file.isFile && !override) {
            throw FileAlreadyExistsException(file.absolutePath)
        }
        val distinguishedNameValues = DistinguishedNameValues().apply {
            setCommonName(commonName)
            setCountry(country)
            setState(state)
            setLocality(locality)
            setStreet(street)
            setOrganization(organization)
            setOrganizationalUnit(organizationalUnit)
        }
        CertCreator.createKeystoreAndKey(
            file,
            keystorePassword.toCharArray(),
            "RSA",
            2048,
            alias,
            aliasPassword.toCharArray(),
            signatureAlgorithm.value,
            validity,
            distinguishedNameValues
        )
        return@withContext ApkKeyStore(
            path = file.absolutePath,
            name = fileName,
            keyStorePassword = keystorePassword,
            alias = alias,
            password = aliasPassword,
            isVerified = true
        )
    }
}

enum class SignatureAlgorithm(val displayName: String, val value: String) {
    MD5withRSA("MD5withRSA", "MD5withRSA"), SHA1withRSA(
        "SHA1withRSA",
        "SHA1withRSA"
    ),
    SHA256withRSA("SHA256withRSA", "SHA256withRSA"), SHA512withRSA(
        "SHA512withRSA",
        "SHA512withRSA"
    ),
}

enum class KeystoreType(val value: String) {
    BKS("bks"), JKS("jks"),
}