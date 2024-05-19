package org.autojs.autojs.model.autocomplete

/**
 * Created by Stardust on 2017/9/27.
 */
class CodeCompletions(private val mCompletions: List<CodeCompletion>) {
    fun size(): Int {
        return mCompletions.size
    }

    fun getHint(position: Int): String {
        return mCompletions[position].hint
    }

    fun get(pos: Int): CodeCompletion {
        return mCompletions[pos]
    }

    companion object {
        fun just(hints: List<String>): CodeCompletions {
            return CodeCompletions(hints.map { CodeCompletion(it, null, 0) })
        }
    }
}
