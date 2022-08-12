package com.stardust.pio

import java.io.InputStream
import java.io.OutputStream

fun InputStream.copyToAndClose(out:OutputStream){
    this.use { input->
        out.use { out->
            input.copyTo(out)
        }
    }
}