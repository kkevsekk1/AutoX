package org.autojs.autojs.ui.main.drawer

import android.app.Application
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.autojs.autoxjs.R
import org.autojs.autojs.network.VersionService2
import org.autojs.autojs.network.entity.GithubReleaseInfo
import org.autojs.autojs.network.entity.isLatestVersion

class DrawerViewModel(private val context: Application) : AndroidViewModel(context) {

    var githubReleaseInfo by mutableStateOf<GithubReleaseInfo?>(null)
        private set

    fun checkUpdate(onUpdate: () -> Unit = {},onComplete: () -> Unit = {}) {
        kotlin.runCatching { }
        Toast.makeText(
            context,
            context.getString(R.string.text_checking_for_updates),
            Toast.LENGTH_SHORT
        ).show()
        viewModelScope.launch {
            try {
                var releaseInfo = VersionService2.gitUpdateCheckApi.getGithubLastReleaseInfo()

                var isLatestVersion = releaseInfo.isLatestVersion()
                if (isLatestVersion == null) {
                    //Get release list
                    VersionService2.gitUpdateCheckApi.getGithubReleaseInfoList()
                        .firstOrNull { it.targetCommitish == "dev-test" && !it.prerelease }
                        ?.let {
                            releaseInfo = it
                            isLatestVersion = releaseInfo.isLatestVersion()
                        }
                }
                if (isLatestVersion == null) {
                    //Can't find information
                    versionInformationNotFound()
                    return@launch
                }
                if (isLatestVersion == true) {
                    //is the latest version
                    Toast.makeText(
                        context,
                        context.getString(R.string.text_is_latest_version),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    //new version
                    githubReleaseInfo = releaseInfo
                    onUpdate()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    context,
                    context.getString(
                        R.string.text_check_update_error,
                        e.localizedMessage ?: ""
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }finally {
                onComplete()
            }
        }

    }

    private fun versionInformationNotFound() {
        Toast.makeText(
            context,
            context.getString(
                R.string.text_check_update_error,
                context.getString(R.string.text_update_information_not_found)
            ),
            Toast.LENGTH_SHORT
        ).show()
    }
}