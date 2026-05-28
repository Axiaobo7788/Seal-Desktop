package com.junkfood.seal.desktop.settings.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.i18n.DesktopLocaleOption
import com.junkfood.seal.desktop.i18n.findDesktopLocaleOption
import com.junkfood.seal.desktop.i18n.matchesLanguageAndScript
import com.junkfood.seal.desktop.i18n.supportedDesktopLocaleOptions
import com.junkfood.seal.desktop.settings.PreferenceSubtitle
import com.junkfood.seal.desktop.settings.SelectionCard
import com.junkfood.seal.desktop.settings.SettingsPageScaffold
import com.junkfood.seal.desktop.originalSystemLocale
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.all_languages
import com.junkfood.seal.shared.generated.resources.follow_system
import com.junkfood.seal.shared.generated.resources.language
import com.junkfood.seal.shared.generated.resources.suggested
import com.junkfood.seal.shared.generated.resources.translate
import com.junkfood.seal.shared.generated.resources.translate_desc
import org.jetbrains.compose.resources.stringResource

private const val weblateUrl = "https://hosted.weblate.org/engage/seal/"

@Composable
fun LanguageSettingsPage(
    selectedLanguageTag: String?,
    onLanguageSelected: (String?) -> Unit,
    onBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val supportedLocales = remember { supportedDesktopLocaleOptions() }
    val selectedOption = remember(selectedLanguageTag) { findDesktopLocaleOption(selectedLanguageTag) }

    val suggestedLocales = remember {
        supportedLocales.filter { it.displayLocale.matchesLanguageAndScript(originalSystemLocale) }.toSet()
    }

    val otherLocales = supportedLocales.filter { it !in suggestedLocales }

    SettingsPageScaffold(title = stringResource(Res.string.language), onBack = onBack) {
        SelectionCard(
            title = stringResource(Res.string.translate),
            description = stringResource(Res.string.translate_desc),
            icon = Icons.Outlined.Translate,
            onClick = { uriHandler.openUri(weblateUrl) },
        )

        PreferenceSubtitle(text = stringResource(Res.string.suggested))
        LanguageChoiceItem(
            text = stringResource(Res.string.follow_system),
            selected = selectedOption == null,
            onClick = { onLanguageSelected(null) },
        )

        suggestedLocales.forEach { option ->
            LanguageChoiceItem(
                text = option.displayNameForSelf(),
                selected = selectedOption == option,
                onClick = { onLanguageSelected(option.persistedTag) },
            )
        }

        PreferenceSubtitle(text = stringResource(Res.string.all_languages))

        otherLocales.forEach { option ->
            LanguageChoiceItem(
                text = option.displayNameForSelf(),
                selected = selectedOption == option,
                onClick = { onLanguageSelected(option.persistedTag) },
            )
        }

    }
}

@Composable
private fun LanguageChoiceItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RadioButton(selected = selected, onClick = null)
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun DesktopLocaleOption.displayNameForSelf(): String =
    displayLocale.getDisplayName(displayLocale)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(displayLocale) else it.toString() }
