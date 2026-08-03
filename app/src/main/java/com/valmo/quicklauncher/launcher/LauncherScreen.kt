package com.valmo.quicklauncher.launcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.valmo.quicklauncher.model.AppShortcut
import com.valmo.quicklauncher.ui.theme.QuickLauncherTheme
import com.valmo.quicklauncher.ui.theme.ShortcutDivider
import com.valmo.quicklauncher.ui.theme.ShortcutPressed

@Composable
fun LauncherScreen(
    shortcuts: List<ShortcutUiState>,
    onShortcutClick: (AppShortcut) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (shortcuts.isEmpty()) {
        EmptyLauncher(modifier)
        return
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        shortcuts.forEachIndexed { index, state ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(ShortcutDivider),
                )
            }
            ShortcutTile(
                state = state,
                onClick = { onShortcutClick(state.shortcut) },
            )
        }
    }
}

@Composable
private fun RowScope.ShortcutTile(
    state: ShortcutUiState,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val background = if (isPressed) ShortcutPressed else Color.Black

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
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
            if (state.icon != null) {
                Image(
                    bitmap = state.icon,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(iconSize),
                )
            } else {
                GenericAppIcon(
                    label = state.shortcut.label,
                    modifier = Modifier.size(iconSize),
                )
            }
            if (!state.isAvailable) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Não instalado",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun GenericAppIcon(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
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
private fun EmptyLauncher(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Text(
            text = "Nenhum atalho configurado",
            color = Color.LightGray,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Preview(
    name = "Launcher landscape",
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
            shortcut = AppShortcut(label, "example.$index"),
            icon = null,
            isAvailable = index != 0,
        )
    }

    QuickLauncherTheme {
        LauncherScreen(
            shortcuts = previewShortcuts,
            onShortcutClick = {},
        )
    }
}
