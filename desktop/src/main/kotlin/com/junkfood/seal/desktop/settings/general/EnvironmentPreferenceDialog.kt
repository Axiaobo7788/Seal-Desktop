package com.junkfood.seal.desktop.settings.general

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.settings.EnvPrefAuto
import com.junkfood.seal.desktop.settings.EnvPrefBundled
import com.junkfood.seal.desktop.settings.EnvPrefSystem
import com.junkfood.seal.desktop.ui.AnimatedAlertDialog
import com.junkfood.seal.desktop.ytdlp.DesktopDependencyResolution
import com.junkfood.seal.desktop.ytdlp.DesktopDependencyResolver
import com.junkfood.seal.desktop.ytdlp.DesktopDependencySource
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.cancel
import com.junkfood.seal.shared.generated.resources.confirm
import com.junkfood.seal.shared.generated.resources.desktop_dependency_check_again
import com.junkfood.seal.shared.generated.resources.desktop_dependency_check_status
import com.junkfood.seal.shared.generated.resources.desktop_dependency_diagnostic_label
import com.junkfood.seal.shared.generated.resources.desktop_dependency_hide_details
import com.junkfood.seal.shared.generated.resources.desktop_dependency_optional
import com.junkfood.seal.shared.generated.resources.desktop_dependency_path_label
import com.junkfood.seal.shared.generated.resources.desktop_dependency_show_details
import com.junkfood.seal.shared.generated.resources.desktop_dependency_source_packaged
import com.junkfood.seal.shared.generated.resources.desktop_dependency_source_selfhost
import com.junkfood.seal.shared.generated.resources.desktop_dependency_source_system
import com.junkfood.seal.shared.generated.resources.desktop_dependency_status_broken
import com.junkfood.seal.shared.generated.resources.desktop_dependency_status_detecting
import com.junkfood.seal.shared.generated.resources.desktop_dependency_status_healthy
import com.junkfood.seal.shared.generated.resources.desktop_dependency_status_missing
import com.junkfood.seal.shared.generated.resources.desktop_dependency_summary_optional_issue
import com.junkfood.seal.shared.generated.resources.desktop_dependency_summary_ready
import com.junkfood.seal.shared.generated.resources.desktop_dependency_summary_required_broken
import com.junkfood.seal.shared.generated.resources.desktop_dependency_summary_required_missing
import com.junkfood.seal.shared.generated.resources.env_pref_auto
import com.junkfood.seal.shared.generated.resources.env_pref_bundled
import com.junkfood.seal.shared.generated.resources.env_pref_system
import com.junkfood.seal.shared.generated.resources.env_preference
import com.junkfood.seal.ui.PlatformVerticalScrollbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun environmentSummaryText(resolution: DesktopDependencyResolution?): String {
    if (resolution == null) return stringResource(Res.string.desktop_dependency_status_detecting)
    val model = resolution.toEnvironmentSummaryUiModel()
    return when (model.health) {
        EnvironmentSummaryHealth.Healthy -> stringResource(Res.string.desktop_dependency_summary_ready)
        EnvironmentSummaryHealth.DegradedOptional ->
            stringResource(Res.string.desktop_dependency_summary_optional_issue, model.optionalIssues.joinToString())
        EnvironmentSummaryHealth.MissingRequired ->
            stringResource(Res.string.desktop_dependency_summary_required_missing, model.requiredIssues.joinToString())
        EnvironmentSummaryHealth.BrokenRequired ->
            stringResource(Res.string.desktop_dependency_summary_required_broken, model.requiredIssues.joinToString())
    }
}

@Composable
internal fun EnvironmentPreferenceDialog(
    visible: Boolean,
    selected: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    onHealthInvalidated: () -> Unit,
) {
    var currentSelection by remember(selected, visible) { mutableIntStateOf(selected) }
    var refreshKey by remember(visible) { mutableIntStateOf(0) }
    var showDetails by remember(visible) { mutableStateOf(false) }
    val resolution by
        produceState<DesktopDependencyResolution?>(null, currentSelection, refreshKey, visible) {
            if (visible) {
                value = withContext(Dispatchers.IO) { DesktopDependencyResolver.resolve(currentSelection) }
            }
        }
    val scrollState = rememberScrollState()

    AnimatedAlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.env_preference)) },
        text = {
            Box {
                Column(
                    modifier = Modifier.verticalScroll(scrollState).padding(end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    environmentPreferenceOptions().forEach { (label, value) ->
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .selectable(
                                        selected = currentSelection == value,
                                        role = Role.RadioButton,
                                        onClick = { currentSelection = value },
                                    )
                                    .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = currentSelection == value, onClick = null)
                            Text(label, modifier = Modifier.padding(start = 10.dp))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(Res.string.desktop_dependency_check_status),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TextButton(
                            onClick = {
                                DesktopDependencyResolver.invalidateHealth()
                                refreshKey += 1
                                onHealthInvalidated()
                            },
                        ) {
                            Text(stringResource(Res.string.desktop_dependency_check_again))
                        }
                    }

                    val currentResolution = resolution
                    if (currentResolution == null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val model = currentResolution.toEnvironmentSummaryUiModel()
                        model.dependencies.forEach { dependency ->
                            DependencyStatusCard(dependency, showDetails)
                        }
                        TextButton(
                            onClick = { showDetails = !showDetails },
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text(
                                stringResource(
                                    if (showDetails) Res.string.desktop_dependency_hide_details
                                    else Res.string.desktop_dependency_show_details,
                                ),
                            )
                        }
                    }
                }
                PlatformVerticalScrollbar(
                    state = scrollState,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(vertical = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSelect(currentSelection)
                    onDismiss()
                },
            ) {
                Text(stringResource(Res.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

@Composable
private fun environmentPreferenceOptions(): List<Pair<String, Int>> =
    listOf(
        stringResource(Res.string.env_pref_auto) to EnvPrefAuto,
        stringResource(Res.string.env_pref_bundled) to EnvPrefBundled,
        stringResource(Res.string.env_pref_system) to EnvPrefSystem,
    )

@Composable
private fun DependencyStatusCard(
    dependency: DependencyUiStatus,
    showDetails: Boolean,
) {
    val statusText =
        when (dependency.health) {
            DependencyUiHealth.Healthy -> stringResource(Res.string.desktop_dependency_status_healthy)
            DependencyUiHealth.Missing -> stringResource(Res.string.desktop_dependency_status_missing)
            DependencyUiHealth.Broken -> stringResource(Res.string.desktop_dependency_status_broken)
        }
    val sourceText =
        when (dependency.source) {
            DesktopDependencySource.AppPrivate -> stringResource(Res.string.desktop_dependency_source_selfhost)
            DesktopDependencySource.Packaged -> stringResource(Res.string.desktop_dependency_source_packaged)
            DesktopDependencySource.SystemPath -> stringResource(Res.string.desktop_dependency_source_system)
            null -> null
        }
    val icon =
        when (dependency.health) {
            DependencyUiHealth.Healthy -> Icons.Outlined.CheckCircle
            DependencyUiHealth.Missing -> Icons.Outlined.RemoveCircleOutline
            DependencyUiHealth.Broken -> Icons.Outlined.ErrorOutline
        }
    val statusColor =
        when (dependency.health) {
            DependencyUiHealth.Healthy -> MaterialTheme.colorScheme.primary
            DependencyUiHealth.Missing -> MaterialTheme.colorScheme.onSurfaceVariant
            DependencyUiHealth.Broken -> MaterialTheme.colorScheme.error
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = statusColor)
                Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(dependency.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (!dependency.required) {
                            Text(
                                " · ${stringResource(Res.string.desktop_dependency_optional)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    listOfNotNull(sourceText, dependency.shortVersion).takeIf { it.isNotEmpty() }?.let { parts ->
                        Text(
                            parts.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                }
                Text(statusText, color = statusColor, style = MaterialTheme.typography.labelLarge)
            }

            AnimatedVisibility(showDetails && (dependency.path != null || dependency.diagnostic != null)) {
                Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    dependency.path?.let { path ->
                        DetailText(stringResource(Res.string.desktop_dependency_path_label), path)
                    }
                    dependency.diagnostic?.let { diagnostic ->
                        DetailText(stringResource(Res.string.desktop_dependency_diagnostic_label), diagnostic)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailText(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(2.dp))
        SelectionContainer {
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
