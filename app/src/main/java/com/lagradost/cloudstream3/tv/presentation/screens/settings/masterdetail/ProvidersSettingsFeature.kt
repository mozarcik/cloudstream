package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.AllLanguagesName
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.DubStatus
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.ui.APIRepository
import com.lagradost.cloudstream3.utils.AppContextUtils.getApiDubstatusSettings
import com.lagradost.cloudstream3.utils.AppContextUtils.getApiProviderLangSettings
import com.lagradost.cloudstream3.utils.DataStoreHelper
import com.lagradost.cloudstream3.utils.SubtitleHelper.getNameNextToFlagEmoji
import java.util.Locale

private object ProvidersSettingsScreenIds {
    const val ProvidersMain = "settings_providers"
    const val ProvidersLanguages = "settings_providers/languages"
    const val ProvidersMediaTypes = "settings_providers/media_types"
    const val ProvidersDisplaySubDub = "settings_providers/display_sub_dub"
}

private data class ProvidersOption(
    val key: String,
    val label: String
)

@Composable
fun rememberProvidersSettingsFeature(
    providersTitle: String
): ProvidersSettingsFeature {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context, providersTitle) {
        ProvidersSettingsFeature(
            context = context,
            providersTitle = providersTitle
        )
    }
}

class ProvidersSettingsFeature(
    private val context: Context,
    private val providersTitle: String
) {
    private val settingsManager by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    private val providerLangKey by lazy { context.getString(R.string.provider_lang_key) }
    private val preferMediaTypeKey by lazy { context.getString(R.string.prefer_media_type_key) }
    private val displaySubKey by lazy { context.getString(R.string.display_sub_key) }
    private val enableNsfwProvidersKey by lazy { context.getString(R.string.enable_nsfw_on_providers_key) }

    val staticScreens: List<SettingsScreen> = listOf(
        ProvidersMainScreen(),
        ProviderLanguagesScreen(),
        ProviderMediaTypesScreen(),
        ProviderDisplaySubDubScreen()
    )

    private inner class ProvidersMainScreen : SettingsScreen {
        override val id: String = ProvidersSettingsScreenIds.ProvidersMain
        override val title: String = providersTitle

        override suspend fun load(): List<SettingsEntry> {
            return buildList {
                add(
                    itemEntry(
                        stableKey = "providers_languages",
                        title = context.getString(R.string.provider_lang_settings),
                        subtitle = resolveProviderLanguageSummary(),
                        fallbackIconRes = R.drawable.ic_baseline_language_24,
                        nextScreenId = ProvidersSettingsScreenIds.ProvidersLanguages
                    )
                )
                add(
                    itemEntry(
                        stableKey = "providers_media_types",
                        title = context.getString(R.string.preferred_media_settings),
                        subtitle = resolvePreferredMediaSummary(),
                        fallbackIconRes = R.drawable.ic_baseline_play_arrow_24,
                        nextScreenId = ProvidersSettingsScreenIds.ProvidersMediaTypes
                    )
                )
                add(
                    itemEntry(
                        stableKey = "providers_display_sub_dub",
                        title = context.getString(R.string.display_subbed_dubbed_settings),
                        subtitle = resolveDubStatusSummary(),
                        fallbackIconRes = R.drawable.ic_outline_voice_over_off_24,
                        nextScreenId = ProvidersSettingsScreenIds.ProvidersDisplaySubDub
                    )
                )
                add(
                    toggleEntry(
                        stableKey = "providers_enable_nsfw",
                        title = context.getString(R.string.enable_nsfw_on_providers),
                        subtitle = context.getString(R.string.apply_on_restart),
                        fallbackIconRes = R.drawable.ic_baseline_extension_24,
                        value = settingsManager.getBoolean(enableNsfwProvidersKey, false),
                        onValueChanged = { enabled ->
                            settingsManager.edit {
                                putBoolean(enableNsfwProvidersKey, enabled)
                            }
                        }
                    )
                )
                add(
                    itemEntry(
                        stableKey = "providers_test_extensions",
                        title = context.getString(R.string.test_extensions),
                        subtitle = context.getString(R.string.test_extensions_summary),
                        fallbackIconRes = R.drawable.baseline_network_ping_24,
                        action = {
                            CommonActivity.showToast(R.string.coming_soon)
                        }
                    )
                )
            }
        }
    }

    private inner class ProviderLanguagesScreen : SettingsScreen {
        override val id: String = ProvidersSettingsScreenIds.ProvidersLanguages
        override val title: String = context.getString(R.string.provider_lang_settings)

        override suspend fun load(): List<SettingsEntry> {
            val current = context.getApiProviderLangSettings().toMutableSet()
            val options = resolveProviderLanguageOptions()
            return buildList {
                options.forEach { option ->
                    add(
                        toggleEntry(
                            stableKey = "providers_language_option_${option.key}",
                            title = option.label,
                            subtitle = null,
                            fallbackIconRes = R.drawable.ic_baseline_language_24,
                            value = current.contains(option.key),
                            onValueChanged = { enabled ->
                                val updated = context.getApiProviderLangSettings().toMutableSet()
                                if (enabled) {
                                    updated.add(option.key)
                                } else {
                                    updated.remove(option.key)
                                }
                                settingsManager.edit {
                                    putStringSet(providerLangKey, updated)
                                }
                            }
                        )
                    )
                }
            }
        }
    }

    private inner class ProviderMediaTypesScreen : SettingsScreen {
        override val id: String = ProvidersSettingsScreenIds.ProvidersMediaTypes
        override val title: String = context.getString(R.string.preferred_media_settings)

        override suspend fun load(): List<SettingsEntry> {
            val orderedTypes = TvType.entries.sorted()
            val defaultSet = orderedTypes
                .filter { type -> type != TvType.NSFW }
                .map { type -> type.ordinal.toString() }
                .toSet()
            val current = settingsManager.getStringSet(preferMediaTypeKey, defaultSet)
                ?.toMutableSet()
                ?: defaultSet.toMutableSet()

            return buildList {
                orderedTypes.forEach { tvType ->
                    val key = tvType.ordinal.toString()
                    add(
                        toggleEntry(
                            stableKey = "providers_media_type_${tvType.name}",
                            title = tvType.name,
                            subtitle = null,
                            fallbackIconRes = R.drawable.ic_baseline_play_arrow_24,
                            value = current.contains(key),
                            onValueChanged = { enabled ->
                                val updated = settingsManager.getStringSet(preferMediaTypeKey, defaultSet)
                                    ?.toMutableSet()
                                    ?: defaultSet.toMutableSet()
                                if (enabled) {
                                    updated.add(key)
                                } else {
                                    updated.remove(key)
                                }
                                settingsManager.edit {
                                    putStringSet(preferMediaTypeKey, updated)
                                }
                                DataStoreHelper.currentHomePage = null
                            }
                        )
                    )
                }
            }
        }
    }

    private inner class ProviderDisplaySubDubScreen : SettingsScreen {
        override val id: String = ProvidersSettingsScreenIds.ProvidersDisplaySubDub
        override val title: String = context.getString(R.string.display_subbed_dubbed_settings)

        override suspend fun load(): List<SettingsEntry> {
            val orderedStatuses = DubStatus.entries
            val current = context.getApiDubstatusSettings()

            return buildList {
                orderedStatuses.forEach { status ->
                    add(
                        toggleEntry(
                            stableKey = "providers_dub_status_${status.name}",
                            title = status.name,
                            subtitle = null,
                            fallbackIconRes = R.drawable.ic_outline_voice_over_off_24,
                            value = current.contains(status),
                            onValueChanged = { enabled ->
                                val updated = context.getApiDubstatusSettings().toMutableSet()
                                if (enabled) {
                                    updated.add(status)
                                } else {
                                    updated.remove(status)
                                }
                                val persisted = updated.map { item -> item.name }.toMutableSet()
                                settingsManager.edit {
                                    putStringSet(displaySubKey, persisted)
                                }
                                APIRepository.dubStatusActive = updated.toHashSet()
                            }
                        )
                    )
                }
            }
        }
    }

    private fun resolveProviderLanguageOptions(): List<ProvidersOption> {
        return synchronized(APIHolder.apis) {
            val languagePairs = APIHolder.apis
                .map { api -> api.lang to (getNameNextToFlagEmoji(api.lang) ?: api.lang) }
                .distinctBy { (langTag, _) -> langTag }
                .sortedBy { (_, label) -> label.substringAfter('\u00a0').lowercase(Locale.ROOT) }

            listOf(
                ProvidersOption(
                    key = AllLanguagesName,
                    label = context.getString(R.string.all_languages_preference)
                )
            ) + languagePairs.map { (langTag, label) ->
                ProvidersOption(key = langTag, label = label)
            }
        }
    }

    private fun resolveProviderLanguageSummary(): String {
        val selected = context.getApiProviderLangSettings()
        val options = resolveProviderLanguageOptions()
        if (selected.contains(AllLanguagesName)) {
            return context.getString(R.string.all_languages_preference)
        }
        val selectedLabels = options
            .filter { option -> selected.contains(option.key) }
            .map { option -> option.label }
        return if (selectedLabels.isEmpty()) {
            context.getString(R.string.none)
        } else {
            selectedLabels.joinToString(", ")
        }
    }

    private fun resolvePreferredMediaSummary(): String {
        val orderedTypes = TvType.entries.sorted()
        val defaultSet = orderedTypes
            .filter { type -> type != TvType.NSFW }
            .map { type -> type.ordinal.toString() }
            .toSet()
        val selected = settingsManager.getStringSet(preferMediaTypeKey, defaultSet)
            ?.toSet()
            ?: defaultSet
        val labels = orderedTypes
            .filter { type -> selected.contains(type.ordinal.toString()) }
            .map { type -> type.name }
        return if (labels.isEmpty()) {
            context.getString(R.string.none)
        } else {
            labels.joinToString(", ")
        }
    }

    private fun resolveDubStatusSummary(): String {
        val selected = context.getApiDubstatusSettings()
        val labels = DubStatus.entries
            .filter { status -> selected.contains(status) }
            .map { status -> status.name }
        return if (labels.isEmpty()) {
            context.getString(R.string.none)
        } else {
            labels.joinToString(", ")
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
