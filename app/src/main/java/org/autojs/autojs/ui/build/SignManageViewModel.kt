package org.autojs.autojs.ui.build

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import org.autojs.autojs.build.ApkKeyStore
import org.autojs.autojs.build.ApkSigner

/**
 * @author wilinz
 * @date 2022/5/23
 */
class SignManageViewModel : ViewModel() {
    val keyStoreList = mutableStateListOf<ApkKeyStore>().apply { addAll(ApkSigner.loadKeyStore()) }

    fun refresh(){
        keyStoreList.apply {
            clear()
            addAll(ApkSigner.loadKeyStore())
        }
    }
}