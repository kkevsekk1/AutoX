package com.aiselp.autox.apkbuilder

import android.content.Context
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecuteResultHandler
import org.apache.commons.exec.DefaultExecutor
import java.io.File

object AAPT_Util {
    private const val aapt2LibName = "lib_bin_aapt2.so"
    private lateinit var aapt2LibPath: String
    private val executor by lazy { DefaultExecutor()}

    fun init(context: Context) {
        if (::aapt2LibPath.isInitialized) return
        aapt2LibPath = "${context.applicationInfo.nativeLibraryDir}/$aapt2LibName"
    }

    fun aapt2Optimize(inFile: File, outFile: File) {
        val args = listOf(
            "optimize",
            "-o",
            outFile.absolutePath,
            "--enable-sparse-encoding",
            inFile.absolutePath
        )
        val line = CommandLine(aapt2LibPath).apply {
            addArguments(args.toTypedArray())
        }
        DefaultExecuteResultHandler();
        executor.execute(line)
    }
}