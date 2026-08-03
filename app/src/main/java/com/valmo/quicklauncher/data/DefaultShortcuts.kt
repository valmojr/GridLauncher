package com.valmo.quicklauncher.data

import android.provider.Settings
import com.valmo.quicklauncher.model.AppShortcut

object DefaultShortcuts {
    val items = listOf(
        AppShortcut(
            label = "CONFIGURAÇÕES",
            packageName = "com.android.settings",
            launchAction = Settings.ACTION_SETTINGS,
        ),
        AppShortcut(
            label = "CÂMERA",
            packageName = "com.motorola.camera3",
        ),
        AppShortcut(
            label = "MAPAS",
            packageName = "com.google.android.apps.maps",
        ),
        AppShortcut(
            label = "RELÓGIO",
            packageName = "com.google.android.deskclock",
        ),
        AppShortcut(
            label = "NAVEGADOR",
            packageName = "com.android.chrome",
        ),
        AppShortcut(
            label = "ATAK",
            packageName = "com.atakmap.app.civ",
        ),
    )
}
