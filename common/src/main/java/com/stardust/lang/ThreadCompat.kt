package com.stardust.lang

/**
 * Created by Stardust on 2017/4/30.
 */
@Deprecated("建议使用自带的 Thread ")
open class ThreadCompat : Thread {
    constructor()
    constructor(target: Runnable?) : super(target)
    constructor(group: ThreadGroup?, target: Runnable?) : super(group, target)
    constructor(name: String) : super(name)
    constructor(group: ThreadGroup?, name: String) : super(group, name)
    constructor(target: Runnable?, name: String) : super(target, name)
    constructor(group: ThreadGroup?, target: Runnable?, name: String) : super(group, target, name)
    constructor(group: ThreadGroup?, target: Runnable?, name: String, stackSize: Long) : super(
        group,
        target,
        name,
        stackSize
    )
}
