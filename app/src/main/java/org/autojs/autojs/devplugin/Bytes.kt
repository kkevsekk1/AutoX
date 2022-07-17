package org.autojs.autojs.devplugin

data class Bytes(val md5: String, val bytes: ByteArray, val timestamp: Long = System.currentTimeMillis()) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Bytes

        if (md5 != other.md5) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = md5.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
