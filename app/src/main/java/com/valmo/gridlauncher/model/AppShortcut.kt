package com.valmo.gridlauncher.model

data class AppShortcut(
    val packageName: String,
    val label: String,
    val launchAction: String? = null,
)
