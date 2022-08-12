package com.stardust.autojs.project

import com.google.gson.annotations.SerializedName

/**
 * Modified by wilinz on 2022/5/23
 */
data class SigningConfig (
    @SerializedName("alias")
    var alias: String? = null,
    @SerializedName("keystore")
    var keyStore: String? = null
)