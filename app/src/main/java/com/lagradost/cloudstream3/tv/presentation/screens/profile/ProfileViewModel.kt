package com.lagradost.cloudstream3.tv.presentation.screens.profile

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.CloudStreamApp.Companion.removeKeys
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.utils.DataStoreHelper
import com.lagradost.cloudstream3.utils.DataStoreHelper.getAccounts
import com.lagradost.cloudstream3.utils.DataStoreHelper.getDefaultAccount
import com.lagradost.cloudstream3.utils.DataStoreHelper.setAccount
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal const val ProfileAddAccountFocusKey = "profile_add_account"

internal fun profileAccountFocusKey(keyIndex: Int): String {
    return "profile_account_$keyIndex"
}

@Immutable
data class ProfileAccountItemUiState(
    val keyIndex: Int,
    val name: String,
    val customImageUrl: String? = null,
    val defaultImageIndex: Int,
    val hasPin: Boolean,
    val isSelected: Boolean,
)

@Immutable
data class ProfileEditorUiState(
    val keyIndex: Int,
    val name: String,
    val customImageUrl: String,
    val defaultImageIndex: Int,
    val lockPin: String?,
    val isNew: Boolean,
    val canDelete: Boolean,
)

enum class ProfileMode {
    Select,
    Manage,
}

enum class ProfilePinPromptMode {
    UnlockSelection,
    SetPin,
    RemovePin,
}

@Immutable
data class ProfilePinPromptUiState(
    val mode: ProfilePinPromptMode,
    val accountKeyIndex: Int,
    val accountName: String,
    val input: String = "",
    val errorMessage: String? = null,
)

@Immutable
data class ProfileUiState(
    val mode: ProfileMode = ProfileMode.Select,
    val accounts: PersistentList<ProfileAccountItemUiState> = persistentListOf(),
    val editor: ProfileEditorUiState? = null,
    val pinPrompt: ProfilePinPromptUiState? = null,
    val pendingListFocusKey: String? = null,
    val listRestoreFocusToken: Int = 0,
    val editorRequestFocusToken: Int = 0,
)

private sealed interface ProfilePinRequest {
    data class Unlock(val account: DataStoreHelper.Account) : ProfilePinRequest
    data class Set(val accountKeyIndex: Int) : ProfilePinRequest
    data class Remove(val accountKeyIndex: Int, val currentPin: String) : ProfilePinRequest
}

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var activePinRequest: ProfilePinRequest? = null

    init {
        refresh()
    }

    fun refresh() {
        emitState()
    }

    fun openManageMode(accountKeyIndex: Int? = currentSelectedAccount()?.keyIndex) {
        emitState(
            mode = ProfileMode.Manage,
            editor = accountKeyIndex
                ?.let(::findAccountByKey)
                ?.toEditorUiState(isNew = false)
                ?: createNewAccountDraft(),
            pendingListFocusKey = accountKeyIndex?.let(::profileAccountFocusKey),
            listRestoreFocusToken = _uiState.value.listRestoreFocusToken + 1,
            editorRequestFocusToken = _uiState.value.editorRequestFocusToken + 1,
        )
    }

    fun exitManageMode() {
        emitState(
            mode = ProfileMode.Select,
            editor = null,
            pinPrompt = null,
            pendingListFocusKey = currentSelectedAccount()?.keyIndex?.let(::profileAccountFocusKey),
            listRestoreFocusToken = _uiState.value.listRestoreFocusToken + 1,
        )
    }

    fun selectAccount(accountKeyIndex: Int) {
        val account = findAccountByKey(accountKeyIndex) ?: return
        if (account.lockPin != null) {
            activePinRequest = ProfilePinRequest.Unlock(account)
            emitState(
                pinPrompt = ProfilePinPromptUiState(
                    mode = ProfilePinPromptMode.UnlockSelection,
                    accountKeyIndex = account.keyIndex,
                    accountName = account.name,
                ),
            )
            return
        }

        applyAccountSelection(account)
    }

    fun startNewAccountEditor() {
        emitState(
            mode = ProfileMode.Manage,
            editor = createNewAccountDraft(),
            pendingListFocusKey = ProfileAddAccountFocusKey,
            listRestoreFocusToken = _uiState.value.listRestoreFocusToken + 1,
            editorRequestFocusToken = _uiState.value.editorRequestFocusToken + 1,
        )
    }

    fun editAccount(accountKeyIndex: Int) {
        val account = findAccountByKey(accountKeyIndex) ?: return
        emitState(
            mode = ProfileMode.Manage,
            editor = account.toEditorUiState(isNew = false),
            pendingListFocusKey = profileAccountFocusKey(accountKeyIndex),
            listRestoreFocusToken = _uiState.value.listRestoreFocusToken + 1,
            editorRequestFocusToken = _uiState.value.editorRequestFocusToken + 1,
        )
    }

    fun updateEditorName(value: String) {
        val editor = _uiState.value.editor ?: return
        _uiState.value = _uiState.value.copy(
            editor = editor.copy(name = value)
        )
    }

    fun updateEditorCustomImageUrl(value: String) {
        val editor = _uiState.value.editor ?: return
        _uiState.value = _uiState.value.copy(
            editor = editor.copy(customImageUrl = value)
        )
    }

    fun cycleEditorBackground() {
        val editor = _uiState.value.editor ?: return
        val nextIndex = (editor.defaultImageIndex + 1) % DataStoreHelper.profileImages.size
        _uiState.value = _uiState.value.copy(
            editor = editor.copy(
                customImageUrl = "",
                defaultImageIndex = nextIndex,
            )
        )
    }

    fun saveEditor() {
        val context = CloudStreamApp.context ?: return
        val editor = _uiState.value.editor ?: return
        val normalizedName = editor.name.trim().ifBlank {
            when {
                editor.keyIndex == 0 -> context.getString(R.string.default_account)
                else -> context.getString(
                    R.string.login_format,
                    context.getString(R.string.account),
                    editor.keyIndex
                )
            }
        }

        val updatedAccount = DataStoreHelper.Account(
            keyIndex = editor.keyIndex,
            name = normalizedName,
            customImage = editor.customImageUrl.trim().ifBlank { null },
            defaultImageIndex = editor.defaultImageIndex,
            lockPin = editor.lockPin,
        )

        val currentAccounts = getAccounts(context).toMutableList()
        val overrideIndex = currentAccounts.indexOfFirst { account ->
            account.keyIndex == updatedAccount.keyIndex
        }
        if (overrideIndex >= 0) {
            currentAccounts[overrideIndex] = updatedAccount
        } else {
            currentAccounts.add(updatedAccount)
        }

        val currentHomePage = DataStoreHelper.currentHomePage
        setAccount(updatedAccount)
        DataStoreHelper.currentHomePage = currentHomePage
        DataStoreHelper.accounts = currentAccounts.toTypedArray()
        MainActivity.reloadHomeEvent(true)
        MainActivity.reloadAccountEvent(true)

        emitState(
            mode = ProfileMode.Manage,
            editor = updatedAccount.toEditorUiState(isNew = false),
            pendingListFocusKey = profileAccountFocusKey(updatedAccount.keyIndex),
            listRestoreFocusToken = _uiState.value.listRestoreFocusToken + 1,
        )
    }

    fun deleteEditor() {
        val context = CloudStreamApp.context ?: return
        val editor = _uiState.value.editor ?: return
        if (!editor.canDelete) return

        removeKeys(editor.keyIndex.toString())

        val currentAccounts = getAccounts(context).toMutableList().apply {
            removeIf { account -> account.keyIndex == editor.keyIndex }
        }
        DataStoreHelper.accounts = currentAccounts.toTypedArray()

        val wasSelected = editor.keyIndex == DataStoreHelper.selectedKeyIndex
        if (wasSelected) {
            setAccount(getDefaultAccount(context))
            MainActivity.reloadHomeEvent(true)
        }
        MainActivity.reloadAccountEvent(true)

        val fallbackAccount = getAccounts(context)
            .firstOrNull { account -> account.keyIndex != editor.keyIndex }
            ?: getDefaultAccount(context)

        emitState(
            mode = ProfileMode.Manage,
            editor = fallbackAccount.toEditorUiState(isNew = false),
            pendingListFocusKey = profileAccountFocusKey(fallbackAccount.keyIndex),
            listRestoreFocusToken = _uiState.value.listRestoreFocusToken + 1,
        )
    }

    fun openSetPinPrompt() {
        val editor = _uiState.value.editor ?: return
        activePinRequest = ProfilePinRequest.Set(editor.keyIndex)
        emitState(
            pinPrompt = ProfilePinPromptUiState(
                mode = ProfilePinPromptMode.SetPin,
                accountKeyIndex = editor.keyIndex,
                accountName = editor.name,
            )
        )
    }

    fun openRemovePinPrompt() {
        val editor = _uiState.value.editor ?: return
        val currentPin = editor.lockPin ?: return
        activePinRequest = ProfilePinRequest.Remove(
            accountKeyIndex = editor.keyIndex,
            currentPin = currentPin,
        )
        emitState(
            pinPrompt = ProfilePinPromptUiState(
                mode = ProfilePinPromptMode.RemovePin,
                accountKeyIndex = editor.keyIndex,
                accountName = editor.name,
            )
        )
    }

    fun dismissPinPrompt() {
        activePinRequest = null
        emitState(pinPrompt = null)
    }

    fun updatePinPromptInput(value: String) {
        val prompt = _uiState.value.pinPrompt ?: return
        _uiState.value = _uiState.value.copy(
            pinPrompt = prompt.copy(
                input = value.take(4),
                errorMessage = null,
            )
        )
    }

    fun submitPinPrompt() {
        val context = CloudStreamApp.context ?: return
        val prompt = _uiState.value.pinPrompt ?: return
        val request = activePinRequest ?: return
        val trimmedPin = prompt.input.trim()
        if (trimmedPin.length != 4) {
            _uiState.value = _uiState.value.copy(
                pinPrompt = prompt.copy(
                    errorMessage = context.getString(R.string.pin_error_length)
                )
            )
            return
        }

        when (request) {
            is ProfilePinRequest.Unlock -> {
                if (trimmedPin != request.account.lockPin) {
                    _uiState.value = _uiState.value.copy(
                        pinPrompt = prompt.copy(
                            input = "",
                            errorMessage = context.getString(R.string.pin_error_incorrect)
                        )
                    )
                    return
                }
                activePinRequest = null
                applyAccountSelection(request.account)
            }

            is ProfilePinRequest.Set -> {
                val editor = _uiState.value.editor ?: return
                activePinRequest = null
                emitState(
                    pinPrompt = null,
                    editor = editor.copy(lockPin = trimmedPin)
                )
            }

            is ProfilePinRequest.Remove -> {
                if (trimmedPin != request.currentPin) {
                    _uiState.value = _uiState.value.copy(
                        pinPrompt = prompt.copy(
                            input = "",
                            errorMessage = context.getString(R.string.pin_error_incorrect)
                        )
                    )
                    return
                }
                val editor = _uiState.value.editor ?: return
                activePinRequest = null
                emitState(
                    pinPrompt = null,
                    editor = editor.copy(lockPin = null)
                )
            }
        }
    }

    private fun applyAccountSelection(account: DataStoreHelper.Account) {
        setAccount(account)
        MainActivity.reloadHomeEvent(true)
        MainActivity.reloadAccountEvent(true)
        emitState(
            mode = ProfileMode.Select,
            editor = null,
            pinPrompt = null,
            pendingListFocusKey = profileAccountFocusKey(account.keyIndex),
            listRestoreFocusToken = _uiState.value.listRestoreFocusToken + 1,
        )
    }

    private fun emitState(
        mode: ProfileMode = _uiState.value.mode,
        editor: ProfileEditorUiState? = _uiState.value.editor,
        pinPrompt: ProfilePinPromptUiState? = _uiState.value.pinPrompt,
        pendingListFocusKey: String? = _uiState.value.pendingListFocusKey,
        listRestoreFocusToken: Int = _uiState.value.listRestoreFocusToken,
        editorRequestFocusToken: Int = _uiState.value.editorRequestFocusToken,
    ) {
        val accounts = buildAccountItems()
        val resolvedEditor = when {
            mode != ProfileMode.Manage -> null
            editor == null -> currentSelectedAccount()?.toEditorUiState(isNew = false)
            editor.isNew -> editor
            accounts.any { account -> account.keyIndex == editor.keyIndex } -> editor
            else -> currentSelectedAccount()?.toEditorUiState(isNew = false)
        }

        _uiState.value = ProfileUiState(
            mode = mode,
            accounts = accounts,
            editor = resolvedEditor,
            pinPrompt = pinPrompt,
            pendingListFocusKey = pendingListFocusKey,
            listRestoreFocusToken = listRestoreFocusToken,
            editorRequestFocusToken = editorRequestFocusToken,
        )
    }

    private fun buildAccountItems(): PersistentList<ProfileAccountItemUiState> {
        return currentAccounts()
            .map { account ->
                ProfileAccountItemUiState(
                    keyIndex = account.keyIndex,
                    name = account.name,
                    customImageUrl = account.customImage,
                    defaultImageIndex = account.defaultImageIndex,
                    hasPin = account.lockPin != null,
                    isSelected = account.keyIndex == DataStoreHelper.selectedKeyIndex,
                )
            }
            .toPersistentList()
    }

    private fun createNewAccountDraft(): ProfileEditorUiState {
        val context = CloudStreamApp.context ?: return ProfileEditorUiState(
            keyIndex = 0,
            name = "",
            customImageUrl = "",
            defaultImageIndex = 0,
            lockPin = null,
            isNew = true,
            canDelete = false,
        )
        val accounts = currentAccounts()
        val nextKeyIndex = (accounts.maxOfOrNull { account -> account.keyIndex } ?: 0) + 1
        val availableImages =
            DataStoreHelper.profileImages.toSet() - accounts.filter { it.customImage == null }
                .mapNotNull { account ->
                    DataStoreHelper.profileImages.getOrNull(account.defaultImageIndex)
                }.toSet()
        val defaultImageIndex = DataStoreHelper.profileImages.indexOf(
            availableImages.randomOrNull() ?: DataStoreHelper.profileImages.random()
        ).coerceAtLeast(0)
        return ProfileEditorUiState(
            keyIndex = nextKeyIndex,
            name = context.getString(
                R.string.login_format,
                context.getString(R.string.account),
                nextKeyIndex
            ),
            customImageUrl = "",
            defaultImageIndex = defaultImageIndex,
            lockPin = null,
            isNew = true,
            canDelete = false,
        )
    }

    private fun currentAccounts(): List<DataStoreHelper.Account> {
        val context = CloudStreamApp.context ?: return DataStoreHelper.accounts.toList()
        return getAccounts(context)
    }

    private fun currentSelectedAccount(): DataStoreHelper.Account? {
        return currentAccounts().firstOrNull { account ->
            account.keyIndex == DataStoreHelper.selectedKeyIndex
        }
    }

    private fun findAccountByKey(accountKeyIndex: Int): DataStoreHelper.Account? {
        return currentAccounts().firstOrNull { account ->
            account.keyIndex == accountKeyIndex
        }
    }
}

private fun DataStoreHelper.Account.toEditorUiState(
    isNew: Boolean,
): ProfileEditorUiState {
    return ProfileEditorUiState(
        keyIndex = keyIndex,
        name = name,
        customImageUrl = customImage.orEmpty(),
        defaultImageIndex = defaultImageIndex,
        lockPin = lockPin,
        isNew = isNew,
        canDelete = keyIndex != 0,
    )
}
