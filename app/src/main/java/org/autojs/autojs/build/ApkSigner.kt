package org.autojs.autojs.build

import org.autojs.autojs.build.apksigner.KeyStoreFileManager
import org.autojs.autojs.build.apksigner.ZipSigner
import org.autojs.autojs.build.apksigner.CertCreator.DistinguishedNameValues
import org.autojs.autojs.build.apksigner.CertCreator
import com.stardust.pio.PFile
import org.autojs.autojs.Pref
import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception
import java.security.PrivateKey
import java.security.UnrecoverableKeyException
import java.security.cert.X509Certificate
import java.util.ArrayList

/**
 * Created by Cc on 2021/04/15
 * Modified by wilinz on 2022/5/23
 */
object ApkSigner {
    @Throws(Exception::class)
    fun sign(keystorePath: String?, keyPassword: String, oldApk: File, newApk: File) {
        val keystoreFile = File(keystorePath)
        if (!keystoreFile.exists()) {
            throw FileNotFoundException("keystore不存在")
        }
        try {
            val password = keyPassword.toCharArray()
            val keyStore = KeyStoreFileManager.loadKeyStore(keystorePath, null)
            val alias = keyStore.aliases().nextElement()
            val publicKey = keyStore.getCertificate(alias) as X509Certificate
            val privateKey = keyStore.getKey(alias, password) as PrivateKey
            ZipSigner.signZip(publicKey, privateKey, "SHA1withRSA", oldApk.path, newApk.path)
        } catch (e: UnrecoverableKeyException) {
            throw UnrecoverableKeyException("密码错误")
        }
    }

    fun deleteKeyStore(path:String){
        File(path).delete()
    }

    @JvmStatic
    fun createKeyStore(
        keystorePath: String, alias: String,
        password: String, year: Int, country: String?,
        name: String?, org: String?, unit: String?, province: String?, city: String?
    ): Boolean {
        try {
            val keyPassword = password.toCharArray()
            val nameValues = DistinguishedNameValues()
            nameValues.setCountry(country)
            nameValues.setCommonName(name)
            nameValues.setOrganization(org)
            nameValues.setOrganizationalUnit(unit)
            nameValues.setState(province)
            nameValues.setLocality(city)
            CertCreator.createKeystoreAndKey(
                keystorePath, keyPassword, "RSA", 2048, alias, keyPassword, "SHA1withRSA",
                year, nameValues
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    @JvmStatic
    fun loadKeyStore(): ArrayList<ApkKeyStore> {
        val list = ArrayList<ApkKeyStore>()
        try {
            val dir = PFile(Pref.getKeyStorePath())
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val files = dir.listFiles()
            for (file in files) {
                if (file.isFile) {
                    val keyStore = KeyStoreFileManager.loadKeyStore(file.path, null)
                    val alias = keyStore.aliases().nextElement()
                    val item = ApkKeyStore()
                    item.path = file.path
                    item.name = file.simplifiedPath
                    item.alias = alias
                    val password = Pref.getKeyStorePassWord(file.name)
                    item.password = password
                    item.isVerified = !password.isEmpty()
                    list.add(item)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    @JvmStatic
    fun loadApkKeyStore(path: String?): ApkKeyStore? {
        try {
            val file = PFile(path!!)
            if (!file.exists()) {
                return null
            }
            val keyStore = KeyStoreFileManager.loadKeyStore(file.path, null)
            val alias = keyStore.aliases().nextElement()
            val item = ApkKeyStore()
            item.path = file.path
            item.name = file.simplifiedPath
            item.alias = alias
            val password = Pref.getKeyStorePassWord(file.name)
            item.password = password
            item.isVerified = !password.isEmpty()
            return item
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    @JvmStatic
    fun checkKeyStore(keystorePath: String?, keyPassword: String): Boolean {
        return try {
            val keystoreFile = File(keystorePath)
            if (!keystoreFile.exists()) {
                return false
            }
            val password = keyPassword.toCharArray()
            val keyStore = KeyStoreFileManager.loadKeyStore(keystorePath, null)
            val alias = keyStore.aliases().nextElement()
            val publicKey = keyStore.getCertificate(alias) as X509Certificate
            val privateKey = keyStore.getKey(alias, password) as PrivateKey
            true
        } catch (e: Exception) {
            false
        }
    }
}