package org.autojs.autojs.build;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.stardust.pio.PFile;

import org.autojs.autojs.Pref;
import org.autojs.autojs.build.apksigner.CertCreator;
import org.autojs.autojs.build.apksigner.KeyStoreFileManager;
import org.autojs.autojs.build.apksigner.ZipSigner;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

/**
 * Created by Cc on 2021/04/15
 */
@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class ApkSigner {

    public static void sign(String keystorePath, String keyPassword, File oldApk, File newApk) throws Exception {
        File keystoreFile = new File(keystorePath);
        if (!keystoreFile.exists()) {
            throw new FileNotFoundException("keystore不存在");
        }
        try {
            char[] password = keyPassword.toCharArray();
            KeyStore keyStore = KeyStoreFileManager.loadKeyStore(keystorePath, null);
            String alias = keyStore.aliases().nextElement();
            X509Certificate publicKey = (X509Certificate) keyStore.getCertificate(alias);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password);
            ZipSigner.signZip(publicKey, privateKey, "SHA1withRSA", oldApk.getPath(), newApk.getPath());
        } catch (UnrecoverableKeyException e) {
            throw new UnrecoverableKeyException("密码错误");
        }
    }

    public static boolean createKeyStore(@NonNull String keystorePath, @NonNull String alias,
                                         @NonNull String password, @NonNull int year, String country,
                                         String name, String org, String unit, String province, String city) {
        try {
            char[] keyPassword = password.toCharArray();
            CertCreator.DistinguishedNameValues nameValues = new CertCreator.DistinguishedNameValues();
            nameValues.setCountry(country);
            nameValues.setCommonName(name);
            nameValues.setOrganization(org);
            nameValues.setOrganizationalUnit(unit);
            nameValues.setState(province);
            nameValues.setLocality(city);
            CertCreator.createKeystoreAndKey(keystorePath, keyPassword, "RSA", 2048, alias, keyPassword, "SHA1withRSA",
                    year, nameValues);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static ArrayList<ApkKeyStore> loadKeyStore() {
        ArrayList<ApkKeyStore> list = new ArrayList<>();
        try {
            PFile dir = new PFile(Pref.getKeyStorePath());
            if (!dir.exists()) {
                dir.mkdirs();
            }
            PFile[] files = dir.listFiles();
            for (PFile file : files) {
                if (file.isFile()) {
                    KeyStore keyStore = KeyStoreFileManager.loadKeyStore(file.getPath(), null);
                    String alias = keyStore.aliases().nextElement();
                    ApkKeyStore item = new ApkKeyStore();
                    item.setPath(file.getPath());
                    item.setName(file.getSimplifiedPath());
                    item.setAlias(alias);
                    String password = Pref.getKeyStorePassWord(file.getName());
                    item.setPassword(password);
                    item.setVerified(!password.isEmpty());
                    list.add(item);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static ApkKeyStore loadApkKeyStore(String path) {
        try {
            PFile file = new PFile(path);
            if (!file.exists()) {
                return null;
            }
            KeyStore keyStore = KeyStoreFileManager.loadKeyStore(file.getPath(), null);
            String alias = keyStore.aliases().nextElement();
            ApkKeyStore item = new ApkKeyStore();
            item.setPath(file.getPath());
            item.setName(file.getSimplifiedPath());
            item.setAlias(alias);
            String password = Pref.getKeyStorePassWord(file.getName());
            item.setPassword(password);
            item.setVerified(!password.isEmpty());
            return item;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean checkKeyStore(String keystorePath, String keyPassword) {
        try {
            File keystoreFile = new File(keystorePath);
            if (!keystoreFile.exists()) {
                return false;
            }
            char[] password = keyPassword.toCharArray();
            KeyStore keyStore = KeyStoreFileManager.loadKeyStore(keystorePath, null);
            String alias = keyStore.aliases().nextElement();
            X509Certificate publicKey = (X509Certificate) keyStore.getCertificate(alias);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
