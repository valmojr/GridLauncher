package com.valmo.gridlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultShortcutsTest {
    @Test
    fun defaultShortcutsHaveValidUniquePackages() {
        val shortcuts = DefaultShortcuts.items

        assertTrue(shortcuts.all { it.packageName.isNotBlank() })
        assertEquals(shortcuts.size, shortcuts.map { it.packageName }.distinct().size)
        assertNotNull(DefaultShortcuts.configFor("com.android.settings"))
        assertNotNull(DefaultShortcuts.configFor("com.atakmap.app.civ"))
    }
}
