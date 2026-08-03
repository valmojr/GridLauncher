package com.valmo.quicklauncher.launcher

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.valmo.quicklauncher.data.DefaultShortcuts
import com.valmo.quicklauncher.model.AppShortcut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShortcutUiState(
    val shortcut: AppShortcut,
    val icon: ImageBitmap?,
    val isAvailable: Boolean,
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val appLauncher = AppLauncher(application)
    private val _shortcuts = MutableStateFlow<List<ShortcutUiState>>(emptyList())

    val shortcuts: StateFlow<List<ShortcutUiState>> = _shortcuts.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _shortcuts.value = DefaultShortcuts.items.map { shortcut ->
                ShortcutUiState(
                    shortcut = shortcut,
                    icon = appLauncher.loadIcon(shortcut)?.toImageBitmapSafely(shortcut),
                    isAvailable = appLauncher.isAvailable(shortcut),
                )
            }
        }
    }

    fun launch(shortcut: AppShortcut): LaunchResult = appLauncher.launch(shortcut)

    private fun Drawable.toImageBitmapSafely(shortcut: AppShortcut): ImageBitmap? =
        try {
            val width = intrinsicWidth.takeIf { it > 0 }?.coerceAtMost(MAX_ICON_PIXELS)
                ?: DEFAULT_ICON_PIXELS
            val height = intrinsicHeight.takeIf { it > 0 }?.coerceAtMost(MAX_ICON_PIXELS)
                ?: DEFAULT_ICON_PIXELS
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            setBounds(0, 0, width, height)
            draw(Canvas(bitmap))
            bitmap.asImageBitmap()
        } catch (error: RuntimeException) {
            Log.w(TAG, "Could not convert icon for ${shortcut.packageName}", error)
            null
        }

    private companion object {
        const val TAG = "QuickLauncher"
        const val DEFAULT_ICON_PIXELS = 96
        const val MAX_ICON_PIXELS = 192
    }
}

