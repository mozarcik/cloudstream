package com.lagradost.cloudstream3.tv.presentation.screens.settings.account

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.syncproviders.AuthLoginRequirement
import com.lagradost.cloudstream3.ui.settings.shouldShowDevicePinCode
import kotlinx.coroutines.flow.StateFlow
import qrcode.QRCode

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

private object ProviderAccountTokens {
    val ContentSpacing = 16.dp
    val SectionSpacing = 12.dp
    val ButtonSpacing = 10.dp
    val CardPadding = 18.dp
    val FieldLabelBottomPadding = 6.dp
    val FieldPadding = 14.dp
    val FieldShape = RoundedCornerShape(12.dp)
    val QrCodeSize = 220.dp
    const val DisabledAlpha = 0.5f
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
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onAccountChangedState by rememberUpdatedState(onAccountChanged)
    val selectedAccount = remember(uiState.accounts, uiState.selectedAccountId) {
        uiState.accounts.firstOrNull { account -> account.id == uiState.selectedAccountId }
    }
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = ProviderAccountTokens.ContentSpacing),
        verticalArrangement = Arrangement.spacedBy(ProviderAccountTokens.ContentSpacing)
    ) {
        item(key = "provider_account_header") {
            Text(
                text = providerName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item(key = "provider_account_status") {
            AccountStatusCard(
                accountName = selectedAccount?.let { account ->
                    providerAccountDisplayName(
                        labelAccount = context.getString(R.string.account),
                        account = account
                    )
                } ?: context.getString(R.string.no_account),
                accountSubtitle = selectedAccount?.let { account ->
                    context.getString(
                        R.string.logged_account,
                        providerAccountDisplayName(
                            labelAccount = context.getString(R.string.account),
                            account = account
                        )
                    )
                } ?: context.getString(R.string.no_account)
            )
        }

        if (uiState.accounts.isNotEmpty()) {
            item(key = "provider_account_list_header") {
                Text(
                    text = context.getString(R.string.pref_category_accounts),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item(key = ProviderAccountNoneTag) {
                AccountActionButton(
                    title = context.getString(R.string.no_account),
                    selected = uiState.selectedAccountId < 0,
                    enabled = isInteractive,
                    onClick = {
                        viewModel.clearSelectedAccount(
                            onAccountChanged = onAccountChanged
                        )
                    },
                    modifier = Modifier.testTag(ProviderAccountNoneTag)
                )
            }

            items(
                items = uiState.accounts,
                key = { account -> account.id }
            ) { account ->
                AccountActionButton(
                    title = providerAccountDisplayName(
                        labelAccount = context.getString(R.string.account),
                        account = account
                    ),
                    selected = account.id == uiState.selectedAccountId,
                    enabled = isInteractive,
                    onClick = {
                        viewModel.selectAccount(
                            accountId = account.id,
                            onAccountChanged = onAccountChanged
                        )
                    },
                    modifier = Modifier.testTag(providerAccountItemTag(account.id))
                )
            }
        }

        if (uiState.authMode == ProviderAccountAuthMode.Idle) {
            item(key = ProviderAccountPrimaryActionTag) {
                AccountActionButton(
                    title = context.getString(
                        if (uiState.accounts.isEmpty()) {
                            R.string.login
                        } else {
                            R.string.add_account
                        }
                    ),
                    selected = false,
                    enabled = isInteractive,
                    onClick = {
                        viewModel.startPreferredLogin(onAccountChanged = onAccountChanged)
                    },
                    modifier = Modifier.testTag(ProviderAccountPrimaryActionTag)
                )
            }
        }

        if (selectedAccount != null) {
            item(key = ProviderAccountLogoutActionTag) {
                AccountActionButton(
                    title = context.getString(R.string.logout),
                    selected = false,
                    enabled = isInteractive,
                    onClick = {
                        viewModel.logoutSelected(
                            onAccountChanged = onAccountChanged
                        )
                    },
                    modifier = Modifier.testTag(ProviderAccountLogoutActionTag)
                )
            }
        }

        if (!createAccountUrl.isNullOrBlank()) {
            item(key = ProviderAccountCreateActionTag) {
                AccountActionButton(
                    title = context.getString(R.string.create_account),
                    selected = false,
                    enabled = isInteractive,
                    onClick = {
                        onOpenCreateAccount(createAccountUrl)
                    },
                    modifier = Modifier.testTag(ProviderAccountCreateActionTag)
                )
            }
        }

        when (uiState.authMode) {
            ProviderAccountAuthMode.Idle -> {
                val idleErrorMessage = uiState.errorMessage
                if (!idleErrorMessage.isNullOrBlank()) {
                    item(key = "provider_account_error") {
                        ErrorMessage(message = idleErrorMessage)
                    }
                }
            }

            ProviderAccountAuthMode.InApp -> {
                item(key = "provider_account_in_app_form") {
                    InAppLoginPanel(
                        requirement = uiState.loginRequirement,
                        form = uiState.form,
                        errorMessage = uiState.errorMessage,
                        isInteractive = isInteractive,
                        onUsernameChanged = viewModel::updateUsername,
                        onPasswordChanged = viewModel::updatePassword,
                        onEmailChanged = viewModel::updateEmail,
                        onServerChanged = viewModel::updateServer,
                        onLogin = {
                            viewModel.loginInApp(onSuccess = onAccountChanged)
                        },
                        onCancel = viewModel::dismissAuthMode,
                        modifier = Modifier.fillMaxWidth(),
                        focusManagerMoveDown = {
                            focusManager.moveFocus(FocusDirection.Down)
                        }
                    )
                }
            }

            ProviderAccountAuthMode.DeviceCode -> {
                item(key = "provider_account_device_code") {
                    DeviceCodePanel(
                        deviceCode = uiState.deviceCode,
                        errorMessage = uiState.errorMessage,
                        isInteractive = isInteractive,
                        onOpenBrowser = viewModel::launchBrowserOAuth,
                        onCancel = viewModel::dismissAuthMode,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            ProviderAccountAuthMode.BrowserOAuth -> {
                item(key = "provider_account_browser_oauth") {
                    BrowserOAuthPanel(
                        errorMessage = uiState.errorMessage,
                        isInteractive = isInteractive,
                        onOpenBrowser = viewModel::launchBrowserOAuth,
                        onCancel = viewModel::dismissAuthMode,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountStatusCard(
    accountName: String,
    accountSubtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ProviderAccountTokens.FieldShape,
        colors = SurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ProviderAccountTokens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(ProviderAccountTokens.SectionSpacing)
        ) {
            Text(
                text = accountName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = accountSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccountActionButton(
    title: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.fillMaxWidth()
        ) {
            Text(text = title)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.fillMaxWidth()
        ) {
            Text(text = title)
        }
    }
}

@Composable
private fun InAppLoginPanel(
    requirement: AuthLoginRequirement?,
    form: ProviderLoginFormUiState,
    errorMessage: String?,
    isInteractive: Boolean,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onServerChanged: (String) -> Unit,
    onLogin: () -> Unit,
    onCancel: () -> Unit,
    focusManagerMoveDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (requirement == null) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ProviderAccountTokens.SectionSpacing)
    ) {
        if (requirement.username) {
            ProviderTextField(
                value = form.username,
                onValueChange = onUsernameChanged,
                label = stringResource(R.string.username),
                placeholder = stringResource(R.string.example_username),
                enabled = isInteractive,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManagerMoveDown() }
                ),
                modifier = Modifier.testTag(ProviderAccountUsernameFieldTag)
            )
        }

        if (requirement.email) {
            ProviderTextField(
                value = form.email,
                onValueChange = onEmailChanged,
                label = stringResource(R.string.example_email),
                placeholder = stringResource(R.string.example_email),
                enabled = isInteractive,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Email
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManagerMoveDown() }
                ),
                modifier = Modifier.testTag(ProviderAccountEmailFieldTag)
            )
        }

        if (requirement.server) {
            ProviderTextField(
                value = form.server,
                onValueChange = onServerChanged,
                label = stringResource(R.string.account),
                placeholder = stringResource(R.string.account),
                enabled = isInteractive,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Uri
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManagerMoveDown() }
                ),
                modifier = Modifier.testTag(ProviderAccountServerFieldTag)
            )
        }

        if (requirement.password) {
            ProviderTextField(
                value = form.password,
                onValueChange = onPasswordChanged,
                label = stringResource(R.string.password),
                placeholder = stringResource(R.string.example_password),
                enabled = isInteractive,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Password
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onLogin() }
                ),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.testTag(ProviderAccountPasswordFieldTag)
            )
        }

        if (!errorMessage.isNullOrBlank()) {
            ErrorMessage(message = errorMessage)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ProviderAccountTokens.ButtonSpacing)
        ) {
            Button(
                onClick = onLogin,
                enabled = isInteractive,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ProviderAccountLoginButtonTag)
            ) {
                Text(text = stringResource(R.string.login))
            }

            OutlinedButton(
                onClick = onCancel,
                enabled = isInteractive,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ProviderAccountCancelAuthTag)
            ) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    }
}

@Composable
private fun DeviceCodePanel(
    deviceCode: ProviderDeviceCodeUiState?,
    errorMessage: String?,
    isInteractive: Boolean,
    onOpenBrowser: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val qrForegroundColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val qrBackgroundColor = MaterialTheme.colorScheme.surface.toArgb()
    val qrBitmap = remember(
        deviceCode?.verificationUrl,
        qrForegroundColor,
        qrBackgroundColor
    ) {
        deviceCode?.verificationUrl?.let { verificationUrl ->
            runCatching {
                QRCode.ofRoundedSquares()
                    .withColor(qrForegroundColor)
                    .withBackgroundColor(qrBackgroundColor)
                    .build(verificationUrl)
                    .render()
                    .nativeImage() as Bitmap
            }.getOrNull()
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ProviderAccountTokens.SectionSpacing)
    ) {
        if (deviceCode != null) {
            qrBitmap?.let { bitmap ->
                Surface(
                    shape = ProviderAccountTokens.FieldShape,
                    colors = SurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(ProviderAccountTokens.FieldPadding)
                            .size(ProviderAccountTokens.QrCodeSize),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Text(
                text = stringResource(R.string.device_pin_qr_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (shouldShowDevicePinCode(deviceCode.userCode)) {
                Surface(
                    shape = ProviderAccountTokens.FieldShape,
                    colors = SurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(ProviderAccountTokens.CardPadding)
                    ) {
                        Text(
                            text = deviceCode.userCode,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.device_pin_manual_url_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = ProviderAccountTokens.FieldShape,
                colors = SurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = deviceCode.verificationUrl,
                    modifier = Modifier.padding(ProviderAccountTokens.CardPadding),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = stringResource(
                    R.string.device_pin_counter_text,
                    deviceCode.remainingSeconds / 60,
                    deviceCode.remainingSeconds % 60
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!errorMessage.isNullOrBlank()) {
            ErrorMessage(message = errorMessage)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ProviderAccountTokens.ButtonSpacing)
        ) {
            if (deviceCode?.canOpenLocalAuth == true) {
                Button(
                    onClick = onOpenBrowser,
                    enabled = isInteractive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ProviderAccountBrowserFallbackButtonTag)
                ) {
                    Text(text = stringResource(R.string.auth_locally))
                }
            }

            OutlinedButton(
                onClick = onCancel,
                enabled = isInteractive,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ProviderAccountCancelAuthTag)
            ) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    }
}

@Composable
private fun BrowserOAuthPanel(
    errorMessage: String?,
    isInteractive: Boolean,
    onOpenBrowser: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ProviderAccountTokens.SectionSpacing)
    ) {
        if (!errorMessage.isNullOrBlank()) {
            ErrorMessage(message = errorMessage)
        }

        Button(
            onClick = onOpenBrowser,
            enabled = isInteractive,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ProviderAccountBrowserButtonTag)
        ) {
            Text(text = stringResource(R.string.login))
        }

        OutlinedButton(
            onClick = onCancel,
            enabled = isInteractive,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ProviderAccountCancelAuthTag)
        ) {
            Text(text = stringResource(R.string.cancel))
        }
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = message,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error
    )
}

@Composable
private fun ProviderTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        imeAction = ImeAction.Next,
        keyboardType = KeyboardType.Text
    ),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val bodyLargeTextStyle = MaterialTheme.typography.bodyLarge
    val disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(
        alpha = ProviderAccountTokens.DisabledAlpha
    )
    val fieldTextStyle = remember(
        enabled,
        disabledTextColor,
        onSurfaceColor,
        bodyLargeTextStyle
    ) {
        bodyLargeTextStyle.copy(
            color = if (enabled) {
                onSurfaceColor
            } else {
                disabledTextColor
            }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ProviderAccountTokens.FieldLabelBottomPadding)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = ProviderAccountTokens.DisabledAlpha
                )
            }
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = ProviderAccountTokens.FieldShape,
            colors = SurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                visualTransformation = visualTransformation,
                textStyle = fieldTextStyle,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(ProviderAccountTokens.FieldPadding)
                    ) {
                        if (value.isBlank()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = ProviderAccountTokens.DisabledAlpha
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

private fun providerAccountDisplayName(
    labelAccount: String,
    account: ProviderAccountItemUi
): String {
    return account.name?.takeIf { name -> name.isNotBlank() }
        ?: "$labelAccount ${account.index + 1}"
}
