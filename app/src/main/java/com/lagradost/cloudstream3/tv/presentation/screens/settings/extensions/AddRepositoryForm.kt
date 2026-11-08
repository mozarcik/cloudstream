package com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions

import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsActionRow
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsInfoParagraph
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsFieldRow
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsLevelHeader
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import androidx.compose.foundation.layout.Arrangement

/**
 * Add repository (mockup SettingsAddRepo): the two fields are rows (OK opens the keyboard), the
 * action row stays disabled until a URL is entered.
 */
@Composable
fun AddRepositoryForm(
    viewModel: ExtensionsViewModel,
    onBack: () -> Unit,
    onRepositoryAdded: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val urlRequester = remember { FocusRequester() }
    val nameRequester = remember { FocusRequester() }
    FocusRequestEffect(requester = urlRequester, requestKey = Unit)

    val canAdd = url.isNotBlank() && !isLoading

    fun submit() {
        if (!canAdd) return
        isLoading = true
        error = null
        // Pass null as name if empty, so it will be auto-detected.
        viewModel.addRepository(name.ifBlank { null }, url.trim()) { result ->
            isLoading = false
            result.fold(
                onSuccess = {
                    onRepositoryAdded()
                    onBack()
                },
                onFailure = { e ->
                    error = when {
                        e.message?.contains("Invalid repository URL") == true ->
                            context.getString(R.string.error_invalid_url)
                        e.message?.contains("No repository found") == true ->
                            context.getString(R.string.no_repository_found_error)
                        e.message?.contains("already exists") == true ->
                            context.getString(R.string.tv_settings_repository_exists)
                        else -> e.message ?: context.getString(R.string.error)
                    }
                }
            )
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SettingsLevelHeader(title = stringResource(R.string.add_repository))
        Column(
            verticalArrangement = Arrangement.spacedBy(TvDimens.SettingsItemGap),
            modifier = Modifier.padding(start = TvDimens.SettingsPaneStartInset, top = TvDimens.SettingsListTopPadding),
        ) {
        SettingsFieldRow(
            label = stringResource(R.string.tv_settings_repository_name),
            value = name,
            onValueChange = { name = it },
            idleHint = stringResource(R.string.tv_settings_optional_not_set),
            focusedHint = stringResource(R.string.tv_settings_optional_keyboard),
            icon = Icons.Default.Edit,
            enabled = !isLoading,
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            rowFocusRequester = nameRequester,
        )
        SettingsFieldRow(
            label = stringResource(R.string.tv_settings_repository_url),
            value = url,
            onValueChange = { url = it },
            idleHint = stringResource(R.string.tv_settings_required),
            focusedHint = stringResource(R.string.tv_settings_required_keyboard),
            icon = Icons.Default.Link,
            enabled = !isLoading,
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            rowFocusRequester = urlRequester,
        )
        error?.let { message ->
            SettingsInfoParagraph(text = message, color = TvColors.ErrorText)
        }
        if (isLoading) {
            SettingsActionRow(
                title = stringResource(R.string.tv_settings_adding_repository),
                loading = true,
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            SettingsActionRow(
                title = stringResource(R.string.add_repository),
                subtitle = if (canAdd) null else stringResource(R.string.tv_settings_enter_url_first),
                icon = Icons.Default.Check,
                enabled = canAdd,
                onClick = ::submit,
            )
        }
        }
    }
}
