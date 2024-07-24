package com.stardust.autojs.servicecomponents

import android.os.Binder
import android.os.IBinder
import android.os.Parcel

interface BinderConsoleListener {
    fun onPrintln(text: String)

    class ServerInterface(val binder: IBinder) : BinderConsoleListener {
        override fun onPrintln(text: String) {
            val data = Parcel.obtain()
            try {
                data.writeInterfaceToken(DESCRIPTOR)
                data.writeString(text)
                TanBinder(binder, 5, data, null, 1).send()
            } finally {
                data.recycle()
            }
        }

    }

    class ClientInterface(
        val listener: BinderConsoleListener
    ) : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            data.enforceInterface(DESCRIPTOR)
            val text = data.readString() ?: ""
            listener.onPrintln(text)
            return super.onTransact(code, data, reply, flags)
        }
    }

    companion object {
        private const val DESCRIPTOR =
            "com.stardust.autojs.servicecomponents.BinderConsoleListener"
    }
}