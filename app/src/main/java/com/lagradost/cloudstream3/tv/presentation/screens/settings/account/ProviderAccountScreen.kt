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
import com.lagradost.cloudstream3.tv.presentation.common.TvListRow
import com.lagradost.cloudstream3.tv.presentation.common.TvRowLeading
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsActionRow
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsDivider
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsFieldRow
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsLevelHeader
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsPreviewHeader
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsPreviewRow
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsPreviewSection
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsSectionLabel
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.SettingsStateBox
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.LocalSettingsBreadcrumb
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

    if (isPreview) {
        ProviderAccountPreview(
            uiState = uiState,
            selectedAccount = selectedAccount,
            providerName = providerName,
            labelAccount = labelAccount,
            hasCreateAccount = !createAccountUrl.isNullOrBlank(),
            modifier = modifier,
        )
        return
    }

    val authMode = uiState.authMode
    // Back from a login flow returns to the action that opened it (Add account / Log in).
    var cameFromAuth by remember { mutableStateOf(false) }
    if (authMode != ProviderAccountAuthMode.Idle) cameFromAuth = true
    val baseCrumb = LocalSettingsBreadcrumb.current
    val crumb = if (authMode == ProviderAccountAuthMode.Idle) {
        baseCrumb
    } else {
        listOf(baseCrumb, providerName).filter { it.isNotEmpty() }.joinToString(" › ")
    }
    val title = when (authMode) {
        ProviderAccountAuthMode.Idle -> providerName
        ProviderAccountAuthMode.InApp -> stringResource(R.string.login)
        else -> stringResource(R.string.tv_settings_login_to_format, providerName)
    }

    Column(modifier = modifier.fillMaxSize()) {
        SettingsLevelHeader(title = title, crumb = crumb)
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
        onFocused = onInitialFocused,
    )

    val idleErrorMessage = uiState.errorMessage

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = TvDimens.SettingsCardPaddingVertical),
    ) {
        if (selectedAccount != null) {
            item(key = "provider_account_status") {
                AccountSummaryCard(
                    name = providerAccountDisplayName(labelAccount, selectedAccount),
                    status = stringResource(R.string.tv_settings_logged_in),
                    dimmed = false,
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
                SettingsStateBox(message = idleErrorMessage, modifier = Modifier.padding(top = TvDimens.SettingsDp6))
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
) {
    TvListRow(
        title = title,
        onClick = onClick,
        enabled = enabled,
        height = TvDimens.SettingsRadioRowHeight,
        horizontalPadding = TvDimens.SettingsRowHorizontalPadding,
        gap = TvDimens.SettingsRowGap,
        leadingIndicator = TvRowLeading.Radio(selected = selected),
        modifier = modifier,
    )
}

/** Avatar + name + status in an inset card (active level and preview, the preview is dimmed). */
@Composable
private fun AccountSummaryCard(
    name: String,
    status: String,
    dimmed: Boolean,
    modifier: Modifier = Modifier,
) {
    val nameColor = if (dimmed) TvColors.SettingsPreviewText else TvColors.Text
    val statusColor = if (dimmed) TvColors.SettingsPreviewSubtext else TvColors.RowSubtitle
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.SettingsDp9),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = TvDimens.SettingsDp6, start = if (dimmed) TvDimens.SettingsDp12 else TvDimens.SettingsDp0, end = if (dimmed) TvDimens.SettingsDp12 else TvDimens.SettingsDp0)
            .background(TvColors.SettingsInset, RoundedCornerShape(TvDimens.SettingsDp9))
            .padding(horizontal = TvDimens.SettingsDp11, vertical = TvDimens.SettingsDp10),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(TvDimens.SettingsAvatarSize)
                .background(TvColors.SettingsAvatar, CircleShape),
        ) {
            Text(
                text = name.take(1).uppercase(),
                style = TvTextStyles.RowPanel,
                color = if (dimmed) TvColors.SettingsPreviewText else TvColors.Meta,
            )
        }
        Column {
            Text(text = name, style = TvTextStyles.SettingsPreviewRow, color = nameColor, maxLines = 1)
            Text(
                text = status,
                style = TvTextStyles.SettingsPreviewSubtitle,
                color = statusColor,
                modifier = Modifier.padding(top = TvDimens.SettingsDp1),
            )
        }
    }
}

/** Non-interactive card for the right-hand side while the focus is on the provider row. */
@Composable
private fun ProviderAccountPreview(
    uiState: ProviderAccountUiState,
    selectedAccount: ProviderAccountItemUi?,
    providerName: String,
    labelAccount: String,
    hasCreateAccount: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        SettingsPreviewHeader(title = providerName)
        if (selectedAccount != null) {
            AccountSummaryCard(
                name = providerAccountDisplayName(labelAccount, selectedAccount),
                status = stringResource(R.string.tv_settings_logged_in),
                dimmed = true,
            )
        }
        if (uiState.accounts.isNotEmpty()) {
            SettingsPreviewSection(text = stringResource(R.string.tv_settings_active_account))
            uiState.accounts.forEach { account ->
                PreviewAccountRow(
                    title = providerAccountDisplayName(labelAccount, account),
                    selected = account.id == uiState.selectedAccountId,
                )
            }
            PreviewAccountRow(title = stringResource(R.string.no_account), selected = uiState.selectedAccountId < 0)
        }
        SettingsPreviewSection(text = stringResource(R.string.tv_settings_manage))
        SettingsPreviewRow(
            title = stringResource(if (uiState.accounts.isEmpty()) R.string.login else R.string.add_account),
            height = TvDimens.SettingsDp38,
        )
        if (hasCreateAccount) {
            SettingsPreviewRow(title = stringResource(R.string.create_account), height = TvDimens.SettingsDp38)
        }
        if (selectedAccount != null) {
            SettingsPreviewRow(title = stringResource(R.string.logout), height = TvDimens.SettingsDp38)
        }
    }
}

@Composable
private fun PreviewAccountRow(title: String, selected: Boolean) {
    SettingsPreviewRow(
        title = title,
        height = TvDimens.SettingsDp38,
        leading = {
            Canvas(modifier = Modifier.size(TvDimens.SettingsDp14)) {
                val border = TvDimens.SettingsDp1_5.toPx()
                val color = if (selected) TvColors.SettingsPreviewRadioSelected else TvColors.SettingsPreviewRadio
                drawCircle(color = color, radius = (size.minDimension - border) / 2f, style = Stroke(width = border))
                if (selected) drawCircle(color = color, radius = TvDimens.SettingsDp3.toPx(), center = Offset(size.width / 2f, size.height / 2f))
            }
        },
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
    Column(modifier = Modifier.fillMaxWidth()) {
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
            SettingsStateBox(message = errorMessage, modifier = Modifier.padding(top = TvDimens.SettingsDp8))
        }
        SettingsDivider(top = TvDimens.SettingsDp10, bottom = TvDimens.SettingsDp10)

        if (isWorking) {
            SettingsActionRow(
                title = stringResource(R.string.tv_settings_logging_in),
                loading = true,
                onClick = {},
                height = TvDimens.SettingsFormRowHeight,
                modifier = Modifier.testTag(ProviderAccountLoginButtonTag),
            )
        } else {
            SettingsActionRow(
                title = stringResource(R.string.login),
                subtitle = if (canLogin) null else stringResource(R.string.tv_settings_enter_credentials),
                icon = Icons.Default.Check,
                enabled = canLogin,
                onClick = onLogin,
                height = TvDimens.SettingsFormRowHeight,
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

    Column(modifier = Modifier.fillMaxWidth()) {
        if (deviceCode != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(TvDimens.SettingsQrGap),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TvDimens.SettingsHeaderHorizontalPadding),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.device_pin_qr_message),
                        style = TvTextStyles.FeaturedBody,
                        color = TvColors.Meta,
                    )
                    Text(
                        text = stringResource(R.string.device_pin_manual_url_title).uppercase(),
                        style = TvTextStyles.SettingsLabelSmall,
                        color = TvColors.LabelPanel,
                        modifier = Modifier.padding(top = TvDimens.SettingsDp13),
                    )
                    Text(
                        text = deviceCode.verificationUrl,
                        // Mockup: 26 px / 600 (13 sp), a bit larger than the generic meta text.
                        style = TvTextStyles.Meta.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                        color = TvColors.Text,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = TvDimens.SettingsDp3),
                    )
                    if (shouldShowDevicePinCode(deviceCode.userCode)) {
                        Text(
                            text = stringResource(R.string.tv_settings_code),
                            style = TvTextStyles.SettingsLabelSmall,
                            color = TvColors.LabelPanel,
                            modifier = Modifier.padding(top = TvDimens.SettingsDp11),
                        )
                        Text(
                            text = deviceCode.userCode,
                            style = TvTextStyles.SettingsCode,
                            color = TvColors.Text,
                            modifier = Modifier.padding(top = TvDimens.SettingsDp3),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TvDimens.SettingsDp5),
                        modifier = Modifier.padding(top = TvDimens.SettingsDp7),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = TvColors.Accent,
                            modifier = Modifier.size(TvDimens.SettingsDp11),
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
        }

        if (!errorMessage.isNullOrBlank()) {
            SettingsStateBox(message = errorMessage, modifier = Modifier.padding(top = TvDimens.SettingsDp8))
        }
        SettingsDivider(top = TvDimens.SettingsDp18, bottom = TvDimens.SettingsDp10)

        if (canOpenLocal) {
            SettingsActionRow(
                title = stringResource(R.string.auth_locally),
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                enabled = isInteractive,
                onClick = onOpenBrowser,
                height = TvDimens.SettingsQrActionRowHeight,
                modifier = Modifier
                    .focusRequester(authRequester)
                    .testTag(ProviderAccountBrowserFallbackButtonTag),
            )
        }
        SettingsActionRow(
            title = stringResource(R.string.cancel),
            icon = Icons.Default.Close,
            onClick = onCancel,
            height = TvDimens.SettingsQrActionRowHeight,
            modifier = Modifier
                .focusRequester(cancelRequester)
                .testTag(ProviderAccountCancelAuthTag),
        )
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

    Column(modifier = Modifier.fillMaxWidth()) {
        if (!errorMessage.isNullOrBlank()) {
            SettingsStateBox(message = errorMessage, modifier = Modifier.padding(bottom = TvDimens.SettingsDp8))
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
