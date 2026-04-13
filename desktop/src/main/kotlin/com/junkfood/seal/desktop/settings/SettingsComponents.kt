package com.junkfood.seal.desktop.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import com.junkfood.seal.desktop.ui.AnimatedAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.back
import com.junkfood.seal.shared.generated.resources.format_selection_desc
import com.junkfood.seal.shared.generated.resources.cancel
import com.junkfood.seal.shared.generated.resources.confirm
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import org.jetbrains.compose.resources.stringResource
import androidx.compose.material3.RadioButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.layout.width

internal data class SettingsEntry(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: (() -> Unit)? = null,
)

private const val DISABLED_CARD_ALPHA = 0.62f

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun SettingsPageScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            state = rememberTopAppBarState(),
            canScroll = { true },
        )
    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 4.dp,
                    bottom = innerPadding.calculateBottomPadding() + 12.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}

@Composable
internal fun SettingRow(entry: SettingsEntry) {
    val enabled = entry.onClick != null
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(enabled = enabled, onClick = { entry.onClick?.invoke() }),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = entry.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun ToggleCard(
    title: String,
    description: String?,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (enabled) 1f else DISABLED_CARD_ALPHA }
            .clip(MaterialTheme.shapes.large)
            .clickable(enabled = enabled, onClick = { onCheckedChange(!checked) }),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
            )
        }
    }
}

@Composable
internal fun PreferenceSwitchWithContainer(
    title: String,
    description: String?,
    icon: ImageVector?,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (enabled) 1f else DISABLED_CARD_ALPHA }
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(enabled = enabled, onClick = { onCheckedChange(!checked) }),
        color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                if (!description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
            )
        }
    }
}

@Composable
internal fun TextFieldCard(
    title: String,
    description: String? = null,
    icon: ImageVector,
    value: String,
    enabled: Boolean = true,
    placeholder: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 6,
    onValueChange: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    if (description != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                placeholder = placeholder?.let { { Text(it) } },
                singleLine = singleLine,
                maxLines = maxLines,
            )
        }
    }
}

@Composable
internal fun SelectionCard(
    title: String,
    description: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (enabled) 1f else DISABLED_CARD_ALPHA }
            .clip(MaterialTheme.shapes.large)
            .clickable(enabled = enabled, onClick = onClick),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            } else {
                Spacer(Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun <T> ChoiceDialog(
    visible: Boolean,
    title: String,
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    var currentSelection by remember(selected, visible) { mutableStateOf(selected) }

    AnimatedAlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onSelect(currentSelection)
                    onDismiss()
                }
            ) {
                Text(stringResource(com.junkfood.seal.shared.generated.resources.Res.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(com.junkfood.seal.shared.generated.resources.Res.string.cancel))
            }
        },
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { (label, value) ->
                    DialogSingleChoiceItem(
                        text = label,
                        selected = value == currentSelection,
                        onClick = { currentSelection = value }
                    )
                }
            }
        },
    )
}

@Composable
internal fun PreferenceSubtitle(
    text: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, top = 20.dp, bottom = 8.dp),
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text = text,
        modifier = modifier.padding(contentPadding),
        color = color,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
internal fun PreferenceInfo(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector = Icons.Outlined.Info,
    applyPaddings: Boolean = true,
) {
    Column(
        modifier =
            modifier.fillMaxWidth().run {
                if (applyPaddings) padding(horizontal = 16.dp, vertical = 16.dp) else this
            }
    ) {
        Icon(modifier = Modifier.padding(), imageVector = icon, contentDescription = null)
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
internal fun PlaceholderPage(title: String, body: String, onBack: () -> Unit) {
    SettingsPageScaffold(title = title, onBack = onBack) {
        Text(text = body, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.format_selection_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun ActionCard(
    title: String,
    description: String?,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(enabled = enabled, onClick = onClick),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun DialogSingleChoiceItemVariant(
    modifier: Modifier = Modifier,
    title: String,
    desc: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp),
                    selected = selected,
                    onClick = null,
                )
                Text(text = title, style = MaterialTheme.typography.titleMedium)
            }
            desc?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, start = 48.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Composable
internal fun DialogSingleChoiceItem(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Spacer(modifier = Modifier.width(8.dp))
        RadioButton(
            modifier = Modifier.padding(12.dp),
            selected = selected,
            onClick = null,
        )
        Text(text = text, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Composable
internal fun SwitchWithDividerCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String?,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (enabled) 1f else DISABLED_CARD_ALPHA }
            .clip(MaterialTheme.shapes.large)
            .clickable(enabled = enabled, onClick = onClick),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    if (!description.isNullOrBlank()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            VerticalDivider(modifier = Modifier.height(32.dp).padding(horizontal = 8.dp))
            Switch(
                checked = checked,
                onCheckedChange = if (enabled) onCheckedChange else null,
                enabled = enabled,
            )
        }
    }
}

@Composable
internal fun ActionWithDividerCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String?,
    icon: ImageVector,
    trailingIcon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
    onTrailingClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (enabled) 1f else DISABLED_CARD_ALPHA }
            .clip(MaterialTheme.shapes.large)
            .clickable(enabled = enabled, onClick = onClick),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(2.dp))
                    if (!description.isNullOrBlank()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            VerticalDivider(modifier = Modifier.height(32.dp).padding(horizontal = 8.dp))
            IconButton(onClick = onTrailingClick, enabled = enabled) {
                Icon(trailingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

