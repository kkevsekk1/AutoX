package com.aiselp.autox.apkbuilder

import android.content.Context
import android.util.Log
import com.mcal.apksigner.ApkSigner
import com.mcal.apksigner.utils.KeyStoreHelper
import java.io.File
import java.security.KeyStore

class ApkSignUtil(val context: Context) : AutoCloseable {

    val signDatabaseHelper = SignDatabaseHelper(context)

    init {
        AAPT_Util.init(context)
    }

    fun getDefaultKeyStore(): ApkKeyStore {
        val ketFile = File(context.filesDir, assetKeyFile)
        defaultKeyStore.path = ketFile.absolutePath
        defaultKeyStore.name = ketFile.name
        if (!ketFile.isFile) {
            ketFile.delete()
            context.assets.open(assetKeyFile).use {
                ketFile.outputStream().use { out ->
                    it.copyTo(out)
                }
            }
        }
        Log.d(TAG, "getDefaultKeyStore: ${ketFile.isFile}")
        return defaultKeyStore
    }

    fun loadApkKeyStore(path: File): List<ApkKeyStore> {
        if (!path.isDirectory) {
            return emptyList()
        }
        val fileList = path.list { _, name ->
            name.endsWith(".bks") || name.endsWith(".jks")
        } ?: emptyArray()
        return fileList.map {
            val keyPath = File(path, it).absolutePath
            signDatabaseHelper.queryPath(keyPath)?.apply {
                isVerified = true
            } ?: ApkKeyStore(
                path = keyPath,
                name = it
            )
        }
//        KeyStoreHelper.validateKeystorePassword()
//        CertCreator.createKeystoreAndKey()
    }

    fun loadSavedApkKeyStore(): List<ApkKeyStore> {
        return signDatabaseHelper.queryAllData().filter {
            File(it.path!!).isFile
        }
    }

    fun queryPath(path: String): ApkKeyStore? {
        return signDatabaseHelper.queryPath(path)
    }

    fun loadKeyStore(path: File, password: String): KeyStore? {
        return try {
            KeyStoreHelper.loadKeyStore(path, password.toCharArray())
        } catch (e: Exception) {
            null
        }
    }

    fun loadTestData(path: File): Array<ApkKeyStore> {
        return arrayOf(
            ApkKeyStore(
                path = File(path, "sedxx.bks").absolutePath,
                name = "sedxx.bks",
                alias = "seeer",
                password = "123456",
                isVerified = true
            ),
            ApkKeyStore(
                path = File(path, "saeee.bks").absolutePath,
                name = "sssae.bks",
                alias = null,
                password = null,
            )
        )
    }

    override fun close() {
        signDatabaseHelper.close()
    }

    companion object {
        private const val TAG = "ApkSignUtil"
        private const val assetKeyFile = "autox-default.jks"
        private val defaultKeyStore = ApkKeyStore(
            keyStorePassword = "12345678",
            alias = "key0",
            password = "12345678",
            isVerified = true
        )

        fun sign(
            oldFile: File, newFile: File,
            apkKeyStore: ApkKeyStore,
            v1Sign: Boolean = true,
            v2Sign: Boolean = true,
            v3Sign: Boolean = false,
            v4Sign: Boolean = false,
        ): Boolean {
            Log.d(
                TAG,
                "sign apk ${oldFile.absolutePath} to ${newFile.absolutePath}\n" +
                        " with ${apkKeyStore.path} ${apkKeyStore.keyStorePassword} ${apkKeyStore.alias} ${apkKeyStore.password}\n" +
                        "v1Sign: $v1Sign v2Sign: $v2Sign v3Sign: $v3Sign v4Sign: $v4Sign"
            )
            return ApkSigner(oldFile, newFile).apply {
                useDefaultSignatureVersion = false
                v1SigningEnabled = v1Sign
                v2SigningEnabled = v2Sign
                v3SigningEnabled = v3Sign
                v4SigningEnabled = v4Sign
            }.signRelease(
                File(apkKeyStore.path!!),
                apkKeyStore.keyStorePassword!!,
                apkKeyStore.alias!!,
                apkKeyStore.password!!
            )
        }
    }
}