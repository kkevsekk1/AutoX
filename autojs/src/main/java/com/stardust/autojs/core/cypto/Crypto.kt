package com.stardust.autojs.core.cypto

object Crypto {

    @JvmStatic
    fun fromHex(paramString: String): ByteArray {
        check(paramString.length % 2 == 0) { "Must have an even length" }
        return paramString.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    @JvmStatic
    fun toHex(paramArrayOfByte: ByteArray): String {
        return paramArrayOfByte.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
    }
}