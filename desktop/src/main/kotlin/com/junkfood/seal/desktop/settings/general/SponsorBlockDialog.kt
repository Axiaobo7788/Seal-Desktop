package com.junkfood.seal.desktop.settings.general

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoneyOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.sponsorblock
import com.junkfood.seal.shared.generated.resources.sponsorblock_categories
import com.junkfood.seal.shared.generated.resources.sponsorblock_categories_desc
import org.jetbrains.compose.resources.stringResource
import com.junkfood.seal.desktop.ui.AnimatedAlertDialog
import com.junkfood.seal.shared.generated.resources.cancel
import com.junkfood.seal.shared.generated.resources.save

val sponsorBlockCategories = listOf(
    "sponsor", "intro", "outro", "selfpromo", 
    "preview", "filler", "interaction", "music_offtopic"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SponsorBlockDialog(
    visible: Boolean,
    initialCategories: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var categories by remember(visible) { mutableStateOf(initialCategories) }

    AnimatedAlertDialog(
        visible = visible,
        icon = { Icon(Icons.Outlined.MoneyOff, contentDescription = null) },
        title = { Text(stringResource(Res.string.sponsorblock)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(Res.string.sponsorblock_categories_desc),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    value = categories,
                    label = { Text(stringResource(Res.string.sponsorblock_categories)) },
                    onValueChange = { categories = it },
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    item { FilterChip(selected = false, onClick = { categories = "default" }, label = { Text("default") }) }
                    item { FilterChip(selected = false, onClick = { categories = "all" }, label = { Text("all") }) }
                    items(sponsorBlockCategories.size) { index ->
                        val it = sponsorBlockCategories[index]
                        if (!categories.contains(it)) {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val safeCat = categories.replace(Regex("(all)|(default)"), "")
                                    val newCat = "$safeCat,$it".removePrefix(",")
                                    categories = newCat
                                },
                                label = { Text(it) }
                            )
                        }
                    }
                }
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = { onConfirm(categories) }) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
