package com.lagradost.cloudstream3.tv.presentation.screens.settings.account

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.syncproviders.AuthLoginRequirement
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsActionRow
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsDivider
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsFieldRow
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsLevelHeader
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsSectionLabel
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsInfoParagraph
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.LocalSettingsInfoPanel
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsGlyph
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsListRow
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsRowWidget
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.settingsInitials
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import com.lagradost.cloudstream3.ui.settings.shouldShowDevicePinCode
import kotlinx.coroutines.flow.StateFlow
import qrcode.QRCode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal const val ProviderAccountNoneTag = "provider_account_none"
internal const val ProviderAccountPrimaryActionTag = "provider_account_primary_action"
internal const val ProviderAccountLogoutActionTag = "provider_account_logout_action"
internal const val ProviderAccountCreateActionTag = "provider_account_create_action"
internal const val ProviderAccountUsernameFieldTag = "provider_account_field_username"
internal const val ProviderAccountPasswordFieldTag = "provider_account_field_password"
internal const val ProviderAccountEmailFieldTag = "provider_account_field_email"
internal const val ProviderAccountServerFieldTag = "provider_account_field_server"
internal const val ProviderAccountLoginButtonTag = "provider_account_login_button"
internal const val ProviderAccountBrowserButtonTag = "provider_account_browser_button"
internal const val ProviderAccountBrowserFallbackButtonTag = "provider_account_browser_fallback_button"
internal const val ProviderAccountCancelAuthTag = "provider_account_cancel_auth"

internal fun providerAccountItemTag(accountId: Int): String {
    return "provider_account_$accountId"
}

internal enum class ProviderAccountBackAction {
    None,
    HideAuthMode,
    NavigateBack,
}

internal fun resolveProviderAccountBackAction(
    isPreview: Boolean,
    authMode: ProviderAccountAuthMode,
): ProviderAccountBackAction {
    return when {
        isPreview -> ProviderAccountBackAction.None
        authMode != ProviderAccountAuthMode.Idle -> ProviderAccountBackAction.HideAuthMode
        else -> ProviderAccountBackAction.NavigateBack
    }
}

/**
 * One account provider level (mockups SettingsAccount / SettingsLoginQr / SettingsLoginForm):
 * the account list as radio rows + Manage actions; the login flows (QR code, form, browser) take
 * over the same card and Back / Cancel returns to the list.
 */
@Composable
fun ProviderAccountScreen(
    stateFlow: StateFlow<ProviderAccountUiState>,
    viewModel: ProviderAccountViewModel,
    providerName: String,
    createAccountUrl: String?,
    isPreview: Boolean,
    onBack: () -> Unit,
    onAccountChanged: () -> Unit,
    onOpenCreateAccount: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by stateFlow.collectAsState()
    val focusManager = LocalFocusManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onAccountChangedState by rememberUpdatedState(onAccountChanged)
    val selectedAccount = remember(uiState.accounts, uiState.selectedAccountId) {
        uiState.accounts.firstOrNull { account -> account.id == uiState.selectedAccountId }
    }
    val labelAccount = stringResource(R.string.account)
    val isInteractive = !isPreview && !uiState.isWorking
    val backAction = resolveProviderAccountBackAction(
        isPreview = isPreview,
        authMode = uiState.authMode,
    )

    DisposableEffect(lifecycleOwner, viewModel, isPreview) {
        if (isPreview) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && viewModel.refreshAccountState()) {
                    onAccountChangedState()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }

    BackHandler(enabled = backAction != ProviderAccountBackAction.None) {
        when (backAction) {
            ProviderAccountBackAction.HideAuthMode -> viewModel.dismissAuthMode()
            ProviderAccountBackAction.NavigateBack -> onBack()
            ProviderAccountBackAction.None -> Unit
        }
    }

    val authMode = uiState.authMode
    // Back from a login flow returns to the action that opened it (Add account / Log in).
    var cameFromAuth by remember { mutableStateOf(false) }
    if (authMode != ProviderAccountAuthMode.Idle) cameFromAuth = true
    val title = when (authMode) {
        ProviderAccountAuthMode.Idle -> providerName
        ProviderAccountAuthMode.InApp -> stringResource(R.string.login)
        else -> stringResource(R.string.tv_settings_login_to_format, providerName)
    }

    Column(modifier = modifier.fillMaxSize()) {
        SettingsLevelHeader(
            title = title,
            description = if (authMode == ProviderAccountAuthMode.DeviceCode && uiState.deviceCode != null) {
                stringResource(R.string.device_pin_qr_message)
            } else {
                null
            },
        )
        when (authMode) {
            ProviderAccountAuthMode.Idle -> AccountListContent(
                uiState = uiState,
                selectedAccount = selectedAccount,
                labelAccount = labelAccount,
                createAccountUrl = createAccountUrl,
                isInteractive = isInteractive,
                viewModel = viewModel,
                onAccountChanged = onAccountChanged,
                onOpenCreateAccount = onOpenCreateAccount,
                isPreview = isPreview,
                focusPrimary = cameFromAuth,
                onInitialFocused = { cameFromAuth = false },
            )

            ProviderAccountAuthMode.InApp -> InAppLoginPanel(
                requirement = uiState.loginRequirement,
                form = uiState.form,
                errorMessage = uiState.errorMessage,
                isWorking = uiState.isWorking,
                onUsernameChanged = viewModel::updateUsername,
                onPasswordChanged = viewModel::updatePassword,
                onEmailChanged = viewModel::updateEmail,
                onServerChanged = viewModel::updateServer,
                onLogin = { viewModel.loginInApp(onSuccess = onAccountChanged) },
                onCancel = viewModel::dismissAuthMode,
                focusManagerMoveDown = { focusManager.moveFocus(FocusDirection.Down) },
            )

            ProviderAccountAuthMode.DeviceCode -> DeviceCodePanel(
                deviceCode = uiState.deviceCode,
                errorMessage = uiState.errorMessage,
                isInteractive = isInteractive,
                onOpenBrowser = viewModel::launchBrowserOAuth,
                onCancel = viewModel::dismissAuthMode,
            )

            ProviderAccountAuthMode.BrowserOAuth -> BrowserOAuthPanel(
                errorMessage = uiState.errorMessage,
                isInteractive = isInteractive,
                onOpenBrowser = viewModel::launchBrowserOAuth,
                onCancel = viewModel::dismissAuthMode,
            )
        }
    }
}

@Composable
private fun AccountListContent(
    uiState: ProviderAccountUiState,
    selectedAccount: ProviderAccountItemUi?,
    labelAccount: String,
    createAccountUrl: String?,
    isInteractive: Boolean,
    viewModel: ProviderAccountViewModel,
    onAccountChanged: () -> Unit,
    onOpenCreateAccount: (String) -> Unit,
    isPreview: Boolean,
    focusPrimary: Boolean,
    onInitialFocused: () -> Unit,
) {
    val noneRequester = remember { FocusRequester() }
    val primaryRequester = remember { FocusRequester() }
    val accountRequesters = remember { HashMap<Int, FocusRequester>() }
    // Decided once per entry: resetting [focusPrimary] afterwards must not move focus again.
    val initialRequester = remember(uiState.accounts.isEmpty()) {
        when {
            focusPrimary || uiState.accounts.isEmpty() -> primaryRequester
            selectedAccount != null -> accountRequesters.getOrPut(selectedAccount.id) { FocusRequester() }
            else -> noneRequester
        }
    }
    FocusRequestEffect(
        requester = initialRequester,
        requestKey = uiState.accounts.isEmpty(),
        enabled = !isPreview,
        onFocused = onInitialFocused,
    )

    val idleErrorMessage = uiState.errorMessage

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = TvDimens.SettingsPaneStartInset,
            top = TvDimens.SettingsListTopPadding,
            bottom = TvDimens.SettingsListTopPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(TvDimens.SettingsItemGap),
    ) {
        if (selectedAccount != null) {
            item(key = "provider_account_status") {
                AccountSummaryCard(
                    name = providerAccountDisplayName(labelAccount, selectedAccount),
                    status = stringResource(R.string.tv_settings_logged_in),
                )
            }
        }

        if (uiState.accounts.isNotEmpty()) {
            item(key = "provider_account_list_header") {
                SettingsSectionLabel(text = stringResource(R.string.tv_settings_active_account))
            }
            items(items = uiState.accounts, key = { account -> account.id }) { account ->
                AccountRadioRow(
                    title = providerAccountDisplayName(labelAccount, account),
                    initials = settingsInitials(providerAccountDisplayName(labelAccount, account)),
                    selected = account.id == uiState.selectedAccountId,
                    enabled = isInteractive,
                    onClick = {
                        viewModel.selectAccount(accountId = account.id, onAccountChanged = onAccountChanged)
                    },
                    modifier = Modifier
                        .focusRequester(accountRequesters.getOrPut(account.id) { FocusRequester() })
                        .testTag(providerAccountItemTag(account.id)),
                )
            }
            item(key = ProviderAccountNoneTag) {
                AccountRadioRow(
                    title = stringResource(R.string.no_account),
                    selected = uiState.selectedAccountId < 0,
                    enabled = isInteractive,
                    onClick = { viewModel.clearSelectedAccount(onAccountChanged = onAccountChanged) },
                    modifier = Modifier
                        .focusRequester(noneRequester)
                        .testTag(ProviderAccountNoneTag),
                )
            }
        }

        item(key = "provider_account_manage_header") {
            SettingsSectionLabel(text = stringResource(R.string.tv_settings_manage))
        }
        item(key = ProviderAccountPrimaryActionTag) {
            SettingsActionRow(
                title = stringResource(if (uiState.accounts.isEmpty()) R.string.login else R.string.add_account),
                enabled = isInteractive,
                onClick = { viewModel.startPreferredLogin(onAccountChanged = onAccountChanged) },
                modifier = Modifier
                    .focusRequester(primaryRequester)
                    .testTag(ProviderAccountPrimaryActionTag),
            )
        }
        if (!createAccountUrl.isNullOrBlank()) {
            item(key = ProviderAccountCreateActionTag) {
                SettingsActionRow(
                    title = stringResource(R.string.create_account),
                    enabled = isInteractive,
                    onClick = { onOpenCreateAccount(createAccountUrl) },
                    modifier = Modifier.testTag(ProviderAccountCreateActionTag),
                )
            }
        }
        if (selectedAccount != null) {
            item(key = ProviderAccountLogoutActionTag) {
                SettingsActionRow(
                    title = stringResource(R.string.logout),
                    enabled = isInteractive,
                    onClick = { viewModel.logoutSelected(onAccountChanged = onAccountChanged) },
                    modifier = Modifier.testTag(ProviderAccountLogoutActionTag),
                )
            }
        }
        if (!idleErrorMessage.isNullOrBlank()) {
            item(key = "provider_account_error") {
                SettingsInfoParagraph(text = idleErrorMessage, color = TvColors.ErrorText)
            }
        }
    }
}

@Composable
private fun AccountRadioRow(
    title: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    initials: String? = null,
) {
    SettingsListRow(
        title = title,
        onClick = onClick,
        enabled = enabled,
        leading = initials?.let { text ->
            { _ -> Text(text = text, style = TvTextStyles.SettingsTile, color = TvColors.Text, maxLines = 1) }
        },
        widget = SettingsRowWidget.Radio(selected = selected),
        modifier = modifier,
    )
}

/** Avatar (initial) + name + status of the active account; informational, never focused. */
@Composable
private fun AccountSummaryCard(
    name: String,
    status: String,
    modifier: Modifier = Modifier,
) {
    SettingsListRow(
        title = name,
        summary = status,
        onClick = {},
        leading = { _ ->
            Text(text = settingsInitials(name), style = TvTextStyles.SettingsTile, color = TvColors.Text, maxLines = 1)
        },
        modifier = modifier.focusProperties { canFocus = false },
    )
}

@Composable
private fun InAppLoginPanel(
    requirement: AuthLoginRequirement?,
    form: ProviderLoginFormUiState,
    errorMessage: String?,
    isWorking: Boolean,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onServerChanged: (String) -> Unit,
    onLogin: () -> Unit,
    onCancel: () -> Unit,
    focusManagerMoveDown: () -> Unit,
) {
    if (requirement == null) return

    val usernameRequester = remember { FocusRequester() }
    val emailRequester = remember { FocusRequester() }
    val serverRequester = remember { FocusRequester() }
    val passwordRequester = remember { FocusRequester() }
    val firstFieldRequester = when {
        requirement.username -> usernameRequester
        requirement.email -> emailRequester
        requirement.server -> serverRequester
        else -> passwordRequester
    }
    FocusRequestEffect(requester = firstFieldRequester, requestKey = Unit)

    val filled = (!requirement.username || form.username.isNotBlank()) &&
        (!requirement.email || form.email.isNotBlank()) &&
        (!requirement.server || form.server.isNotBlank()) &&
        (!requirement.password || form.password.isNotBlank())
    val canLogin = filled && !isWorking
    val requiredHint = stringResource(R.string.tv_settings_required)
    val requiredFocusedHint = stringResource(R.string.tv_settings_required_keyboard)
    Column(
        verticalArrangement = Arrangement.spacedBy(TvDimens.SettingsItemGap),
        modifier = Modifier.fillMaxWidth().padding(start = TvDimens.SettingsPaneStartInset, top = TvDimens.SettingsListTopPadding),
    ) {
        if (requirement.username) {
            SettingsFieldRow(
                label = stringResource(R.string.username),
                value = form.username,
                onValueChange = onUsernameChanged,
                idleHint = requiredHint,
                focusedHint = requiredFocusedHint,
                icon = Icons.Default.Person,
                enabled = !isWorking,
                imeAction = ImeAction.Next,
                onImeAction = focusManagerMoveDown,
                rowFocusRequester = usernameRequester,
                modifier = Modifier.testTag(ProviderAccountUsernameFieldTag),
            )
        }
        if (requirement.email) {
            SettingsFieldRow(
                label = stringResource(R.string.tv_settings_email),
                value = form.email,
                onValueChange = onEmailChanged,
                idleHint = requiredHint,
                focusedHint = requiredFocusedHint,
                icon = Icons.Default.Email,
                enabled = !isWorking,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                onImeAction = focusManagerMoveDown,
                rowFocusRequester = emailRequester,
                modifier = Modifier.testTag(ProviderAccountEmailFieldTag),
            )
        }
        if (requirement.server) {
            SettingsFieldRow(
                label = stringResource(R.string.tv_settings_server),
                value = form.server,
                onValueChange = onServerChanged,
                idleHint = requiredHint,
                focusedHint = requiredFocusedHint,
                icon = Icons.Outlined.Dns,
                enabled = !isWorking,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next,
                onImeAction = focusManagerMoveDown,
                rowFocusRequester = serverRequester,
                modifier = Modifier.testTag(ProviderAccountServerFieldTag),
            )
        }
        if (requirement.password) {
            SettingsFieldRow(
                label = stringResource(R.string.password),
                value = form.password,
                onValueChange = onPasswordChanged,
                idleHint = requiredHint,
                focusedHint = requiredFocusedHint,
                icon = Icons.Default.Lock,
                password = true,
                enabled = !isWorking,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onImeAction = { if (canLogin) onLogin() },
                rowFocusRequester = passwordRequester,
                modifier = Modifier.testTag(ProviderAccountPasswordFieldTag),
            )
        }

        if (!errorMessage.isNullOrBlank()) {
            SettingsInfoParagraph(text = errorMessage, color = TvColors.ErrorText)
        }

        if (isWorking) {
            SettingsActionRow(
                title = stringResource(R.string.tv_settings_logging_in),
                loading = true,
                onClick = {},
                modifier = Modifier.testTag(ProviderAccountLoginButtonTag),
            )
        } else {
            SettingsActionRow(
                title = stringResource(R.string.login),
                subtitle = if (canLogin) null else stringResource(R.string.tv_settings_enter_credentials),
                icon = Icons.Default.Check,
                enabled = canLogin,
                onClick = onLogin,
                modifier = Modifier.testTag(ProviderAccountLoginButtonTag),
            )
        }
        SettingsActionRow(
            title = stringResource(R.string.cancel),
            icon = Icons.Default.Close,
            enabled = !isWorking,
            onClick = onCancel,
            modifier = Modifier.testTag(ProviderAccountCancelAuthTag),
        )
    }
}

@Composable
private fun DeviceCodePanel(
    deviceCode: ProviderDeviceCodeUiState?,
    errorMessage: String?,
    isInteractive: Boolean,
    onOpenBrowser: () -> Unit,
    onCancel: () -> Unit,
) {
    val qrBitmap = remember(deviceCode?.verificationUrl) {
        deviceCode?.verificationUrl?.let { verificationUrl ->
            runCatching {
                QRCode.ofSquares()
                    .withColor(TvColors.OnFocus.toArgb())
                    .withBackgroundColor(TvColors.Focus.toArgb())
                    .build(verificationUrl)
                    .render()
                    .nativeImage() as Bitmap
            }.getOrNull()
        }
    }
    val authRequester = remember { FocusRequester() }
    val cancelRequester = remember { FocusRequester() }
    val canOpenLocal = deviceCode?.canOpenLocalAuth == true
    FocusRequestEffect(
        requester = if (canOpenLocal) authRequester else cancelRequester,
        requestKey = canOpenLocal,
        enabled = isInteractive || !canOpenLocal,
    )

    // The QR code, URL and countdown live in the info panel on the right (alpha 1, not focusable).
    val setInfoPanel = LocalSettingsInfoPanel.current
    DisposableEffect(Unit) { onDispose { setInfoPanel(null) } }
    if (deviceCode != null) {
        SideEffect {
            setInfoPanel { DeviceCodeInfoPanel(deviceCode = deviceCode, qrBitmap = qrBitmap) }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(TvDimens.SettingsItemGap),
        modifier = Modifier.fillMaxWidth().padding(start = TvDimens.SettingsPaneStartInset, top = TvDimens.SettingsListTopPadding),
    ) {
        if (!errorMessage.isNullOrBlank()) {
            SettingsInfoParagraph(text = errorMessage, color = TvColors.ErrorText)
        }

        if (canOpenLocal) {
            SettingsActionRow(
                title = stringResource(R.string.auth_locally),
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                enabled = isInteractive,
                onClick = onOpenBrowser,
                modifier = Modifier
                    .focusRequester(authRequester)
                    .testTag(ProviderAccountBrowserFallbackButtonTag),
            )
        }
        SettingsActionRow(
            title = stringResource(R.string.cancel),
            icon = Icons.Default.Close,
            onClick = onCancel,
            modifier = Modifier
                .focusRequester(cancelRequester)
                .testTag(ProviderAccountCancelAuthTag),
        )
    }
}

/** Right-hand info panel of the QR login: QR card, manual URL, code and countdown. */
@Composable
private fun DeviceCodeInfoPanel(deviceCode: ProviderDeviceCodeUiState, qrBitmap: Bitmap?) {
    Column(
        verticalArrangement = Arrangement.spacedBy(TvDimens.SettingsDp8),
        modifier = Modifier
            .fillMaxSize()
            .padding(start = TvDimens.SettingsPaneStartInset + TvDimens.SettingsTextIndent, end = TvDimens.SettingsPaneEndPadding, top = TvDimens.SettingsDecorTitleTop),
    ) {
        qrBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(TvDimens.SettingsQrSize)
                    .background(TvColors.Focus, RoundedCornerShape(TvDimens.SettingsQrRadius))
                    .padding(TvDimens.SettingsQrPadding),
            )
        }
        Text(
            text = stringResource(R.string.device_pin_manual_url_title).uppercase(),
            style = TvTextStyles.SettingsCategory,
            color = TvColors.LabelPanel,
            modifier = Modifier.padding(top = TvDimens.SettingsDp22),
        )
        Text(
            text = deviceCode.verificationUrl,
            style = TvTextStyles.SettingsInfo,
            color = TvColors.Text,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (shouldShowDevicePinCode(deviceCode.userCode)) {
            Text(
                text = stringResource(R.string.tv_settings_code).uppercase(),
                style = TvTextStyles.SettingsCategory,
                color = TvColors.LabelPanel,
                modifier = Modifier.padding(top = TvDimens.SettingsDp8),
            )
            Text(text = deviceCode.userCode, style = TvTextStyles.SettingsCode, color = TvColors.Text)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TvDimens.SettingsDp5),
            modifier = Modifier.padding(top = TvDimens.SettingsDp8),
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                tint = TvColors.Accent,
                modifier = Modifier.size(TvDimens.SettingsDp12),
            )
            Text(
                text = stringResource(
                    R.string.device_pin_counter_text,
                    deviceCode.remainingSeconds / 60,
                    deviceCode.remainingSeconds % 60
                ),
                style = TvTextStyles.Chip,
                color = TvColors.Accent,
            )
        }
    }
}

@Composable
private fun BrowserOAuthPanel(
    errorMessage: String?,
    isInteractive: Boolean,
    onOpenBrowser: () -> Unit,
    onCancel: () -> Unit,
) {
    val loginRequester = remember { FocusRequester() }
    FocusRequestEffect(requester = loginRequester, requestKey = Unit)

    Column(
        verticalArrangement = Arrangement.spacedBy(TvDimens.SettingsItemGap),
        modifier = Modifier.fillMaxWidth().padding(start = TvDimens.SettingsPaneStartInset, top = TvDimens.SettingsListTopPadding),
    ) {
        if (!errorMessage.isNullOrBlank()) {
            SettingsInfoParagraph(text = errorMessage, color = TvColors.ErrorText)
        }
        SettingsActionRow(
            title = stringResource(R.string.login),
            icon = Icons.AutoMirrored.Filled.OpenInNew,
            enabled = isInteractive,
            onClick = onOpenBrowser,
            modifier = Modifier
                .focusRequester(loginRequester)
                .testTag(ProviderAccountBrowserButtonTag),
        )
        SettingsActionRow(
            title = stringResource(R.string.cancel),
            icon = Icons.Default.Close,
            enabled = isInteractive,
            onClick = onCancel,
            modifier = Modifier.testTag(ProviderAccountCancelAuthTag),
        )
    }
}

private fun providerAccountDisplayName(
    labelAccount: String,
    account: ProviderAccountItemUi
): String {
    return account.name?.takeIf { name -> name.isNotBlank() }
        ?: "$labelAccount ${account.index + 1}"
}
