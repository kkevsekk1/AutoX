package com.stardust.autojs.project

import com.google.gson.annotations.SerializedName

/**
 * Modified by wilinz on 2022/5/23
 */
class SigningConfig {
    @SerializedName("alias")
    var alias: String = ""

    @SerializedName("keystore")
    var keyStore: String = ""
}