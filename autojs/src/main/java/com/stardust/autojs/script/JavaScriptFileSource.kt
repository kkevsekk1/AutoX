package com.stardust.autojs.script

import com.stardust.autojs.script.EncryptedScriptFileHeader.getHeaderFlags
import com.stardust.pio.PFiles.read
import com.stardust.pio.UncheckedIOException
import java.io.File
import java.io.FileNotFoundException
import java.io.Reader

/**
 * Created by Stardust on 2017/4/2.
 */
class JavaScriptFileSource : JavaScriptSource {
    var file: File
        private set
    private var mScript: String? = null
    private var mCustomsName = false

    constructor(file: File) : super(file.nameWithoutExtension) {
        this.file = file
    }

    constructor(path: String) : this(File(path)) {}
    constructor(name: String, file: File) : super(name) {
        mCustomsName = true
        this.file = file
    }

    override fun parseExecutionMode(): Int {
        val flags = getHeaderFlags(file)
        return if (flags == EncryptedScriptFileHeader.FLAG_INVALID_FILE) {
            super.parseExecutionMode()
        } else flags.toInt()
    }

    override val script: String
        get() = mScript ?: read(file).also { mScript = it }

    override val scriptReader: Reader?
        get() {
            return try {
                file.reader()
            } catch (e: FileNotFoundException) {
                throw UncheckedIOException(e)
            }
        }

    override fun toString(): String {
        return if (mCustomsName) {
            super.toString()
        } else file.toString()
    }
}