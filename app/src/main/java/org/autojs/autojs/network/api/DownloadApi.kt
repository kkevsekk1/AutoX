package org.autojs.autojs.network.api

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * Created by Stardust on 2017/12/6.
 */
interface DownloadApi {
    @GET
    @Streaming
    fun download(@Url url: String): Observable<ResponseBody>
}
