package com.lagradost.cloudstream3.tv.presentation.screens.settings.account

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.syncproviders.AccountManager
import com.lagradost.cloudstream3.syncproviders.AuthLoginRequirement
import com.lagradost.cloudstream3.syncproviders.AuthLoginResponse
import com.lagradost.cloudstream3.syncproviders.AuthPinData
import com.lagradost.cloudstream3.syncproviders.AuthRepo
import com.lagradost.cloudstream3.ui.settings.PinRequestFailureAction
import com.lagradost.cloudstream3.ui.settings.resolvePinRequestFailureAction
import com.lagradost.cloudstream3.utils.txt
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ProviderAccountAuthMode {
    Idle,
    InApp,
    DeviceCode,
    BrowserOAuth,
}

@Immutable
data class ProviderAccountItemUi(
    val id: Int,
    val name: String?,
    val index: Int,
)

@Immutable
data class ProviderLoginFormUiState(
    val username: String = "",
    val password: String = "",
    val email: String = "",
    val server: String = "",
)

@Immutable
data class ProviderDeviceCodeUiState(
    val verificationUrl: String,
    val userCode: String,
    val remainingSeconds: Int,
    val intervalSeconds: Int,
    val canOpenLocalAuth: Boolean,
)

@Immutable
data class ProviderAccountUiState(
    val accounts: PersistentList<ProviderAccountItemUi> = persistentListOf(),
    val selectedAccountId: Int = AccountManager.NONE_ID,
    val authMode: ProviderAccountAuthMode = ProviderAccountAuthMode.Idle,
    val loginRequirement: AuthLoginRequirement? = null,
    val form: ProviderLoginFormUiState = ProviderLoginFormUiState(),
    val deviceCode: ProviderDeviceCodeUiState? = null,
    val errorMessage: String? = null,
    val isWorking: Boolean = false,
)

private data class ProviderAccountSnapshot(
    val accountIds: List<Int>,
    val selectedAccountId: Int,
)

class ProviderAccountViewModel(
    private val authRepo: AuthRepo
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProviderAccountUiState())
    val uiState: StateFlow<ProviderAccountUiState> = _uiState.asStateFlow()

    private var latestSnapshot = ProviderAccountSnapshot(
        accountIds = emptyList(),
        selectedAccountId = AccountManager.NONE_ID,
    )
    private var deviceCodePollingJob: Job? = null

    init {
        updateAccountSnapshot(
            authMode = preferredInitialAuthMode(),
            clearForm = true,
        )
    }

    fun refreshAccountState(): Boolean {
        return updateAccountSnapshot(
            authMode = _uiState.value.authMode,
            deviceCode = _uiState.value.deviceCode,
        )
    }

    fun selectAccount(accountId: Int, onAccountChanged: () -> Unit = {}) {
        if (_uiState.value.isWorking) return
        authRepo.accountId = accountId
        updateAccountSnapshot(
            authMode = ProviderAccountAuthMode.Idle,
            errorMessage = null,
        )
        onAccountChanged()
    }

    fun clearSelectedAccount(onAccountChanged: () -> Unit = {}) {
        selectAccount(AccountManager.NONE_ID, onAccountChanged = onAccountChanged)
    }

    fun showInAppForm() {
        cancelDeviceCodePolling()
        updateAccountSnapshot(
            authMode = ProviderAccountAuthMode.InApp,
            errorMessage = null,
        )
    }

    fun dismissAuthMode() {
        cancelDeviceCodePolling()
        updateAccountSnapshot(
            authMode = ProviderAccountAuthMode.Idle,
            deviceCode = null,
            errorMessage = null,
        )
    }

    fun startPreferredLogin(onAccountChanged: () -> Unit = {}) {
        when {
            authRepo.hasPin -> requestDeviceCode(onAccountChanged = onAccountChanged)
            authRepo.hasInApp -> showInAppForm()
            authRepo.hasOAuth2 -> showBrowserOAuth()
            else -> updateAccountSnapshot(
                authMode = ProviderAccountAuthMode.Idle,
                errorMessage = defaultAuthFailureMessage(),
            )
        }
    }

    fun showBrowserOAuth() {
        cancelDeviceCodePolling()
        updateAccountSnapshot(
            authMode = ProviderAccountAuthMode.BrowserOAuth,
            deviceCode = null,
            errorMessage = null,
        )
    }

    fun launchBrowserOAuth() {
        cancelDeviceCodePolling()
        updateAccountSnapshot(
            authMode = ProviderAccountAuthMode.BrowserOAuth,
            deviceCode = null,
            errorMessage = null,
        )

        val result = runCatching { authRepo.openOAuth2Page() }
        result.onFailure(::logError)
        if (result.getOrNull() == true) return

        updateAccountSnapshot(
            authMode = ProviderAccountAuthMode.BrowserOAuth,
            deviceCode = null,
            errorMessage = result.exceptionOrNull()?.toReadableMessage()
                ?: defaultAuthFailureMessage(),
        )
    }

    fun requestDeviceCode(onAccountChanged: () -> Unit = {}) {
        if (_uiState.value.isWorking) return
        cancelDeviceCodePolling()
        updateAccountSnapshot(
            authMode = ProviderAccountAuthMode.DeviceCode,
            deviceCode = null,
            errorMessage = null,
            isWorking = true,
        )

        viewModelScope.launch {
            var hasSpecificPinErrorMessage = false
            var specificPinErrorMessage: String? = null
            val pinCodeData = try {
                authRepo.pinRequest()
            } catch (error: ErrorLoadingException) {
                if (!error.message.isNullOrBlank()) {
                    hasSpecificPinErrorMessage = true
                    specificPinErrorMessage = error.message
                    null
                } else {
                    throw error
                }
            } catch (throwable: Throwable) {
                logError(throwable)
                null
            }

            if (pinCodeData == null) {
                when (
                    resolvePinRequestFailureAction(
                        hasOAuth2 = authRepo.hasOAuth2,
                        hasSpecificPinErrorMessage = hasSpecificPinErrorMessage,
                    )
                ) {
                    PinRequestFailureAction.None -> {
                        updateAccountSnapshot(
                            authMode = ProviderAccountAuthMode.Idle,
                            deviceCode = null,
                            errorMessage = specificPinErrorMessage ?: defaultAuthFailureMessage(),
                            isWorking = false,
                        )
                    }

                    PinRequestFailureAction.OpenOAuth -> {
                        updateAccountSnapshot(
                            authMode = ProviderAccountAuthMode.BrowserOAuth,
                            deviceCode = null,
                            errorMessage = contextString(R.string.device_pin_error_message),
                            isWorking = false,
                        )
                    }

                    PinRequestFailureAction.ShowGenericAuthFailure -> {
                        updateAccountSnapshot(
                            authMode = ProviderAccountAuthMode.Idle,
                            deviceCode = null,
                            errorMessage = defaultAuthFailureMessage(),
                            isWorking = false,
                        )
                    }
                }
                return@launch
            }

            updateAccountSnapshot(
                authMode = ProviderAccountAuthMode.DeviceCode,
                deviceCode = pinCodeData.toUiState(
                    canOpenLocalAuth = authRepo.hasOAuth2
                ),
                errorMessage = null,
                isWorking = false,
            )

            startDeviceCodePolling(
                pinCodeData = pinCodeData,
                onAccountChanged = onAccountChanged,
            )
        }
    }

    fun updateUsername(value: String) {
        updateForm { copy(username = value) }
    }

    fun updatePassword(value: String) {
        updateForm { copy(password = value) }
    }

    fun updateEmail(value: String) {
        updateForm { copy(email = value) }
    }

    fun updateServer(value: String) {
        updateForm { copy(server = value) }
    }

    fun loginInApp(onSuccess: () -> Unit = {}) {
        val currentState = _uiState.value
        val loginRequirement = authRepo.inAppLoginRequirement ?: return
        if (currentState.isWorking) return

        _uiState.value = currentState.copy(
            isWorking = true,
            errorMessage = null,
        )

        viewModelScope.launch {
            val result = runCatching {
                authRepo.login(
                    form = currentState.form.toLoginResponse(loginRequirement)
                )
            }

            result.onSuccess { success ->
                if (success) {
                    showToast(txt(R.string.authenticated_user, authRepo.name))
                    updateAccountSnapshot(
                        authMode = ProviderAccountAuthMode.Idle,
                        errorMessage = null,
                        isWorking = false,
                        clearForm = true,
                    )
                    onSuccess()
                } else {
                    updateAccountSnapshot(
                        authMode = ProviderAccountAuthMode.InApp,
                        errorMessage = defaultAuthFailureMessage(),
                        isWorking = false,
                    )
                }
            }.onFailure { throwable ->
                logError(throwable)
                updateAccountSnapshot(
                    authMode = ProviderAccountAuthMode.InApp,
                    errorMessage = throwable.toReadableMessage() ?: defaultAuthFailureMessage(),
                    isWorking = false,
                )
            }
        }
    }

    fun logoutSelected(onAccountChanged: () -> Unit = {}) {
        val selectedUser = authRepo.authUser() ?: return
        if (_uiState.value.isWorking) return

        _uiState.value = _uiState.value.copy(
            isWorking = true,
            errorMessage = null,
        )

        viewModelScope.launch {
            val result = runCatching {
                authRepo.logout(selectedUser)
            }

            result.onFailure { throwable ->
                logError(throwable)
                showToast(throwable.toReadableMessage() ?: defaultAuthFailureMessage())
            }

            updateAccountSnapshot(
                authMode = preferredInitialAuthMode(),
                errorMessage = null,
                isWorking = false,
                clearForm = authRepo.accounts.isEmpty(),
            )
            onAccountChanged()
        }
    }

    private fun updateForm(update: ProviderLoginFormUiState.() -> ProviderLoginFormUiState) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            form = currentState.form.update(),
            errorMessage = null,
        )
    }

    private fun startDeviceCodePolling(
        pinCodeData: AuthPinData,
        onAccountChanged: () -> Unit,
    ) {
        cancelDeviceCodePolling()
        val startTimeMs = System.currentTimeMillis()
        var lastPolledSecond = -1

        deviceCodePollingJob = viewModelScope.launch {
            while (true) {
                val elapsedSeconds = ((System.currentTimeMillis() - startTimeMs) / 1000L).toInt()
                val remainingSeconds = (pinCodeData.expiresIn - elapsedSeconds).coerceAtLeast(0)

                _uiState.value = _uiState.value.copy(
                    deviceCode = _uiState.value.deviceCode?.copy(
                        remainingSeconds = remainingSeconds
                    )
                )

                if (remainingSeconds <= 0) {
                    showToast(R.string.device_pin_expired_message)
                    updateAccountSnapshot(
                        authMode = ProviderAccountAuthMode.Idle,
                        deviceCode = null,
                        errorMessage = contextString(R.string.device_pin_expired_message),
                    )
                    deviceCodePollingJob = null
                    break
                }

                if (
                    elapsedSeconds % pinCodeData.interval == 0 &&
                    elapsedSeconds != lastPolledSecond
                ) {
                    lastPolledSecond = elapsedSeconds
                    val isSuccessful = runCatching { authRepo.login(pinCodeData) }
                        .onFailure(::logError)
                        .getOrDefault(false)
                    if (isSuccessful) {
                        showToast(txt(R.string.authenticated_user, authRepo.name))
                        updateAccountSnapshot(
                            authMode = ProviderAccountAuthMode.Idle,
                            deviceCode = null,
                            errorMessage = null,
                            clearForm = true,
                        )
                        deviceCodePollingJob = null
                        onAccountChanged()
                        break
                    }
                }

                delay(1000L)
            }
        }
    }

    private fun cancelDeviceCodePolling() {
        deviceCodePollingJob?.cancel()
        deviceCodePollingJob = null
    }

    private fun updateAccountSnapshot(
        authMode: ProviderAccountAuthMode = _uiState.value.authMode,
        deviceCode: ProviderDeviceCodeUiState? = _uiState.value.deviceCode,
        errorMessage: String? = _uiState.value.errorMessage,
        isWorking: Boolean = _uiState.value.isWorking,
        clearForm: Boolean = false,
    ): Boolean {
        val currentState = _uiState.value
        val accounts = authRepo.accounts
            .mapIndexed { index, account ->
                ProviderAccountItemUi(
                    id = account.user.id,
                    name = account.user.name,
                    index = index,
                )
            }
            .toPersistentList()
        val currentSnapshot = ProviderAccountSnapshot(
            accountIds = accounts.map { it.id },
            selectedAccountId = authRepo.accountId,
        )
        val snapshotChanged = currentSnapshot != latestSnapshot
        latestSnapshot = currentSnapshot

        if (snapshotChanged && authMode == ProviderAccountAuthMode.DeviceCode) {
            cancelDeviceCodePolling()
        }

        val resolvedAuthMode = when {
            snapshotChanged && (
                authMode == ProviderAccountAuthMode.DeviceCode ||
                    authMode == ProviderAccountAuthMode.BrowserOAuth
                ) -> ProviderAccountAuthMode.Idle
            else -> authMode
        }

        _uiState.value = currentState.copy(
            accounts = accounts,
            selectedAccountId = authRepo.accountId,
            authMode = resolvedAuthMode,
            loginRequirement = authRepo.inAppLoginRequirement,
            form = if (clearForm) {
                ProviderLoginFormUiState()
            } else {
                currentState.form
            },
            deviceCode = if (resolvedAuthMode == ProviderAccountAuthMode.DeviceCode) {
                deviceCode
            } else {
                null
            },
            errorMessage = errorMessage,
            isWorking = isWorking,
        )
        return snapshotChanged
    }

    private fun preferredInitialAuthMode(): ProviderAccountAuthMode {
        return if (authRepo.accounts.isEmpty() && authRepo.hasInApp) {
            ProviderAccountAuthMode.InApp
        } else {
            ProviderAccountAuthMode.Idle
        }
    }

    private fun defaultAuthFailureMessage(): String {
        return contextString(R.string.authenticated_user_fail, authRepo.name)
    }

    private fun contextString(resId: Int, vararg formatArgs: Any): String {
        val context = CloudStreamApp.context
        return if (context != null) {
            context.getString(resId, *formatArgs)
        } else {
            resId.toString()
        }
    }

    private fun Throwable.toReadableMessage(): String? {
        return (this as? ErrorLoadingException)?.message
            ?: message
    }
}

private fun ProviderLoginFormUiState.toLoginResponse(
    requirement: AuthLoginRequirement
): AuthLoginResponse {
    return AuthLoginResponse(
        username = username.takeIf { requirement.username && it.isNotBlank() },
        password = password.takeIf { requirement.password && it.isNotBlank() },
        email = email.takeIf { requirement.email && it.isNotBlank() },
        server = server.takeIf { requirement.server && it.isNotBlank() },
    )
}

private fun AuthPinData.toUiState(
    canOpenLocalAuth: Boolean
): ProviderDeviceCodeUiState {
    return ProviderDeviceCodeUiState(
        verificationUrl = verificationUrl,
        userCode = userCode,
        remainingSeconds = expiresIn,
        intervalSeconds = interval,
        canOpenLocalAuth = canOpenLocalAuth,
    )
}
