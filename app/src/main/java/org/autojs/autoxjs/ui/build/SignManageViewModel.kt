package org.autojs.autoxjs.ui.build

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import org.autojs.autoxjs.build.ApkKeyStore
import org.autojs.autoxjs.build.ApkSigner

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