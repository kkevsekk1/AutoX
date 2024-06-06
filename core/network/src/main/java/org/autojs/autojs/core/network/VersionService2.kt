package org.autojs.autojs.core.network
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.utils.CacheControl
import io.ktor.http.HttpHeaders
import org.autojs.autojs.core.model.github.GithubReleaseInfo
import org.autojs.autojs.core.network.client.client

object VersionService2 {

    suspend fun getGithubLastReleaseInfo(): GithubReleaseInfo {
        return client.get("https://api.github.com/repos/kkevsekk1/AutoX/releases/latest") {
            header(HttpHeaders.CacheControl, CacheControl.NO_CACHE)
        }.body()
    }

    suspend fun getGithubReleaseInfoList(): List<GithubReleaseInfo> {
        return client.get("https://api.github.com/repos/kkevsekk1/AutoX/releases") {
            header(HttpHeaders.CacheControl, CacheControl.NO_CACHE)
        }.body()
    }

}