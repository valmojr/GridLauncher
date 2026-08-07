package com.valmo.gridlauncher.launcher

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.valmo.gridlauncher.model.AppShortcut
import com.valmo.gridlauncher.ui.theme.GridLauncherTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LauncherScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun launcherShowsUnavailableStateAndHandlesClicks() {
        var clickedPackage: String? = null
        composeRule.setContent {
            GridLauncherTheme {
                LauncherScreen(
                    uiState = LauncherUiState(
                        shortcuts = listOf(
                            ShortcutUiState(
                                shortcut = AppShortcut(
                                    packageName = "com.example.maps",
                                    label = "MAPAS",
                                ),
                                isAvailable = true,
                            ),
                            ShortcutUiState(
                                shortcut = AppShortcut(
                                    packageName = "com.example.camera",
                                    label = "CÂMERA",
                                ),
                                isAvailable = false,
                            ),
                        ),
                        isLoading = false,
                        hasLoadedShortcuts = true,
                    ),
                    onShortcutClick = { clickedPackage = it.packageName },
                    onEditClick = {},
                    onToggleApp = {},
                    onSaveEdit = {},
                    onCancelEdit = {},
                    onOpenAppInfo = {},
                    onUninstallApp = {},
                )
            }
        }

        composeRule.onNodeWithText("MAPAS").assertIsDisplayed()
        composeRule.onNodeWithTag("shortcut:com.example.maps").performClick()
        composeRule.onNodeWithText("Não instalado").assertIsDisplayed()

        assertEquals("com.example.maps", clickedPackage)
    }

    @Test
    fun editorShowsInstalledAppsAndLongPressActions() {
        val maps = AppShortcut("com.example.maps", "Mapas")
        val camera = AppShortcut("com.example.camera", "Câmera")

        composeRule.setContent {
            GridLauncherTheme {
                LauncherScreen(
                    uiState = LauncherUiState(
                        shortcuts = listOf(ShortcutUiState(maps, true)),
                        availableApps = listOf(camera, maps),
                        selectedPackages = linkedSetOf(maps.packageName),
                        isEditing = true,
                        isLoading = false,
                        isEditorLoading = false,
                        hasLoadedShortcuts = true,
                    ),
                    onShortcutClick = {},
                    onEditClick = {},
                    onToggleApp = {},
                    onSaveEdit = {},
                    onCancelEdit = {},
                    onOpenAppInfo = {},
                    onUninstallApp = {},
                )
            }
        }

        composeRule.onNodeWithText("Atalhos").assertIsDisplayed()
        composeRule.onNodeWithText("Aplicativos instalados").assertIsDisplayed()
        composeRule.onNodeWithTag("installed-app:com.example.camera").performTouchInput {
            longClick()
        }

        composeRule.onNodeWithText("Abrir").assertIsDisplayed()
        composeRule.onNodeWithText("Adicionar aos atalhos").assertIsDisplayed()
        composeRule.onNodeWithText("Informações do App").assertIsDisplayed()
        composeRule.onNodeWithText("Desinstalar o app").assertIsDisplayed()
    }
}
