package org.autojs.autojs.ui.main.scripts

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.leinardi.android.speeddial.compose.FabWithLabel
import com.leinardi.android.speeddial.compose.SpeedDial
import com.leinardi.android.speeddial.compose.SpeedDialScope
import com.leinardi.android.speeddial.compose.SpeedDialState
import com.stardust.app.GlobalAppContext.get
import com.stardust.util.IntentUtil
import org.autojs.autojs.Pref
import org.autojs.autojs.external.fileprovider.AppFileProvider
import org.autojs.autojs.model.explorer.ExplorerDirPage
import org.autojs.autojs.model.explorer.Explorers
import org.autojs.autojs.model.script.Scripts.edit
import org.autojs.autojs.ui.build.ProjectConfigActivity
import org.autojs.autojs.ui.build.ProjectConfigActivity_
import org.autojs.autojs.ui.common.ScriptOperations
import org.autojs.autojs.ui.explorer.ExplorerViewKt
import org.autojs.autojs.ui.main.rememberExternalStoragePermissionsState
import org.autojs.autojs.ui.main.showExternalStoragePermissionToast
import org.autojs.autojs.ui.viewmodel.ExplorerItemList.SortConfig
import org.autojs.autojs.ui.widget.fillMaxSize
import org.autojs.autoxjs.R

/**
 * Created by wilinz on 2022/7/15.
 */
class ScriptListFragment : Fragment() {

    val explorerView by lazy { ExplorerViewKt(this.requireContext()) }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        explorerView.setUpViews()
        return ComposeView(requireContext()).apply {
            setContent {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
//                        FloatingButton()
                    },
                ) {
                    AndroidView(
                        modifier = Modifier.padding(it),
                        factory = { explorerView }
                    )
                }
            }
        }
    }

    @OptIn(
        ExperimentalMaterialApi::class,
        ExperimentalAnimationApi::class
    )
    @Composable
    private fun FloatingButton() {
        var speedDialState by rememberSaveable { mutableStateOf(SpeedDialState.Collapsed) }
        val context = LocalContext.current
        SpeedDial(
            state = speedDialState,
            onFabClick = { expanded ->
                speedDialState =
                    if (expanded) SpeedDialState.Collapsed else SpeedDialState.Expanded
            },
            fabClosedContent = {
                Icon(
                    Icons.Default.Add,
                    null,
                    tint = MaterialTheme.colors.onSecondary
                )
            },
            fabOpenedContent = {
                Icon(
                    Icons.Default.Close,
                    null,
                    tint = MaterialTheme.colors.onSecondary
                )
            },
        ) {
            this.items(context)
        }
    }

    private fun SpeedDialScope.items(context: Context) {
        item {
            NewDirectory(context)
        }
        item {
            NewFile(context)
        }
        item {
            ImportFile(context)
        }
        item {
            NewProject(context)
        }
    }

    @OptIn(
        ExperimentalMaterialApi::class
    )
    @Composable
    private fun NewProject(context: Context) {
        FabWithLabel(
            onClick = {
                val explorerView = this@ScriptListFragment.explorerView
                ProjectConfigActivity_.intent(context)
                    .extra(
                        ProjectConfigActivity.EXTRA_PARENT_DIRECTORY,
                        explorerView.currentPage?.path
                    )
                    .extra(ProjectConfigActivity.EXTRA_NEW_PROJECT, true)
                    .start()
            },
            labelContent = { Text(text = stringResource(id = R.string.text_project)) },
        ) {
            Icon(painterResource(id = R.drawable.ic_project2), null)
        }
    }

    @OptIn(
        ExperimentalMaterialApi::class,
        ExperimentalPermissionsApi::class
    )
    @Composable
    private fun ImportFile(context: Context) {
        val permission = rememberExternalStoragePermissionsState {
            if (it) getScriptOperations(
                context,
                this@ScriptListFragment
            ).importFile()
            else showExternalStoragePermissionToast(context)
        }
        FabWithLabel(
            onClick = {
                permission.launchMultiplePermissionRequest()
            },
            labelContent = { Text(text = stringResource(id = R.string.text_import)) },
        ) {
            Icon(
                painterResource(id = R.drawable.ic_floating_action_menu_open),
                null
            )
        }
    }

    @OptIn(
        ExperimentalMaterialApi::class,
        ExperimentalPermissionsApi::class
    )
    @Composable
    private fun NewFile(context: Context) {
        val permission = rememberExternalStoragePermissionsState {
            if (it) getScriptOperations(
                context,
                this@ScriptListFragment
            ).newFile()
            else showExternalStoragePermissionToast(context)
        }
        FabWithLabel(
            onClick = {
                permission.launchMultiplePermissionRequest()
            },
            labelContent = { Text(text = stringResource(id = R.string.text_file)) },
        ) {
            Icon(
                painterResource(id = R.drawable.ic_floating_action_menu_file),
                null
            )
        }
    }

    @OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterialApi::class)
    @Composable
    private fun NewDirectory(context: Context) {
        val permission = rememberExternalStoragePermissionsState {
            if (it) getScriptOperations(
                context,
                this@ScriptListFragment
            ).newDirectory()
            else showExternalStoragePermissionToast(context)
        }
        FabWithLabel(
            onClick = {
                permission.launchMultiplePermissionRequest()
            },
            labelContent = { Text(text = stringResource(id = R.string.text_directory)) },
        ) {
            Icon(
                painterResource(id = R.drawable.ic_floating_action_menu_dir),
                null
            )
        }
    }

    fun ExplorerViewKt.setUpViews() {
        fillMaxSize()
        sortConfig = SortConfig.from(
            PreferenceManager.getDefaultSharedPreferences(
                requireContext()
            )
        )
        setExplorer(
            Explorers.workspace(),
            ExplorerDirPage.createRoot(Pref.getScriptDirPath())
        )
        setOnItemClickListener { _, item ->
            item?.let {
                if (item.isEditable) {
                    edit(requireContext(), item.toScriptFile());
                } else {
                    IntentUtil.viewFile(get(), item.path, AppFileProvider.AUTHORITY)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        explorerView.sortConfig?.saveInto(
            PreferenceManager.getDefaultSharedPreferences(
                requireContext()
            )
        )
    }

    private fun getScriptOperations(
        context: Context,
        scriptListFragment: ScriptListFragment
    ): ScriptOperations {
        val explorerView = scriptListFragment.explorerView
        return ScriptOperations(
            context,
            explorerView,
            explorerView.currentPage
        )
    }

    fun onBackPressed(): Boolean {
        if (explorerView.canGoBack()) {
            explorerView.goBack()
            return true
        }
        return false
    }

    companion object {
        private const val TAG = "MyScriptListFragment"
    }


}