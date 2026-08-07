package com.valmo.gridlauncher.data

import android.provider.Settings
import androidx.annotation.StringRes
import com.valmo.gridlauncher.R

data class DefaultShortcut(
    val packageName: String,
    @param:StringRes val fallbackLabelRes: Int,
    val launchAction: String? = null,
)

object DefaultShortcuts {
    val items = listOf(
        DefaultShortcut(
            packageName = "com.android.settings",
            fallbackLabelRes = R.string.default_settings,
            launchAction = Settings.ACTION_SETTINGS,
        ),
        DefaultShortcut(
            packageName = "com.motorola.camera3",
            fallbackLabelRes = R.string.default_camera,
        ),
        DefaultShortcut(
            packageName = "com.google.android.apps.maps",
            fallbackLabelRes = R.string.default_maps,
        ),
        DefaultShortcut(
            packageName = "com.google.android.deskclock",
            fallbackLabelRes = R.string.default_clock,
        ),
        DefaultShortcut(
            packageName = "com.android.chrome",
            fallbackLabelRes = R.string.default_browser,
        ),
        DefaultShortcut(
            packageName = "com.atakmap.app.civ",
            fallbackLabelRes = R.string.default_atak,
        ),
    )

    private val byPackage = items.associateBy(DefaultShortcut::packageName)

    fun configFor(packageName: String): DefaultShortcut? = byPackage[packageName]
}
