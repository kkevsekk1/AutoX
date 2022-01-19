package org.autojs.autojs.network.api;

import org.autojs.autojs.BuildConfig;
import org.autojs.autojs.network.entity.VersionInfo;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

/**
 * Created by Stardust on 2017/9/20.
 */

public interface UpdateCheckApi {

    @GET("http://file.ar01.cn/appstore/app/checkversion" + BuildConfig.APPID)
    @Headers("Cache-Control: no-cache")
    Observable<VersionInfo> checkForUpdates();

    @GET("http://112.74.161.35:9317/device/getinfo")
    @Headers("Cache-Control: no-cache")
    Observable<ResponseBody> deviceInfo(@Query("imei") String imei, @Query("sfid") String sfid, @Query("time") long time);

}
