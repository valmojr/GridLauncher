package com.valmo.gridlauncher.launcher

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Process
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.valmo.gridlauncher.R
import com.valmo.gridlauncher.model.AppShortcut
import com.valmo.gridlauncher.ui.theme.GridLauncherTheme
import com.valmo.gridlauncher.ui.theme.ShortcutDivider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LauncherScreen(
    uiState: LauncherUiState,
    onShortcutClick: (AppShortcut) -> Unit,
    onEditClick: () -> Unit,
    onToggleApp: (String) -> Unit,
    onSaveEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onOpenAppInfo: (AppShortcut) -> Unit,
    onUninstallApp: (AppShortcut) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isEditing) {
        AppSelectionScreen(
            currentShortcuts = uiState.shortcuts,
            apps = uiState.availableApps,
            selectedPackages = uiState.selectedPackages,
            isLoading = uiState.isEditorLoading,
            onOpenApp = onShortcutClick,
            onToggleApp = onToggleApp,
            onAppInfo = onOpenAppInfo,
            onUninstall = onUninstallApp,
            onSave = onSaveEdit,
            onCancel = onCancelEdit,
            modifier = modifier,
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when {
            uiState.shortcuts.isNotEmpty() -> ShortcutRow(
                shortcuts = uiState.shortcuts,
                onShortcutClick = onShortcutClick,
                onShortcutLongClick = onEditClick,
                modifier = Modifier.fillMaxSize(),
            )

            !uiState.hasLoadedShortcuts || uiState.isLoading -> LoadingLauncher(
                modifier = Modifier.fillMaxSize(),
            )

            else -> EmptyLauncher(
                message = stringResource(R.string.no_shortcuts_configured),
                modifier = Modifier.fillMaxSize(),
            )
        }

        TextButton(
            onClick = onEditClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
        ) {
            Text(text = stringResource(R.string.edit_shortcuts))
        }
    }
}

@Composable
private fun ShortcutRow(
    shortcuts: List<ShortcutUiState>,
    onShortcutClick: (AppShortcut) -> Unit,
    onShortcutLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val visibleTiles = shortcuts.size.coerceIn(1, MAX_VISIBLE_TILES)
        val tileWidth = maxWidth / visibleTiles.toFloat()

        LazyRow(modifier = Modifier.fillMaxSize()) {
            items(
                items = shortcuts,
                key = { it.shortcut.packageName },
            ) { state ->
                Row(
                    modifier = Modifier
                        .width(tileWidth)
                        .fillMaxHeight(),
                ) {
                    ShortcutTile(
                        state = state,
                        onClick = { onShortcutClick(state.shortcut) },
                        onLongClick = onShortcutLongClick,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(ShortcutDivider),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortcutTile(
    state: ShortcutUiState,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .testTag("shortcut:${state.shortcut.packageName}")
            .combinedClickable(
                onClick = { if (state.isAvailable) onClick() },
                onLongClick = onLongClick,
            )
            .padding(horizontal = 8.dp, vertical = 20.dp)
            .alpha(if (state.isAvailable) 1f else 0.48f),
    ) {
        val iconSize = minOf(maxWidth * 0.42f, maxHeight * 0.34f)
            .coerceIn(36.dp, 96.dp)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = state.shortcut.label,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(18.dp))
            AppIcon(
                shortcut = state.shortcut,
                modifier = Modifier.size(iconSize),
            )
            if (!state.isAvailable) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.not_installed),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun AppSelectionScreen(
    currentShortcuts: List<ShortcutUiState>,
    apps: List<AppShortcut>,
    selectedPackages: Set<String>,
    isLoading: Boolean,
    onOpenApp: (AppShortcut) -> Unit,
    onToggleApp: (String) -> Unit,
    onAppInfo: (AppShortcut) -> Unit,
    onUninstall: (AppShortcut) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appsByPackage = remember(apps, currentShortcuts) {
        buildMap {
            currentShortcuts.forEach { put(it.shortcut.packageName, it.shortcut) }
            apps.forEach { put(it.packageName, it) }
        }
    }
    val selectedApps = selectedPackages.mapNotNull(appsByPackage::get)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val isWide = maxWidth >= LANDSCAPE_EDITOR_MIN_WIDTH

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            EditorHeader(
                selectedCount = selectedPackages.size,
                canSave = selectedPackages.isNotEmpty() && !isLoading,
                onSave = onSave,
                onCancel = onCancel,
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isWide) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    SelectedShortcutsPanel(
                        selectedApps = selectedApps,
                        onRemove = { onToggleApp(it.packageName) },
                        modifier = Modifier
                            .weight(0.36f)
                            .fillMaxHeight(),
                    )
                    InstalledAppsPanel(
                        apps = apps,
                        selectedPackages = selectedPackages,
                        isLoading = isLoading,
                        onOpenApp = onOpenApp,
                        onToggleApp = onToggleApp,
                        onAppInfo = onAppInfo,
                        onUninstall = onUninstall,
                        modifier = Modifier
                            .weight(0.64f)
                            .fillMaxHeight(),
                    )
                }
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    SelectedShortcutsPanel(
                        selectedApps = selectedApps,
                        onRemove = { onToggleApp(it.packageName) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InstalledAppsPanel(
                        apps = apps,
                        selectedPackages = selectedPackages,
                        isLoading = isLoading,
                        onOpenApp = onOpenApp,
                        onToggleApp = onToggleApp,
                        onAppInfo = onAppInfo,
                        onUninstall = onUninstall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorHeader(
    selectedCount: Int,
    canSave: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.choose_apps),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.selected_app_count, selectedCount),
                color = Color.LightGray,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        TextButton(onClick = onCancel) {
            Text(text = stringResource(R.string.cancel))
        }
        Button(
            enabled = canSave,
            onClick = onSave,
        ) {
            Text(text = stringResource(R.string.save))
        }
    }
}

@Composable
private fun SelectedShortcutsPanel(
    selectedApps: List<AppShortcut>,
    onRemove: (AppShortcut) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color(0xFF101010), RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Text(
            text = stringResource(R.string.shortcuts_title),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = ShortcutDivider)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                items = selectedApps,
                key = { it.packageName },
            ) { app ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    AppIcon(
                        shortcut = app,
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = app.label,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onRemove(app) }) {
                        Text(text = stringResource(R.string.remove))
                    }
                }
            }
        }
    }
}

@Composable
private fun InstalledAppsPanel(
    apps: List<AppShortcut>,
    selectedPackages: Set<String>,
    isLoading: Boolean,
    onOpenApp: (AppShortcut) -> Unit,
    onToggleApp: (String) -> Unit,
    onAppInfo: (AppShortcut) -> Unit,
    onUninstall: (AppShortcut) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuPackage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .background(Color(0xFF101010), RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Text(
            text = stringResource(R.string.installed_apps_title),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.long_press_app_actions),
            color = Color.LightGray,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading && apps.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        if (apps.isEmpty()) {
            EmptyLauncher(
                message = stringResource(R.string.no_installed_apps),
                modifier = Modifier.fillMaxSize(),
            )
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 170.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            gridItems(
                items = apps,
                key = { it.packageName },
            ) { app ->
                val selected = app.packageName in selectedPackages
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("installed-app:${app.packageName}")
                            .background(
                                color = Color(0xFF1A1A1A),
                                shape = RoundedCornerShape(12.dp),
                            )
                            .combinedClickable(
                                onClick = { onToggleApp(app.packageName) },
                                onLongClick = { menuPackage = app.packageName },
                            )
                            .padding(10.dp),
                    ) {
                        AppIcon(
                            shortcut = app,
                            modifier = Modifier.size(42.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = app.label,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { onToggleApp(app.packageName) },
                        )
                    }

                    DropdownMenu(
                        expanded = menuPackage == app.packageName,
                        onDismissRequest = { menuPackage = null },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.open_app)) },
                            onClick = {
                                menuPackage = null
                                onOpenApp(app)
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (selected) R.string.remove_shortcut
                                        else R.string.add_shortcut,
                                    ),
                                )
                            },
                            onClick = {
                                menuPackage = null
                                onToggleApp(app.packageName)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.app_info)) },
                            onClick = {
                                menuPackage = null
                                onAppInfo(app)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.uninstall_app)) },
                            onClick = {
                                menuPackage = null
                                onUninstall(app)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppIcon(
    shortcut: AppShortcut,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val icon by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = shortcut.packageName,
    ) {
        value = withContext(Dispatchers.IO) {
            loadIcon(context, shortcut.packageName)?.toImageBitmapSafely()
        }
    }

    if (icon != null) {
        Image(
            bitmap = icon!!,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    } else {
        GenericAppIcon(
            label = shortcut.label,
            modifier = modifier,
        )
    }
}

private fun loadIcon(
    context: Context,
    packageName: String,
): Drawable? {
    val launcherApps = context.getSystemService(LauncherApps::class.java)
    val launchableIcon = runCatching {
        launcherApps.getActivityList(packageName, Process.myUserHandle())
            .firstOrNull()
            ?.getBadgedIcon(context.resources.displayMetrics.densityDpi)
    }.getOrNull()

    return launchableIcon ?: runCatching {
        context.packageManager.getApplicationIcon(packageName)
    }.getOrNull()
}

private fun Drawable.toImageBitmapSafely(): ImageBitmap? =
    runCatching {
        val width = intrinsicWidth.takeIf { it > 0 }
            ?.coerceAtMost(MAX_ICON_PIXELS)
            ?: DEFAULT_ICON_PIXELS
        val height = intrinsicHeight.takeIf { it > 0 }
            ?.coerceAtMost(MAX_ICON_PIXELS)
            ?: DEFAULT_ICON_PIXELS
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        setBounds(0, 0, width, height)
        draw(Canvas(bitmap))
        bitmap.asImageBitmap()
    }.getOrNull()

@Composable
private fun GenericAppIcon(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(
            color = Color(0xFF303030),
            shape = RoundedCornerShape(18.dp),
        ),
    ) {
        Text(
            text = label.firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun LoadingLauncher(
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(Color.Black),
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyLauncher(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(Color.Black),
    ) {
        Text(
            text = message,
            color = Color.LightGray,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Preview(
    name = "GridLauncher landscape",
    widthDp = 960,
    heightDp = 540,
    showBackground = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun LauncherScreenPreview() {
    val previewShortcuts = listOf(
        "ATAK",
        "CÂMERA",
        "MAPAS",
        "RELÓGIO",
        "NAVEGADOR",
        "CONFIGURAÇÕES",
    ).mapIndexed { index, label ->
        ShortcutUiState(
            shortcut = AppShortcut(
                packageName = "example.$index",
                label = label,
            ),
            isAvailable = index != 0,
        )
    }

    GridLauncherTheme {
        LauncherScreen(
            uiState = LauncherUiState(
                shortcuts = previewShortcuts,
                isLoading = false,
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

private val LANDSCAPE_EDITOR_MIN_WIDTH = 720.dp
private const val MAX_VISIBLE_TILES = 6
private const val DEFAULT_ICON_PIXELS = 96
private const val MAX_ICON_PIXELS = 192
