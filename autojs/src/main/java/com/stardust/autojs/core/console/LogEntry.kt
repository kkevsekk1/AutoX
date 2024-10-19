package com.stardust.autojs.core.console

class LogEntry(
    val id: Int, val level: Int,
    val content: CharSequence,
    val newLine: Boolean = false,
) : Comparable<LogEntry> {

    override fun compareTo(other: LogEntry): Int {
        return 0
    }
}