package com.valmo.gridlauncher.launcher

import com.valmo.gridlauncher.data.LauncherRepository
import com.valmo.gridlauncher.model.AppShortcut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeLauncherRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeLauncherRepository(
            configured = listOf(
                AppShortcut("com.example.maps", "Mapas"),
                AppShortcut("com.example.camera", "Câmera"),
            ),
            available = listOf(
                AppShortcut("com.example.camera", "Câmera"),
                AppShortcut("com.example.clock", "Relógio"),
                AppShortcut("com.example.maps", "Mapas"),
            ),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun refreshLoadsConfiguredShortcutsAndAvailability() = runTest(dispatcher) {
        repository.unavailablePackages += "com.example.camera"
        val viewModel = LauncherViewModel(repository, dispatcher)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.hasLoadedShortcuts)
        assertEquals(2, viewModel.uiState.value.shortcuts.size)
        assertTrue(viewModel.uiState.value.shortcuts.first().isAvailable)
        assertFalse(viewModel.uiState.value.shortcuts.last().isAvailable)
    }

    @Test
    fun editorPersistsCurrentOrderAndAppendsNewSelections() = runTest(dispatcher) {
        val viewModel = LauncherViewModel(repository, dispatcher)
        advanceUntilIdle()

        viewModel.openEditor()
        advanceUntilIdle()
        viewModel.toggleApp("com.example.camera")
        viewModel.toggleApp("com.example.clock")
        viewModel.saveEditor()
        advanceUntilIdle()

        assertEquals(
            listOf("com.example.maps", "com.example.clock"),
            repository.savedPackages,
        )
        assertFalse(viewModel.uiState.value.isEditing)
        assertEquals(
            repository.savedPackages,
            viewModel.uiState.value.shortcuts.map { it.shortcut.packageName },
        )
    }

    @Test
    fun appInfoAndUninstallActionsDelegateToRepository() = runTest(dispatcher) {
        val viewModel = LauncherViewModel(repository, dispatcher)
        val app = AppShortcut("com.example.maps", "Mapas")

        assertTrue(viewModel.openAppInfo(app))
        assertTrue(viewModel.requestUninstall(app))
        assertEquals("com.example.maps", repository.lastInfoPackage)
        assertEquals("com.example.maps", repository.lastUninstallPackage)
    }

    private class FakeLauncherRepository(
        configured: List<AppShortcut>,
        private val available: List<AppShortcut>,
    ) : LauncherRepository {
        private var configuredApps = configured
        val unavailablePackages = mutableSetOf<String>()
        var savedPackages: List<String> = emptyList()
            private set
        var lastInfoPackage: String? = null
            private set
        var lastUninstallPackage: String? = null
            private set

        override suspend fun configuredShortcuts(): List<AppShortcut> = configuredApps

        override suspend fun availableApps(): List<AppShortcut> = available

        override suspend fun saveConfiguredPackages(packageNames: List<String>) {
            savedPackages = packageNames
            configuredApps = packageNames.mapNotNull { packageName ->
                available.firstOrNull { it.packageName == packageName }
            }
        }

        override fun isAvailable(shortcut: AppShortcut): Boolean =
            shortcut.packageName !in unavailablePackages

        override fun launch(shortcut: AppShortcut): LaunchResult = LaunchResult.Success

        override fun openAppInfo(packageName: String): Boolean {
            lastInfoPackage = packageName
            return true
        }

        override fun requestUninstall(packageName: String): Boolean {
            lastUninstallPackage = packageName
            return true
        }
    }
}
