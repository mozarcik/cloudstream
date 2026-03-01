package com.lagradost.cloudstream3.tv.presentation.screens.settings.account

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.syncproviders.AccountManager.Companion.NONE_ID
import com.lagradost.cloudstream3.syncproviders.AuthLoginResponse
import com.lagradost.cloudstream3.syncproviders.AuthRepo
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
data class OpenSubtitlesAccountItemUi(
    val id: Int,
    val name: String?,
    val index: Int
)

@Immutable
data class OpenSubtitlesAccountUiState(
    val accounts: PersistentList<OpenSubtitlesAccountItemUi> = persistentListOf(),
    val selectedAccountId: Int = NONE_ID,
    val username: String = "",
    val password: String = "",
    val errorMessage: String? = null,
    val showLoginForm: Boolean = true,
    val isWorking: Boolean = false
)

class OpenSubtitlesAccountViewModel(
    private val authRepo: AuthRepo
) : ViewModel() {
    private val _uiState = MutableStateFlow(OpenSubtitlesAccountUiState())
    val uiState: StateFlow<OpenSubtitlesAccountUiState> = _uiState.asStateFlow()

    init {
        refreshAccountState()
    }

    fun refreshAccountState() {
        updateAccountSnapshot()
    }

    fun updateUsername(value: String) {
        _uiState.value = _uiState.value.copy(
            username = value,
            errorMessage = null
        )
    }

    fun updatePassword(value: String) {
        _uiState.value = _uiState.value.copy(
            password = value,
            errorMessage = null
        )
    }

    fun showLoginForm() {
        _uiState.value = _uiState.value.copy(
            showLoginForm = true,
            errorMessage = null
        )
    }

    fun hideLoginForm() {
        if (_uiState.value.accounts.isEmpty()) return
        _uiState.value = _uiState.value.copy(
            showLoginForm = false,
            password = "",
            errorMessage = null
        )
    }

    fun selectAccount(accountId: Int, onAccountChanged: () -> Unit = {}) {
        if (_uiState.value.isWorking) return
        authRepo.accountId = accountId
        updateAccountSnapshot(errorMessage = null)
        onAccountChanged()
    }

    fun clearSelectedAccount(onAccountChanged: () -> Unit = {}) {
        selectAccount(accountId = NONE_ID, onAccountChanged = onAccountChanged)
    }

    fun login(
        context: Context,
        onSuccess: () -> Unit = {}
    ) {
        val currentState = _uiState.value
        if (currentState.isWorking) return

        _uiState.value = currentState.copy(
            isWorking = true,
            errorMessage = null
        )

        viewModelScope.launch {
            val result = runCatching {
                authRepo.login(
                    AuthLoginResponse(
                        username = currentState.username.takeIf { it.isNotBlank() },
                        password = currentState.password.takeIf { it.isNotBlank() },
                        email = null,
                        server = null
                    )
                )
            }

            result.onSuccess { success ->
                if (success) {
                    showToast(context.getString(R.string.authenticated_user, authRepo.name))
                    updateAccountSnapshot(
                        isWorking = false,
                        errorMessage = null,
                        showLoginForm = false,
                        password = ""
                    )
                    onSuccess()
                } else {
                    updateAccountSnapshot(
                        isWorking = false,
                        errorMessage = context.getString(
                            R.string.authenticated_user_fail,
                            authRepo.name
                        )
                    )
                }
            }.onFailure { throwable ->
                logError(throwable)
                val message = (throwable as? ErrorLoadingException)?.message
                    ?: throwable.message
                    ?: context.getString(R.string.authenticated_user_fail, authRepo.name)
                updateAccountSnapshot(
                    isWorking = false,
                    errorMessage = message
                )
            }
        }
    }

    fun logoutSelected(
        context: Context,
        onAccountChanged: () -> Unit = {}
    ) {
        val selectedUser = authRepo.authUser() ?: return
        if (_uiState.value.isWorking) return

        _uiState.value = _uiState.value.copy(
            isWorking = true,
            errorMessage = null
        )

        viewModelScope.launch {
            val result = runCatching {
                authRepo.logout(selectedUser)
            }

            result.onFailure { throwable ->
                logError(throwable)
                val message = (throwable as? ErrorLoadingException)?.message
                    ?: throwable.message
                    ?: context.getString(R.string.authenticated_user_fail, authRepo.name)
                showToast(message)
            }

            updateAccountSnapshot(
                isWorking = false,
                errorMessage = null,
                showLoginForm = authRepo.accounts.isEmpty(),
                password = ""
            )
            onAccountChanged()
        }
    }

    private fun updateAccountSnapshot(
        isWorking: Boolean = _uiState.value.isWorking,
        errorMessage: String? = _uiState.value.errorMessage,
        showLoginForm: Boolean? = null,
        password: String = _uiState.value.password
    ) {
        val currentState = _uiState.value
        val accounts = authRepo.accounts
            .mapIndexed { index, account ->
                OpenSubtitlesAccountItemUi(
                    id = account.user.id,
                    name = account.user.name,
                    index = index
                )
            }
            .toPersistentList()

        val resolvedShowLoginForm = showLoginForm
            ?: if (accounts.isEmpty()) {
                true
            } else {
                currentState.showLoginForm
            }

        _uiState.value = currentState.copy(
            accounts = accounts,
            selectedAccountId = authRepo.accountId,
            showLoginForm = resolvedShowLoginForm,
            isWorking = isWorking,
            errorMessage = errorMessage,
            password = password
        )
    }
}
