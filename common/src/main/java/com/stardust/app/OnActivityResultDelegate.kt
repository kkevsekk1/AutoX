package com.stardust.app

import android.content.Intent
import androidx.collection.SparseArrayCompat

/**
 * Created by Stardust on 2017/3/5.
 */
interface OnActivityResultDelegate {
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)

    interface DelegateHost {
        val onActivityResultDelegateMediator: Mediator
    }

    class Mediator : OnActivityResultDelegate {
        private val mSpecialDelegate = SparseArrayCompat<OnActivityResultDelegate>()
        private val mDelegates = mutableListOf<OnActivityResultDelegate>()

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            val delegate = mSpecialDelegate[requestCode]
            delegate?.onActivityResult(requestCode, resultCode, data)
            for (d in mDelegates) {
                d.onActivityResult(requestCode, resultCode, data)
            }
        }

        fun addDelegate(delegate: OnActivityResultDelegate) {
            mDelegates.add(delegate)
        }

        fun addDelegate(requestCode: Int, delegate: OnActivityResultDelegate) {
            mSpecialDelegate.put(requestCode, delegate)
        }

        fun removeDelegate(delegate: OnActivityResultDelegate) {
            if (mDelegates.remove(delegate)) {
                mSpecialDelegate.removeAt(mSpecialDelegate.indexOfValue(delegate))
            }
        }
    }
}
