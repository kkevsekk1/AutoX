package com.stardust.autojs.project

import com.google.gson.annotations.SerializedName

/**
 * Modified by wilinz on 2022/5/23
 */
data class SigningConfig(
    @SerializedName("alias")
    var alias: String? = null,
    @SerializedName("keystore")
    var keyStore: String? = null,
    @SerializedName("v1Sign")
    var v1Sign: Boolean = true,
    @SerializedName("v2Sign")
    var v2Sign: Boolean = true,
    @SerializedName("v3Sign")
    var v3Sign: Boolean = false,
    @SerializedName("v4Sign")
    var v4Sign: Boolean = false,
)