package com.valmo.gridlauncher.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.valmo.gridlauncher.data.LauncherRepository
import com.valmo.gridlauncher.model.AppShortcut
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ShortcutUiState(
    val shortcut: AppShortcut,
    val isAvailable: Boolean,
)

data class LauncherUiState(
    val shortcuts: List<ShortcutUiState> = emptyList(),
    val availableApps: List<AppShortcut> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isEditorLoading: Boolean = false,
    val hasLoadedShortcuts: Boolean = false,
)

private data class RefreshData(
    val shortcuts: List<ShortcutUiState>,
    val availableApps: List<AppShortcut>?,
)

private data class EditorData(
    val shortcuts: List<ShortcutUiState>,
    val availableApps: List<AppShortcut>,
)

class LauncherViewModel(
    private val repository: LauncherRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LauncherUiState())
    private var refreshJob: Job? = null
    private var editorJob: Job? = null

    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val refreshEditor = _uiState.value.isEditing
            val result = runCatching {
                withContext(ioDispatcher) {
                    val shortcuts = repository.configuredShortcuts().map { shortcut ->
                        ShortcutUiState(
                            shortcut = shortcut,
                            isAvailable = repository.isAvailable(shortcut),
                        )
                    }
                    RefreshData(
                        shortcuts = shortcuts,
                        availableApps = if (refreshEditor) repository.availableApps() else null,
                    )
                }
            }

            result.onSuccess { data ->
                _uiState.update { current ->
                    current.copy(
                        shortcuts = data.shortcuts,
                        availableApps = data.availableApps ?: current.availableApps,
                        selectedPackages = if (current.isEditing) {
                            current.selectedPackages
                        } else {
                            data.shortcuts.mapTo(linkedSetOf()) { it.shortcut.packageName }
                        },
                        isLoading = false,
                        hasLoadedShortcuts = true,
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasLoadedShortcuts = true,
                    )
                }
            }
        }
    }

    fun launch(shortcut: AppShortcut): LaunchResult = repository.launch(shortcut)

    fun openAppInfo(app: AppShortcut): Boolean = repository.openAppInfo(app.packageName)

    fun requestUninstall(app: AppShortcut): Boolean = repository.requestUninstall(app.packageName)

    fun openEditor() {
        editorJob?.cancel()
        _uiState.update { it.copy(isEditing = true, isEditorLoading = true) }
        editorJob = viewModelScope.launch {
            val result = runCatching {
                withContext(ioDispatcher) {
                    val shortcuts = repository.configuredShortcuts().map { shortcut ->
                        ShortcutUiState(
                            shortcut = shortcut,
                            isAvailable = repository.isAvailable(shortcut),
                        )
                    }
                    EditorData(
                        shortcuts = shortcuts,
                        availableApps = repository.availableApps(),
                    )
                }
            }

            result.onSuccess { data ->
                _uiState.update { current ->
                    current.copy(
                        shortcuts = data.shortcuts,
                        availableApps = data.availableApps,
                        selectedPackages = data.shortcuts.mapTo(linkedSetOf()) {
                            it.shortcut.packageName
                        },
                        isEditorLoading = false,
                        hasLoadedShortcuts = true,
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isEditorLoading = false) }
            }
        }
    }

    fun toggleApp(packageName: String) {
        _uiState.update { current ->
            val selected = current.selectedPackages.toMutableSet()
            if (!selected.add(packageName)) {
                selected.remove(packageName)
            }
            current.copy(selectedPackages = selected)
        }
    }

    fun cancelEditor() {
        editorJob?.cancel()
        _uiState.update {
            it.copy(
                isEditing = false,
                isEditorLoading = false,
                selectedPackages = it.shortcuts.mapTo(linkedSetOf()) { state ->
                    state.shortcut.packageName
                },
            )
        }
    }

    fun saveEditor() {
        val orderedPackages = _uiState.value.selectedPackages.toList()
        if (orderedPackages.isEmpty()) {
            return
        }

        editorJob?.cancel()
        editorJob = viewModelScope.launch {
            withContext(ioDispatcher) {
                repository.saveConfiguredPackages(orderedPackages)
            }
            _uiState.update { it.copy(isEditing = false, isEditorLoading = false) }
            refresh()
        }
    }

    companion object {
        fun factory(repository: LauncherRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LauncherViewModel(repository) as T
            }
    }
}
