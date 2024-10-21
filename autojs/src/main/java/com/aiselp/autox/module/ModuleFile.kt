package com.aiselp.autox.module

import java.io.File

class ModuleFile(
    val file: File,
    val modelType: ModelType,
) {
    enum class ModelType {
        COMMONJS,
        ES_MODULE,
        JSON,
    }
}