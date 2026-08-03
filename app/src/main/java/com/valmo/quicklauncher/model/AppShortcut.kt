package com.valmo.quicklauncher.model

data class AppShortcut(
    val label: String,
    val packageName: String,
    val launchAction: String? = null,
)

