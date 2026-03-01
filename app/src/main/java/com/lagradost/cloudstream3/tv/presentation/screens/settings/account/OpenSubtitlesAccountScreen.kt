package com.lagradost.cloudstream3.tv.presentation.screens.settings.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import kotlinx.coroutines.flow.StateFlow

private object OpenSubtitlesAccountTokens {
    val ContentSpacing = 16.dp
    val SectionSpacing = 12.dp
    val ButtonSpacing = 10.dp
    val CardPadding = 18.dp
    val FieldLabelBottomPadding = 6.dp
    val FieldPadding = 14.dp
    val FieldShape = RoundedCornerShape(12.dp)
    const val DisabledAlpha = 0.5f
}

@Composable
fun OpenSubtitlesAccountScreen(
    stateFlow: StateFlow<OpenSubtitlesAccountUiState>,
    viewModel: OpenSubtitlesAccountViewModel,
    providerName: String,
    createAccountUrl: String?,
    isPreview: Boolean,
    onAccountChanged: () -> Unit,
    onOpenCreateAccount: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by stateFlow.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val selectedAccount = remember(uiState.accounts, uiState.selectedAccountId) {
        uiState.accounts.firstOrNull { account -> account.id == uiState.selectedAccountId }
    }
    val isInteractive = !isPreview && !uiState.isWorking

    LaunchedEffect(viewModel) {
        viewModel.refreshAccountState()
    }

    BackHandler(
        enabled = !isPreview && uiState.showLoginForm && uiState.accounts.isNotEmpty()
    ) {
        viewModel.hideLoginForm()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = OpenSubtitlesAccountTokens.ContentSpacing),
        verticalArrangement = Arrangement.spacedBy(OpenSubtitlesAccountTokens.ContentSpacing)
    ) {
        item(key = "opensubtitles_header") {
            Text(
                text = providerName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item(key = "opensubtitles_status") {
            AccountStatusCard(
                accountName = selectedAccount?.let { account ->
                    accountDisplayName(
                        labelAccount = context.getString(R.string.account),
                        account = account
                    )
                } ?: context.getString(R.string.no_account),
                accountSubtitle = selectedAccount?.let { account ->
                    context.getString(
                        R.string.logged_account,
                        accountDisplayName(
                            labelAccount = context.getString(R.string.account),
                            account = account
                        )
                    )
                } ?: context.getString(R.string.no_account)
            )
        }

        if (uiState.accounts.isNotEmpty()) {
            item(key = "opensubtitles_accounts_header") {
                Text(
                    text = context.getString(R.string.pref_category_accounts),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item(key = "opensubtitles_none_account") {
                AccountActionButton(
                    title = context.getString(R.string.no_account),
                    selected = uiState.selectedAccountId < 0,
                    enabled = isInteractive,
                    onClick = {
                        viewModel.clearSelectedAccount(onAccountChanged = onAccountChanged)
                    }
                )
            }

            items(
                items = uiState.accounts,
                key = { account -> "opensubtitles_account_${account.id}" }
            ) { account ->
                AccountActionButton(
                    title = accountDisplayName(
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
                    }
                )
            }
        }

        if (!uiState.showLoginForm) {
            item(key = "opensubtitles_primary_action") {
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
                        viewModel.showLoginForm()
                    }
                )
            }
        }

        if (selectedAccount != null) {
            item(key = "opensubtitles_logout_action") {
                AccountActionButton(
                    title = context.getString(R.string.logout),
                    selected = false,
                    enabled = isInteractive,
                    onClick = {
                        viewModel.logoutSelected(
                            context = context,
                            onAccountChanged = onAccountChanged
                        )
                    }
                )
            }
        }

        if (!createAccountUrl.isNullOrBlank()) {
            item(key = "opensubtitles_create_account_action") {
                AccountActionButton(
                    title = context.getString(R.string.create_account),
                    selected = false,
                    enabled = isInteractive,
                    onClick = {
                        onOpenCreateAccount(createAccountUrl)
                    }
                )
            }
        }

        if (uiState.showLoginForm) {
            item(key = "opensubtitles_login_form") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(OpenSubtitlesAccountTokens.SectionSpacing)
                ) {
                    TvAccountTextField(
                        value = uiState.username,
                        onValueChange = viewModel::updateUsername,
                        label = context.getString(R.string.username),
                        placeholder = context.getString(R.string.example_username),
                        enabled = isInteractive,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Text
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    TvAccountTextField(
                        value = uiState.password,
                        onValueChange = viewModel::updatePassword,
                        label = context.getString(R.string.password),
                        placeholder = context.getString(R.string.example_password),
                        enabled = isInteractive,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Password
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        visualTransformation = PasswordVisualTransformation()
                    )

                    uiState.errorMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(OpenSubtitlesAccountTokens.ButtonSpacing)
                    ) {
                        Button(
                            onClick = {
                                viewModel.login(
                                    context = context,
                                    onSuccess = onAccountChanged
                                )
                            },
                            enabled = isInteractive,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = context.getString(R.string.login)
                            )
                        }

                        if (uiState.accounts.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.hideLoginForm()
                                },
                                enabled = isInteractive,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = context.getString(android.R.string.cancel))
                            }
                        }
                    }
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
        shape = OpenSubtitlesAccountTokens.FieldShape,
        colors = SurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OpenSubtitlesAccountTokens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(OpenSubtitlesAccountTokens.SectionSpacing)
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
private fun TvAccountTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    enabled: Boolean,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val bodyLargeTextStyle = MaterialTheme.typography.bodyLarge
    val disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(
        alpha = OpenSubtitlesAccountTokens.DisabledAlpha
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
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(OpenSubtitlesAccountTokens.FieldLabelBottomPadding)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = OpenSubtitlesAccountTokens.DisabledAlpha
                )
            }
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = OpenSubtitlesAccountTokens.FieldShape,
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
                            .padding(OpenSubtitlesAccountTokens.FieldPadding)
                    ) {
                        if (value.isBlank()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = OpenSubtitlesAccountTokens.DisabledAlpha
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

private fun accountDisplayName(
    labelAccount: String,
    account: OpenSubtitlesAccountItemUi
): String {
    return account.name?.takeIf { name -> name.isNotBlank() }
        ?: "$labelAccount ${account.index + 1}"
}
