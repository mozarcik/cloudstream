package com.lagradost.cloudstream3.phone.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.phone.presentation.common.CsButton
import com.lagradost.cloudstream3.phone.presentation.common.CsKicker
import com.lagradost.cloudstream3.phone.presentation.screens.PhoneRoutes
import com.lagradost.cloudstream3.phone.presentation.theme.LocalPhoneAccent
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneColors
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneText
import com.lagradost.cloudstream3.syncproviders.AccountManager
import com.lagradost.cloudstream3.tv.presentation.screens.library.LibraryScreenUiState
import com.lagradost.cloudstream3.tv.presentation.screens.library.LibrarySectionUiState
import com.lagradost.cloudstream3.tv.presentation.screens.profile.ProfileViewModel

/** Shared by the modal drawer (phone) and the permanent one (tablet): profile, navigation, provider lists, Settings. */
@Composable
internal fun PhoneDrawerContent(
    route: String?,
    library: LibraryScreenUiState,
    profileViewModel: ProfileViewModel,
    onNavigate: (String) -> Unit,
    onSwitchProvider: (String) -> Unit,
    onOpenList: (LibrarySectionUiState) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showProfiles by remember { mutableStateOf(false) }
    val profiles by profileViewModel.uiState.collectAsStateWithLifecycle()
    val current = profiles.accounts.firstOrNull { it.isSelected } ?: profiles.accounts.firstOrNull()
    // Only providers the user is signed in to, with Local (always available) below them.
    val localName = AccountManager.localListApi.name
    val providers = remember(library.availableSources) {
        AccountManager.syncApis.filter { it.isAvailable }.map { it.name }.sortedBy { it == localName }
    }

    var collapsed by remember { mutableStateOf(setOf<String>()) }

    Column(modifier.statusBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button) { profileViewModel.refresh(); showProfiles = true }.padding(start = 20.dp, end = 16.dp, top = 22.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Avatar(current?.name.orEmpty(), 48.dp)
                Column(Modifier.weight(1f)) {
                    Text(current?.name.orEmpty(), style = PhoneText.Row.copy(fontSize = 17.sp), color = PhoneColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.phone_switch_profile), style = PhoneText.Meta, color = PhoneColors.Text2)
                }
                Icon(Icons.Default.ExpandMore, null, tint = PhoneColors.Label)
            }
            DrawerItem(Icons.Default.Home, stringResource(R.string.title_home), route == PhoneRoutes.Home) { onNavigate(PhoneRoutes.Home) }
            DrawerItem(Icons.Default.Search, stringResource(R.string.search), route == PhoneRoutes.Search) { onNavigate(PhoneRoutes.Search) }
            DrawerItem(Icons.Default.Download, stringResource(R.string.title_downloads), route == PhoneRoutes.Downloads) { onNavigate(PhoneRoutes.Downloads) }
            Divider()
            providers.forEach { provider ->
                val isCurrent = provider == library.currentApiName
                val open = isCurrent && provider !in collapsed
                ProviderHeader(
                    name = provider,
                    expanded = open,
                    loading = isCurrent && library.isLoading,
                    onClick = {
                        when {
                            !isCurrent -> { collapsed = collapsed - provider; onSwitchProvider(provider) }
                            else -> collapsed = if (provider in collapsed) collapsed - provider else collapsed + provider
                        }
                    },
                )
                if (open) {
                    when {
                        library.isLoading -> Text(stringResource(R.string.phone_loading_lists), style = PhoneText.Meta, color = PhoneColors.Text2, modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp))
                        library.sections.isEmpty() -> Text(stringResource(R.string.phone_no_lists), style = PhoneText.Meta, color = PhoneColors.Text2, modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp))
                        else -> library.sections.forEach { section ->
                            DrawerItem(Icons.Default.Bookmark, section.title, false, count = section.gridItems.size, height = 44.dp) { onOpenList(section) }
                        }
                    }
                }
            }
        }
        // Pinned, so it does not move when the provider lists above grow or shrink.
        Divider()
        DrawerItem(Icons.Default.Settings, stringResource(R.string.title_settings), route == PhoneRoutes.Settings) { onNavigate(PhoneRoutes.Settings) }
        Box(Modifier.height(8.dp))
    }

    if (showProfiles) ProfileSheet(profileViewModel, onDismiss = { showProfiles = false })
}

/** Provider title row: the chevron says it folds, the spinner says its lists are loading. */
@Composable
private fun ProviderHeader(name: String, expanded: Boolean, loading: Boolean, onClick: () -> Unit) {
    val label = stringResource(if (expanded) R.string.phone_collapse_provider else R.string.phone_expand_provider, name)
    Row(
        Modifier.fillMaxWidth().height(48.dp).clickable(role = Role.Button, onClickLabel = label, onClick = onClick).padding(start = 28.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CsKicker(name, Modifier.weight(1f), color = if (expanded) PhoneColors.Text2 else PhoneColors.Link)
        if (loading) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = LocalPhoneAccent.current)
        }
        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = PhoneColors.Label, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun Divider() {
    Box(Modifier.padding(horizontal = 20.dp, vertical = 10.dp).fillMaxWidth().height(1.dp).background(PhoneColors.Divider))
}

@Composable
private fun Avatar(name: String, size: Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(LocalPhoneAccent.current), contentAlignment = Alignment.Center) {
        Text(name.take(1).uppercase(), style = PhoneText.Level.copy(fontSize = 20.sp), color = PhoneColors.OnAccent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSheet(viewModel: ProfileViewModel, onDismiss: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = PhoneColors.Sheet) {
        Text(stringResource(R.string.phone_profiles), style = PhoneText.Section, color = PhoneColors.Text, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp))
        state.accounts.forEach { account ->
            Row(
                Modifier.fillMaxWidth().height(56.dp).background(if (account.isSelected) Color(0x14FFFFFF) else Color.Transparent)
                    .clickable(role = Role.Button) {
                        viewModel.selectAccount(account.keyIndex)
                        // A PIN-protected profile asks for its PIN first; the sheet stays until it is unlocked.
                        if (!account.hasPin) onDismiss()
                    }.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Avatar(account.name, 36.dp)
                Text(account.name, style = PhoneText.Row, color = PhoneColors.Text, modifier = Modifier.weight(1f))
                if (account.isSelected) Icon(Icons.Default.Check, null, tint = LocalPhoneAccent.current)
            }
        }
        Box(Modifier.height(24.dp))
    }
    state.pinPrompt?.let { prompt ->
        AlertDialog(
            onDismissRequest = viewModel::dismissPinPrompt,
            containerColor = PhoneColors.Sheet,
            title = { Text(stringResource(R.string.phone_enter_pin, prompt.accountName), style = PhoneText.Section, color = PhoneColors.Text) },
            text = {
                Column {
                    BasicTextField(
                        value = prompt.input, onValueChange = viewModel::updatePinPromptInput, singleLine = true,
                        textStyle = PhoneText.Level.copy(color = PhoneColors.Text, letterSpacing = 8.sp),
                        cursorBrush = SolidColor(PhoneColors.Text),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth().background(PhoneColors.Card, androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).padding(14.dp),
                    )
                    prompt.errorMessage?.let { Text(it, style = PhoneText.Meta, color = PhoneColors.Danger, modifier = Modifier.padding(top = 8.dp)) }
                }
            },
            confirmButton = { TextButton({ viewModel.submitPinPrompt(); if (viewModel.uiState.value.pinPrompt == null) onDismiss() }) { Text(stringResource(R.string.phone_confirm), style = PhoneText.Button, color = PhoneColors.Text) } },
            dismissButton = { TextButton(viewModel::dismissPinPrompt) { Text(stringResource(R.string.cancel), style = PhoneText.Button, color = PhoneColors.Text2) } },
        )
    }
}

@Composable
internal fun DrawerItem(icon: ImageVector, label: String, selected: Boolean, count: Int? = null, height: Dp = 56.dp, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .height(height)
            .background(if (selected) Color(0x24FFFFFF) else Color.Transparent, androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (count == null) PhoneColors.Text else PhoneColors.Text2, modifier = Modifier.size(if (count == null) 22.dp else 20.dp))
        Text(label, style = PhoneText.Row, color = PhoneColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 14.dp))
        if (count != null) Text(count.toString(), style = PhoneText.Meta, color = PhoneColors.Text2)
    }
}
