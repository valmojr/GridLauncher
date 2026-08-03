package com.valmo.quicklauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultShortcutsTest {
    @Test
    fun defaultShortcutsHaveValidUniquePackages() {
        val shortcuts = DefaultShortcuts.items

        assertTrue(shortcuts.all { it.label.isNotBlank() && it.packageName.isNotBlank() })
        assertEquals(shortcuts.size, shortcuts.map { it.packageName }.distinct().size)
        assertTrue(shortcuts.any { it.packageName == "com.android.settings" })
        assertTrue(shortcuts.any { it.packageName == "com.atakmap.app.civ" })
    }
}

