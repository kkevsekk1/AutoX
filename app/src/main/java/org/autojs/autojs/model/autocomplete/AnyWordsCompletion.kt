package org.autojs.autojs.model.autocomplete

import android.text.Editable
import org.autojs.autojs.ui.widget.SimpleTextWatcher
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.Volatile

/**
 * Created by Stardust on 2018/2/26.
 */
private const val PATTERN = "[\\W]"
class AnyWordsCompletion(private val mExecutorService: ExecutorService) : SimpleTextWatcher() {
    @Volatile
    private var mDictionaryTree: DictionaryTree<String>? = null
    private val mExecuteId = AtomicInteger()

    override fun afterTextChanged(s: Editable) {
        val str = s.toString()
        val id = mExecuteId.incrementAndGet()
        mExecutorService.execute { splitWords(id, str) }
    }

    private fun splitWords(id: Int, s: String) {
        if (id != mExecuteId.get()) {
            return
        }
        val tree = DictionaryTree<String>()
        val words = s.split(PATTERN.toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        for (word in words) {
            if (id != mExecuteId.get()) {
                return
            }
            tree.putWord(word, word)
        }
        mDictionaryTree = tree
    }

    fun findCodeCompletion(completions: MutableList<CodeCompletion?>, wordPrefill: String) {
        if (mDictionaryTree == null) return
        val result = mDictionaryTree!!.searchByPrefill(wordPrefill)
        for (entry in result) {
            completions.add(CodeCompletion(entry.tag, null, wordPrefill.length))
        }
    }
}
