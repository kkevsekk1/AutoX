package com.aiselp.autox.engine

class EventLoopQueue {
    private val queue = ArrayDeque<Runnable>()
    private val queueX = ArrayDeque<Runnable>()
    private var currentQueue = queue

    init {

    }

    fun executeQueue(): Boolean = synchronized(this) {
        val executeQueue = currentQueue
        if (executeQueue.isEmpty()) {
            return false
        }
        currentQueue = if (currentQueue === queue) {
            queueX
        } else queue
        executeQueue.forEach { it.run() }
        executeQueue.clear()
        return true
    }

    fun addTask(task: Runnable) = synchronized(this) {
        currentQueue.add(task)
    }

    companion object {
        private val TAG = "EventLoopQueue"
    }
}