package org.autojs.autojs.network

import org.autojs.autojs.network.api.GithubUpdateCheckApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

object VersionService2 {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val gitUpdateCheckApi = retrofit.create<GithubUpdateCheckApi>()
}