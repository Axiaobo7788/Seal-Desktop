package com.junkfood.seal.desktop.settings.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.settings.DesktopAppSettings
import com.junkfood.seal.desktop.settings.DialogSingleChoiceItem
import com.junkfood.seal.desktop.settings.PreferenceInfo
import com.junkfood.seal.desktop.settings.PreferenceSubtitle
import com.junkfood.seal.desktop.settings.SettingsPageScaffold
import com.junkfood.seal.desktop.settings.UpdateChannelPreview
import com.junkfood.seal.desktop.settings.UpdateChannelStable
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.auto_update
import com.junkfood.seal.shared.generated.resources.check_for_updates
import com.junkfood.seal.shared.generated.resources.enable_auto_update
import com.junkfood.seal.shared.generated.resources.pre_release_channel
import com.junkfood.seal.shared.generated.resources.stable_channel
import com.junkfood.seal.shared.generated.resources.update_channel
import com.junkfood.seal.shared.generated.resources.update_channel_desc
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun UpdateSettingsPage(
    settings: DesktopAppSettings,
    onUpdate: ((DesktopAppSettings) -> DesktopAppSettings) -> Unit,
    onBack: () -> Unit,
) {
    SettingsPageScaffold(title = stringResource(Res.string.auto_update), onBack = onBack) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .clickable { onUpdate { it.copy(autoUpdateEnabled = !it.autoUpdateEnabled) } }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.enable_auto_update),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = settings.autoUpdateEnabled,
                        onCheckedChange = null,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        PreferenceSubtitle(text = stringResource(Res.string.update_channel), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp))

        DialogSingleChoiceItem(
            text = stringResource(Res.string.stable_channel),
            selected = settings.updateChannel == UpdateChannelStable,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            onUpdate { it.copy(updateChannel = UpdateChannelStable) }
        }

        DialogSingleChoiceItem(
            text = stringResource(Res.string.pre_release_channel),
            selected = settings.updateChannel == UpdateChannelPreview,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            onUpdate { it.copy(updateChannel = UpdateChannelPreview) }
        }
        
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            FilledTonalButton(
                onClick = { /* TODO desktop check for update */ },
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
            ) {
                Icon(Icons.Rounded.Update, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(Res.string.check_for_updates))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        PreferenceInfo(text = stringResource(Res.string.update_channel_desc))
    }
}
