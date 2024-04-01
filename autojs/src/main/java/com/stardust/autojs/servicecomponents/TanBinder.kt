package com.stardust.autojs.servicecomponents

import android.os.IBinder
import android.os.IBinder.LAST_CALL_TRANSACTION
import android.os.Parcel

data class TanBinder(
    val binder: IBinder,
    var action: Int = LAST_CALL_TRANSACTION,
    val data: Parcel,
    val reply: Parcel,
    var flags: Int = 0
) {
    fun send() {
        binder.transact(action, data, reply, flags)
    }
}