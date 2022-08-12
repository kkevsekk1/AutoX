package com.stardust.util

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class AdvancedEncryptionStandard(private val key: ByteArray, private val initVector: String) {

    /**
     * Encrypts the given plain text
     *
     * @param plainText The plain text to encrypt
     */
    @Throws(Exception::class)
    fun encrypt(plainText: ByteArray): ByteArray {
        val cipher = getCipher(Cipher.ENCRYPT_MODE)
        return cipher.doFinal(plainText)
    }

    /**
     * Decrypts the given byte array
     *
     * @param cipherText The data to decrypt
     */
    fun decrypt(cipherText: ByteArray, start: Int = 0, end: Int = cipherText.size): ByteArray {
        val cipher = getCipher(Cipher.DECRYPT_MODE)
        return cipher.doFinal(cipherText, start, end - start)
    }

    fun decrypt(file: File, newFile: File) {
        file.inputStream().aesTo(newFile.outputStream(), Cipher.DECRYPT_MODE, true)
    }

    fun encrypt(file: File, newFile: File) {
        file.inputStream().aesTo(newFile.outputStream(), Cipher.ENCRYPT_MODE, true)
    }

    fun decrypt(input: InputStream, out: OutputStream, close: Boolean = false) {
        input.aesTo(out, Cipher.DECRYPT_MODE, close)
    }

    fun encrypt(input: InputStream, out: OutputStream, close: Boolean = false) {
        input.aesTo(out, Cipher.ENCRYPT_MODE, close)
    }

    fun InputStream.aesTo(out: OutputStream, mode: Int, close: Boolean = false) {
        if (close) {
            out.use {
                this.use {
                    aesTo(out, mode)
                }
            }
        } else aesTo(out, mode)
    }

    fun InputStream.aesTo(out: OutputStream, mode: Int) {
        val cipher = getCipher(mode)
        CipherInputStream(this, cipher).copyTo(out)
    }

    private fun getCipher(mode: Int): Cipher {
        val secretKey = SecretKeySpec(key, ALGORITHM)
        val cipher = Cipher.getInstance(FULL_ALGORITHM)
        val ivParameterSpec = IvParameterSpec(initVector.toByteArray())
        cipher.init(mode, secretKey, ivParameterSpec)
        return cipher
    }

    companion object {

        private const val ALGORITHM = "AES"
        private const val FULL_ALGORITHM = "AES/CBC/PKCS5Padding"
    }
}