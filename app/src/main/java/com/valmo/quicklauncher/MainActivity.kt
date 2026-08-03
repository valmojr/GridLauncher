package com.valmo.quicklauncher

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valmo.quicklauncher.launcher.LaunchResult
import com.valmo.quicklauncher.launcher.LauncherScreen
import com.valmo.quicklauncher.launcher.LauncherViewModel
import com.valmo.quicklauncher.ui.theme.QuickLauncherTheme

class MainActivity : ComponentActivity() {
    private val launcherViewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureFullScreen()

        setContent {
            val shortcuts by launcherViewModel.shortcuts.collectAsStateWithLifecycle()
            val context = LocalContext.current

            QuickLauncherTheme {
                LauncherScreen(
                    shortcuts = shortcuts,
                    onShortcutClick = { shortcut ->
                        when (val result = launcherViewModel.launch(shortcut)) {
                            LaunchResult.Success -> Unit
                            is LaunchResult.Unavailable -> Toast.makeText(
                                context,
                                "${result.label} não está instalado ou não pode ser aberto.",
                                Toast.LENGTH_SHORT,
                            ).show()

                            is LaunchResult.Failed -> Toast.makeText(
                                context,
                                "Não foi possível abrir ${result.label}.",
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

