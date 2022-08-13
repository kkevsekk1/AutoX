package org.autojs.autojs.build

import java.io.Serializable

/**
 * @author wilinz
 * @date 2022/5/23
 */
data class ApkKeyStore(
    var path: String? = null,
    var name: String? = null,
    var alias: String? = null,
    var password: String? = null,
    var isVerified: Boolean = false
) : Serializable