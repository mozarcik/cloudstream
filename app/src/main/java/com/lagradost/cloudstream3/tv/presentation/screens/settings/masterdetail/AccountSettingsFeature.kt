package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getActivity
import com.lagradost.cloudstream3.CloudStreamApp.Companion.openBrowser
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.syncproviders.AccountManager.Companion.aniListApi
import com.lagradost.cloudstream3.syncproviders.AccountManager.Companion.kitsuApi
import com.lagradost.cloudstream3.syncproviders.AccountManager.Companion.malApi
import com.lagradost.cloudstream3.syncproviders.AccountManager.Companion.openSubtitlesApi
import com.lagradost.cloudstream3.syncproviders.AccountManager.Companion.simklApi
import com.lagradost.cloudstream3.syncproviders.AccountManager.Companion.subDlApi
import com.lagradost.cloudstream3.syncproviders.AuthRepo
import com.lagradost.cloudstream3.syncproviders.SubtitleRepo
import com.lagradost.cloudstream3.syncproviders.SyncRepo
import com.lagradost.cloudstream3.ui.settings.SettingsAccount
import com.lagradost.cloudstream3.tv.presentation.screens.settings.account.OpenSubtitlesAccountScreen
import com.lagradost.cloudstream3.tv.presentation.screens.settings.account.OpenSubtitlesAccountViewModel
import com.lagradost.cloudstream3.utils.BackupUtils
import com.lagradost.cloudstream3.utils.BiometricAuthenticator.BiometricCallback
import com.lagradost.cloudstream3.utils.BiometricAuthenticator.authCallback
import com.lagradost.cloudstream3.utils.BiometricAuthenticator.deviceHasPasswordPinLock
import com.lagradost.cloudstream3.utils.BiometricAuthenticator.isAuthEnabled
import com.lagradost.cloudstream3.utils.BiometricAuthenticator.startBiometricAuthentication

private object AccountSettingsScreenIds {
    const val Prefix = "settings_account"
    const val AccountMain = "settings_account"
    const val OpenSubtitles = "$Prefix/opensubtitles"
}

private data class AccountProvider(
    val stableId: String,
    val authRepo: AuthRepo
)

@Composable
fun rememberAccountSettingsFeature(
    accountTitle: String
): AccountSettingsFeature {
    val context = androidx.compose.ui.platform.LocalContext.current
    val openSubtitlesRepo = remember { SubtitleRepo(openSubtitlesApi) }
    val openSubtitlesViewModel = remember {
        OpenSubtitlesAccountViewModel(openSubtitlesRepo)
    }
    return remember(context, accountTitle, openSubtitlesRepo, openSubtitlesViewModel) {
        AccountSettingsFeature(
            context = context,
            accountTitle = accountTitle,
            openSubtitlesRepo = openSubtitlesRepo,
            openSubtitlesViewModel = openSubtitlesViewModel
        )
    }
}

class AccountSettingsFeature(
    private val context: Context,
    private val accountTitle: String,
    private val openSubtitlesRepo: AuthRepo,
    private val openSubtitlesViewModel: OpenSubtitlesAccountViewModel
) {
    private val settingsManager by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    private val skipStartupAccountSelectKey by lazy {
        context.getString(R.string.skip_startup_account_select_key)
    }
    private val biometricKey by lazy {
        context.getString(R.string.biometric_key)
    }

    val staticScreens: List<SettingsScreen> = listOf(
        AccountMainScreen(),
        OpenSubtitlesScreen()
    )

    private inner class AccountMainScreen : SettingsScreen {
        override val id: String = AccountSettingsScreenIds.AccountMain
        override val title: String = accountTitle

        override suspend fun load(): List<SettingsEntry> {
            val providers = accountProviders()
            return buildList {
                add(
                    headerEntry(
                        stableKey = "account_header_accounts",
                        title = context.getString(R.string.pref_category_accounts)
                    )
                )
                providers.forEach { provider ->
                    add(
                        itemEntry(
                            stableKey = "account_provider_${provider.stableId}",
                            title = provider.authRepo.name,
                            subtitle = providerSubtitle(provider),
                            fallbackIconRes = provider.authRepo.icon
                                ?: R.drawable.ic_outline_account_circle_24,
                            nextScreenId = providerScreenId(provider),
                            action = providerAction(provider)
                        )
                    )
                }
                add(
                    toggleEntry(
                        stableKey = "account_skip_startup_selector",
                        title = context.getString(R.string.skip_startup_account_select_pref),
                        subtitle = null,
                        fallbackIconRes = R.drawable.ic_outline_account_circle_24,
                        value = settingsManager.getBoolean(skipStartupAccountSelectKey, false),
                        onValueChanged = { enabled ->
                            settingsManager.edit {
                                putBoolean(skipStartupAccountSelectKey, enabled)
                            }
                        }
                    )
                )

                add(
                    headerEntry(
                        stableKey = "account_header_security",
                        title = context.getString(R.string.pref_category_security)
                    )
                )
                add(
                    itemEntry(
                        stableKey = "account_biometric",
                        title = context.getString(R.string.biometric_setting),
                        subtitle = context.getString(R.string.biometric_setting_summary),
                        fallbackIconRes = R.drawable.ic_fingerprint,
                        action = {
                            handleBiometricAction()
                        }
                    )
                )
            }
        }
    }

    private fun accountProviders(): List<AccountProvider> {
        return listOf(
            AccountProvider(
                stableId = "mal",
                authRepo = SyncRepo(malApi)
            ),
            AccountProvider(
                stableId = "kitsu",
                authRepo = SyncRepo(kitsuApi)
            ),
            AccountProvider(
                stableId = "anilist",
                authRepo = SyncRepo(aniListApi)
            ),
            AccountProvider(
                stableId = "simkl",
                authRepo = SyncRepo(simklApi)
            ),
            AccountProvider(
                stableId = "opensubtitles",
                authRepo = openSubtitlesRepo
            ),
            AccountProvider(
                stableId = "subdl",
                authRepo = SubtitleRepo(subDlApi)
            )
        )
    }

    private fun providerSubtitle(provider: AccountProvider): String? {
        if (provider.stableId != "opensubtitles") return null
        return selectedAccountLabel(provider.authRepo)
            ?.let { accountName ->
                context.getString(R.string.logged_account, accountName)
            }
            ?: context.getString(R.string.no_account)
    }

    private fun providerScreenId(provider: AccountProvider): String? {
        return if (provider.stableId == "opensubtitles") {
            AccountSettingsScreenIds.OpenSubtitles
        } else {
            null
        }
    }

    private fun providerAction(provider: AccountProvider): (() -> Unit)? {
        return if (provider.stableId == "opensubtitles") {
            null
        } else {
            { openAccountManager(provider.authRepo) }
        }
    }

    private fun selectedAccountLabel(authRepo: AuthRepo): String? {
        val selectedAccountId = authRepo.accountId
        val selectedIndex = authRepo.accounts.indexOfFirst { account ->
            account.user.id == selectedAccountId
        }
        if (selectedIndex < 0) return null

        val account = authRepo.accounts[selectedIndex]
        return account.user.name?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.login_format, context.getString(R.string.account), selectedIndex + 1)
    }

    private fun openAccountManager(authRepo: AuthRepo) {
        val activity = context.getActivity() as? FragmentActivity ?: return
        val info = authRepo.authUser()
        val index = authRepo.accounts.indexOfFirst { account -> account.user.id == info?.id }
        if (authRepo.accounts.isNotEmpty()) {
            SettingsAccount.showLoginInfo(activity, authRepo, info, index)
        } else {
            SettingsAccount.addAccount(activity, authRepo)
        }
    }

    private fun handleBiometricAction() {
        val activity = context.getActivity() as? FragmentActivity ?: return
        if (isAuthEnabled(context)) {
            updateBiometricPreference(false)
            return
        }
        if (!deviceHasPasswordPinLock(context)) {
            showToast(R.string.biometric_unsupported)
            updateBiometricPreference(false)
            return
        }

        val callback = object : BiometricCallback {
            override fun onAuthenticationSuccess() {
                updateBiometricPreference(true)
                BackupUtils.backup(activity)
                authCallback = null
            }

            override fun onAuthenticationError() {
                updateBiometricPreference(false)
                authCallback = null
            }
        }

        startBiometricAuthentication(
            activity = activity,
            title = R.string.biometric_authentication_title,
            setDeviceCred = false
        )
        authCallback = callback
    }

    private fun updateBiometricPreference(enabled: Boolean) {
        settingsManager.edit {
            putBoolean(biometricKey, enabled)
        }
    }

    private inner class OpenSubtitlesScreen : SettingsScreen {
        override val id: String = AccountSettingsScreenIds.OpenSubtitles
        override val title: String = openSubtitlesRepo.name
        override val hasCustomContent: Boolean = true

        @Composable
        override fun Content(
            modifier: Modifier,
            contentPadding: PaddingValues,
            isPreview: Boolean,
            onBack: () -> Unit,
            onDataChanged: (String) -> Unit
        ) {
            val activity = context.getActivity() as? FragmentActivity
            OpenSubtitlesAccountScreen(
                stateFlow = openSubtitlesViewModel.uiState,
                viewModel = openSubtitlesViewModel,
                providerName = openSubtitlesRepo.name,
                createAccountUrl = openSubtitlesRepo.createAccountUrl,
                isPreview = isPreview,
                onAccountChanged = {
                    onDataChanged(AccountSettingsScreenIds.Prefix)
                },
                onOpenCreateAccount = { url ->
                    openBrowser(url, activity)
                },
                modifier = modifier
                    .padding(contentPadding)
            )
        }
    }
}

private fun itemEntry(
    stableKey: String,
    title: String,
    subtitle: String? = null,
    fallbackIconRes: Int? = null,
    showCheckmark: Boolean = false,
    nextScreenId: String? = null,
    action: (() -> Unit)? = null
): SettingsEntry {
    return SettingsEntry(
        title = title,
        subtitle = subtitle,
        fallbackIconRes = fallbackIconRes,
        showCheckmark = showCheckmark,
        stableKey = stableKey,
        nextScreenId = nextScreenId,
        action = action
    )
}

private fun headerEntry(
    stableKey: String,
    title: String
): SettingsEntry {
    return SettingsEntry(
        title = title,
        stableKey = stableKey,
        type = SettingsEntryType.Header
    )
}

private fun toggleEntry(
    stableKey: String,
    title: String,
    subtitle: String?,
    fallbackIconRes: Int? = null,
    value: Boolean,
    onValueChanged: (Boolean) -> Unit
): SettingsEntry {
    return SettingsEntry(
        title = title,
        subtitle = subtitle,
        fallbackIconRes = fallbackIconRes,
        stableKey = stableKey,
        type = SettingsEntryType.Toggle,
        toggleValue = value,
        onToggleChanged = onValueChanged
    )
}
