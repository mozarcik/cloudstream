package com.lagradost.cloudstream3.phone.presentation.screens.settings

import com.lagradost.cloudstream3.phone.presentation.common.navBarBottom
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.designsystem.CsColors
import com.lagradost.cloudstream3.phone.presentation.common.CsButton
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneColors
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneDimens
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneShapes
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneText
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsEntry
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsEntryType
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsLoadException
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsScreen
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.rememberMasterDetailSettingsRegistry
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamTheme

private const val RootId = "settings_root"
private const val ExtensionsPrefix = "settings_extensions"

private sealed interface Loaded {
    data object Loading : Loaded
    data class Ready(val entries: List<SettingsEntry>) : Loaded
    data class Failed(val title: String?, val message: String) : Loaded
}

/** Lets other screens (drawer "Sign in") open Settings straight on a nested level. */
object PhoneSettingsDeepLink {
    private var pending: List<String>? = null
    fun openAccounts() { pending = listOf(RootId, "settings_account") }
    internal fun consume(): List<String>? = pending.also { pending = null }
}

/**
 * Settings as one column with push/pop levels. The levels, rows and actions come from the same
 * registry as TV (`SettingsRegistry`), so both UIs always offer the same settings.
 */
@Composable
fun PhoneSettingsScreen(onOpenMenu: () -> Unit, modifier: Modifier = Modifier) {
    val registry = rememberMasterDetailSettingsRegistry()
    val initialStack = remember { PhoneSettingsDeepLink.consume() ?: listOf(RootId) }
    var stack by rememberSaveable { mutableStateOf(initialStack) }
    val currentId = stack.last()
    BackHandler(enabled = stack.size > 1) { stack = stack.dropLast(1) }

    // Extensions have their own phone UI (repositories / plugins); every other level is data driven.
    if (currentId.startsWith(ExtensionsPrefix)) {
        PhoneExtensionsScreen(onBack = { stack = stack.dropLast(1) }, modifier = modifier)
        return
    }

    val screen = remember(registry, currentId) { runCatching { registry.get(currentId) }.getOrNull() }
    if (screen == null) {
        stack = listOf(RootId)
        return
    }
    var reloadKey by remember { mutableIntStateOf(0) }
    val loaded by produceState<Loaded>(Loaded.Loading, screen, reloadKey) {
        // Keep the old rows visible while reloading after a change (no flicker back to Loading).
        if (reloadKey == 0) value = Loaded.Loading
        value = try {
            Loaded.Ready(screen.load())
        } catch (e: SettingsLoadException) {
            Loaded.Failed(e.errorTitle, e.message.orEmpty())
        } catch (e: Exception) {
            Loaded.Failed(null, e.localizedMessage.orEmpty())
        }
    }

    Column(modifier.fillMaxSize().background(PhoneColors.Ground).statusBarsPadding()) {
        if (stack.size == 1) {
            LevelHeader(screen.title, onOpenMenu.takeUnless { com.lagradost.cloudstream3.phone.presentation.common.LocalPermanentMenu.current }, icon = Icons.Default.Menu, backLabel = stringResource(R.string.phone_open_menu))
        } else {
            LevelHeader(screen.title, { stack = stack.dropLast(1) })
        }
        when {
            screen.hasCustomContent -> CustomLevel(screen, onBack = { stack = stack.dropLast(1) }, onChanged = { reloadKey++ })
            loaded is Loaded.Loading -> Text(stringResource(R.string.phone_loading), style = PhoneText.Body, color = PhoneColors.Text2, modifier = Modifier.padding(PhoneDimens.ScreenPadding))
            loaded is Loaded.Failed -> Column(Modifier.padding(PhoneDimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val failed = loaded as Loaded.Failed
                failed.title?.let { Text(it, style = PhoneText.Section, color = PhoneColors.Text) }
                Text(failed.message, style = PhoneText.Body, color = PhoneColors.Text2)
                CsButton(stringResource(R.string.phone_retry), { reloadKey++ })
            }
            else -> EntryList(
                description = screen.description,
                entries = (loaded as Loaded.Ready).entries,
                onPush = { stack = stack + it },
                onChanged = { reloadKey++ },
            )
        }
    }
}

/** Levels that draw their own content (accounts, a few general screens) reuse the TV composable inside the TV theme. */
@Composable
private fun CustomLevel(screen: SettingsScreen, onBack: () -> Unit, onChanged: (String) -> Unit) {
    CloudStreamTheme {
        Box(Modifier.fillMaxSize()) {
            screen.Content(Modifier.fillMaxSize(), PaddingValues(start = PhoneDimens.ScreenPadding, top = PhoneDimens.ScreenPadding, end = PhoneDimens.ScreenPadding, bottom = PhoneDimens.ScreenPadding + navBarBottom()), false, onBack, onChanged)
        }
    }
}

@Composable
private fun EntryList(description: String?, entries: List<SettingsEntry>, onPush: (String) -> Unit, onChanged: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp + navBarBottom())) {
        if (!description.isNullOrBlank()) {
            item(key = "description") { Text(description, style = PhoneText.Body, color = PhoneColors.Text2, modifier = Modifier.padding(bottom = 12.dp)) }
        }
        itemsIndexed(entries, key = { index, entry -> "${entry.stableKey}#$index" }) { _, entry ->
            EntryRow(entry, onPush, onChanged)
        }
    }
}

@Composable
private fun EntryRow(entry: SettingsEntry, onPush: (String) -> Unit, onChanged: () -> Unit) {
    when (entry.type) {
        SettingsEntryType.Header -> Text(
            entry.title.uppercase(), style = PhoneText.Kicker, color = PhoneColors.Label,
            modifier = Modifier.padding(top = 18.dp, bottom = 6.dp, start = 4.dp),
        )
        SettingsEntryType.Message -> Column(Modifier.padding(vertical = 8.dp, horizontal = 4.dp)) {
            Text(entry.title, style = PhoneText.Row, color = PhoneColors.Text)
            entry.subtitle?.let { Text(it, style = PhoneText.Body, color = PhoneColors.Text2) }
        }
        SettingsEntryType.Slider -> Column(
            Modifier.fillMaxWidth().padding(top = 8.dp).clip(PhoneShapes.Card).background(PhoneColors.Sheet).padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            var value by remember(entry.stableKey, entry.sliderValue) { mutableStateOf(entry.sliderValue.toFloat()) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.title, style = PhoneText.Row, color = PhoneColors.Text, modifier = Modifier.weight(1f))
                Text(value.toInt().toString(), style = PhoneText.Button, color = CsColors.Link)
            }
            val range = entry.sliderRange
            val steps = ((range.last - range.first) / entry.sliderStep.coerceAtLeast(1) - 1).coerceAtLeast(0)
            Slider(
                value = value, onValueChange = { value = it },
                onValueChangeFinished = { entry.onSliderChanged?.invoke(value.toInt()); onChanged() },
                valueRange = range.first.toFloat()..range.last.toFloat().coerceAtLeast(range.first + 1f), steps = steps,
            )
        }
        else -> Row(
            Modifier.fillMaxWidth().padding(top = 8.dp).clip(PhoneShapes.Card).background(PhoneColors.Sheet)
                .heightIn(min = if (entry.compactRow) 46.dp else 56.dp)
                .clickable(role = Role.Button) {
                    when (entry.type) {
                        SettingsEntryType.Toggle, SettingsEntryType.MultiSelect -> { entry.onToggleChanged?.invoke(!entry.toggleValue); onChanged() }
                        else -> {
                            entry.action?.invoke()
                            entry.nextScreenId?.let(onPush)
                            if (entry.action != null) onChanged()
                        }
                    }
                }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (entry.type == SettingsEntryType.Radio) RadioButton(selected = entry.showCheckmark, onClick = null)
            if (entry.type == SettingsEntryType.MultiSelect) Checkbox(checked = entry.toggleValue, onCheckedChange = null)
            when {
                entry.icon != null -> Icon(entry.icon, null, tint = PhoneColors.Text, modifier = Modifier.size(22.dp))
                !entry.iconUrl.isNullOrBlank() -> AsyncImage(entry.iconUrl, null, Modifier.size(28.dp).clip(PhoneShapes.Poster))
                entry.fallbackIconRes != null -> Icon(painterResource(entry.fallbackIconRes), null, tint = PhoneColors.Text, modifier = Modifier.size(22.dp))
                entry.initials != null -> Box(Modifier.size(32.dp).clip(PhoneShapes.Poster).background(entry.tileColorArgb?.let(::Color) ?: PhoneColors.Card), contentAlignment = Alignment.Center) {
                    Text(entry.initials, style = PhoneText.Meta.copy(fontSize = 11.sp), color = PhoneColors.Text)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(entry.title, style = PhoneText.Row, color = PhoneColors.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                entry.subtitle?.takeIf { it.isNotBlank() }?.let { Text(it, style = PhoneText.Meta, color = PhoneColors.Text2, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
            entry.valueText?.let { Text(it, style = PhoneText.Meta, color = PhoneColors.Text2) }
            when (entry.type) {
                SettingsEntryType.Toggle -> Switch(checked = entry.toggleValue, onCheckedChange = null)
                SettingsEntryType.Item -> if (entry.nextScreenId != null) Icon(Icons.Default.ChevronRight, null, tint = PhoneColors.Label)
                else -> Unit
            }
        }
    }
}
