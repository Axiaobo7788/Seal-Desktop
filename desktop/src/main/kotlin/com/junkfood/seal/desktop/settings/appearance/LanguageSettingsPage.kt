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
import com.junkfood.seal.desktop.settings.PreferenceSubtitle
import com.junkfood.seal.desktop.settings.SelectionCard
import com.junkfood.seal.desktop.settings.SettingsPageScaffold
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.all_languages
import com.junkfood.seal.shared.generated.resources.follow_system
import com.junkfood.seal.shared.generated.resources.language
import com.junkfood.seal.shared.generated.resources.suggested
import com.junkfood.seal.shared.generated.resources.translate
import com.junkfood.seal.shared.generated.resources.translate_desc
import java.util.Locale
import org.jetbrains.compose.resources.stringResource

private const val weblateUrl = "https://hosted.weblate.org/engage/seal/"

@Composable
fun LanguageSettingsPage(
    selectedLanguageTag: String?,
    onLanguageSelected: (String?) -> Unit,
    onBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val supportedLocales = remember { supportedLocales() }
    val selectedLocale = selectedLanguageTag?.takeIf { it.isNotBlank() }?.let(Locale::forLanguageTag)

    val suggestedLocales = remember {
        val preferred = Locale.getDefault()
        supportedLocales.filter { it.matchesLanguageAndScript(preferred) }.toSet()
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
            selected = selectedLocale == null,
            onClick = { onLanguageSelected(null) },
        )

        suggestedLocales.forEach { locale ->
            LanguageChoiceItem(
                text = locale.displayNameForSelf(),
                selected = selectedLocale == locale,
                onClick = { onLanguageSelected(locale.toLanguageTag()) },
            )
        }

        PreferenceSubtitle(text = stringResource(Res.string.all_languages))

        otherLocales.forEach { locale ->
            LanguageChoiceItem(
                text = locale.displayNameForSelf(),
                selected = selectedLocale == locale,
                onClick = { onLanguageSelected(locale.toLanguageTag()) },
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

private fun Locale.displayNameForSelf(): String =
    getDisplayName(this).replaceFirstChar { if (it.isLowerCase()) it.titlecase(this) else it.toString() }

private fun Locale.matchesLanguageAndScript(other: Locale): Boolean {
    if (!language.equals(other.language, ignoreCase = true)) return false
    val script = script.orEmpty()
    val otherScript = other.script.orEmpty()
    val scriptMatch = script.isBlank() || otherScript.isBlank() || script.equals(otherScript, ignoreCase = true)
    val countryMatch = country.isBlank() || other.country.isBlank() || country.equals(other.country, ignoreCase = true)
    return scriptMatch && countryMatch
}

private fun supportedLocales(): List<Locale> =
    listOf(
        Locale.forLanguageTag("ar"),
        Locale.forLanguageTag("az"),
        Locale.forLanguageTag("eu"),
        Locale.forLanguageTag("be"),
        Locale.forLanguageTag("bn"),
        Locale.forLanguageTag("ca"),
        Locale.forLanguageTag("zh-CN"),
        Locale.forLanguageTag("zh-TW"),
        Locale.forLanguageTag("hr"),
        Locale.forLanguageTag("cs"),
        Locale.forLanguageTag("da"),
        Locale.forLanguageTag("nl"),
        Locale.forLanguageTag("en-US"),
        Locale.forLanguageTag("fil"),
        Locale.forLanguageTag("fr"),
        Locale.forLanguageTag("de"),
        Locale.forLanguageTag("el"),
        Locale.forLanguageTag("iw"),
        Locale.forLanguageTag("hi"),
        Locale.forLanguageTag("hu"),
        Locale.forLanguageTag("in"),
        Locale.forLanguageTag("it"),
        Locale.forLanguageTag("ja"),
        Locale.forLanguageTag("kn"),
        Locale.forLanguageTag("km"),
        Locale.forLanguageTag("ko"),
        Locale.forLanguageTag("ms"),
        Locale.forLanguageTag("ml"),
        Locale.forLanguageTag("mn"),
        Locale.forLanguageTag("nb"),
        Locale.forLanguageTag("nn"),
        Locale.forLanguageTag("fa"),
        Locale.forLanguageTag("pl"),
        Locale.forLanguageTag("pt"),
        Locale.forLanguageTag("pt-PT"),
        Locale.forLanguageTag("pt-BR"),
        Locale.forLanguageTag("pa"),
        Locale.forLanguageTag("ru"),
        Locale.forLanguageTag("sr"),
        Locale.forLanguageTag("si"),
        Locale.forLanguageTag("es"),
        Locale.forLanguageTag("sv"),
        Locale.forLanguageTag("ta"),
        Locale.forLanguageTag("th"),
        Locale.forLanguageTag("tr"),
        Locale.forLanguageTag("uk"),
        Locale.forLanguageTag("vi"),
    )
