package com.lagradost.cloudstream3.tv.presentation.screens.settings.homeserver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.homeserver.HomeServerApi
import com.lagradost.cloudstream3.homeserver.HomeServerConfig
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsActionRow
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsFieldRow
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsInfoParagraph
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsLevelHeader
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsRowWidget
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import kotlinx.coroutines.launch

/** Home server: address (saved as typed), connection test and the upload switches. */
@Composable
fun HomeServerSettingsForm(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val urlRequester = remember { FocusRequester() }
    FocusRequestEffect(requester = urlRequester, requestKey = Unit)

    var url by remember { mutableStateOf(HomeServerConfig.baseUrl(context).orEmpty()) }
    var autoUpload by remember { mutableStateOf(HomeServerConfig.autoUpload(context)) }
    var deleteAfter by remember { mutableStateOf(HomeServerConfig.deleteAfterUpload(context)) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Boolean?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        SettingsLevelHeader(title = stringResource(R.string.home_server))
        Column(
            verticalArrangement = Arrangement.spacedBy(TvDimens.SettingsItemGap),
            modifier = Modifier.padding(start = TvDimens.SettingsPaneStartInset, top = TvDimens.SettingsListTopPadding),
        ) {
            SettingsFieldRow(
                label = stringResource(R.string.home_server_url),
                value = url,
                onValueChange = {
                    url = it
                    HomeServerConfig.setBaseUrl(context, it)
                    testResult = null
                },
                idleHint = stringResource(R.string.home_server_url_hint),
                focusedHint = stringResource(R.string.tv_settings_required_keyboard),
                icon = Icons.Default.Link,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                rowFocusRequester = urlRequester,
            )
            SettingsActionRow(
                title = stringResource(R.string.home_server_test),
                loading = testing,
                icon = Icons.Default.Refresh,
                enabled = url.isNotBlank() && !testing,
                onClick = {
                    testing = true
                    scope.launch {
                        testResult = HomeServerApi(HomeServerConfig.baseUrl(context) ?: "").health()
                        testing = false
                    }
                },
            )
            testResult?.let { ok ->
                SettingsInfoParagraph(
                    text = stringResource(if (ok) R.string.home_server_ok else R.string.home_server_unreachable),
                    color = if (ok) TvColors.Success else TvColors.ErrorText,
                )
            }
            SettingsActionRow(
                title = stringResource(R.string.home_server_auto_upload),
                widget = SettingsRowWidget.Switch(autoUpload),
                onClick = {
                    autoUpload = !autoUpload
                    HomeServerConfig.setAutoUpload(context, autoUpload)
                },
            )
            SettingsActionRow(
                title = stringResource(R.string.home_server_delete_after),
                widget = SettingsRowWidget.Switch(deleteAfter),
                onClick = {
                    deleteAfter = !deleteAfter
                    HomeServerConfig.setDeleteAfterUpload(context, deleteAfter)
                },
            )
        }
    }
}

/** Settings level with its own content (the form above); it has no preview pane. */
class HomeServerSettingsScreen(
    override val id: String,
    override val title: String,
) : com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsScreen {
    override val hasCustomContent: Boolean = true

    @Composable
    override fun Content(
        modifier: Modifier,
        contentPadding: androidx.compose.foundation.layout.PaddingValues,
        isPreview: Boolean,
        onBack: () -> Unit,
        onDataChanged: (String) -> Unit,
    ) {
        if (isPreview) return
        HomeServerSettingsForm(modifier = modifier)
    }
}
