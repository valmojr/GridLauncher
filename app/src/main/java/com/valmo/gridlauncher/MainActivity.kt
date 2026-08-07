package com.valmo.gridlauncher

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valmo.gridlauncher.data.AndroidLauncherRepository
import com.valmo.gridlauncher.launcher.LaunchResult
import com.valmo.gridlauncher.launcher.LauncherScreen
import com.valmo.gridlauncher.launcher.LauncherViewModel
import com.valmo.gridlauncher.ui.theme.GridLauncherTheme

class MainActivity : ComponentActivity() {
    private val repository by lazy { AndroidLauncherRepository(application) }
    private val launcherViewModel: LauncherViewModel by viewModels {
        LauncherViewModel.factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureFullScreen()

        setContent {
            val uiState by launcherViewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current
            val resources = LocalResources.current

            GridLauncherTheme {
                LauncherScreen(
                    uiState = uiState,
                    onShortcutClick = { shortcut ->
                        when (val result = launcherViewModel.launch(shortcut)) {
                            LaunchResult.Success -> Unit
                            is LaunchResult.Unavailable -> Toast.makeText(
                                context,
                                resources.getString(R.string.app_unavailable, result.label),
                                Toast.LENGTH_SHORT,
                            ).show()
                            is LaunchResult.Failed -> Toast.makeText(
                                context,
                                resources.getString(R.string.app_launch_failed, result.label),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    onEditClick = launcherViewModel::openEditor,
                    onToggleApp = launcherViewModel::toggleApp,
                    onSaveEdit = launcherViewModel::saveEditor,
                    onCancelEdit = launcherViewModel::cancelEditor,
                    onOpenAppInfo = { app ->
                        if (!launcherViewModel.openAppInfo(app)) {
                            Toast.makeText(
                                context,
                                resources.getString(R.string.app_action_failed, app.label),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    onUninstallApp = { app ->
                        if (!launcherViewModel.requestUninstall(app)) {
                            Toast.makeText(
                                context,
                                resources.getString(R.string.app_action_failed, app.label),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        launcherViewModel.refresh()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun configureFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
