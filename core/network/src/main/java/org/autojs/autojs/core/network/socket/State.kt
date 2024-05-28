package org.autojs.autojs.core.network.socket

data class State(val state: Int, val e: Throwable? = null) {
    companion object {
        const val DISCONNECTED = 0
        const val CONNECTING = 1
        const val CONNECTED = 2
        const val CONNECTION_FAILED = 3
        const val RECONNECTING = 4
        const val HANDSHAKE_TIMEOUT = 5
    }
}