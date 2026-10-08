package com.junkfood.seal.desktop.settings.about

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.settings.PreferenceInfo
import com.junkfood.seal.desktop.settings.SettingsPageScaffold
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.desktop_app_update
import com.junkfood.seal.shared.generated.resources.desktop_manual_update_desc
import com.junkfood.seal.shared.generated.resources.desktop_open_releases
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun UpdateSettingsPage(
    onBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    SettingsPageScaffold(title = stringResource(Res.string.desktop_app_update), onBack = onBack) {
        PreferenceInfo(text = stringResource(Res.string.desktop_manual_update_desc))

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            FilledTonalButton(
                onClick = { uriHandler.openUri(desktopReleaseUrl) },
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
            ) {
                Icon(Icons.Rounded.NewReleases, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(Res.string.desktop_open_releases))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}
