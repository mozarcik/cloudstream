package com.lagradost.cloudstream3.tv.presentation.screens.welcome

/** Screens of the first-run flow. [Language]..[Media] are the five numbered steps (1..5). */
enum class WelcomeStep(val number: Int) {
    Start(0),
    Language(1),
    Repository(2),
    Extensions(3),
    ExtensionLanguages(4),
    Media(5),
    Done(6),
}

/** Secondary screen shown inside a step; Back closes it before the step goes back. */
enum class WelcomeSub {
    None,
    RestoreInfo,
    RestoreDone,
    RestoreFailed,
    AddRepository,
    CommunityRepositories,
    DeleteRepository,
    RepositoryPlugins,
}

/** Sequential download of the selected extensions (the plugin manager reports no byte progress). */
data class WelcomeDownload(
    val total: Int,
    val finished: Int,
    val currentName: String?,
)

/** One immutable state of the whole flow; the checklist and footers are derived from [step] and [sub]. */
data class WelcomeUiState(
    val step: WelcomeStep = WelcomeStep.Start,
    val sub: WelcomeSub = WelcomeSub.None,
    /** Step 4 exists only when the installed extensions offer more than one language. */
    val hasExtensionLanguagesStep: Boolean = false,
    val openRepositoryUrl: String? = null,
    /** Last opened repository: the list focuses it again when it is shown. */
    val lastRepositoryUrl: String? = null,
    val selectedPlugins: Set<String> = emptySet(),
    val download: WelcomeDownload? = null,
    /** Set by choosing an app language: the language step then focuses Next (also after the recreate). */
    val focusPrimary: Boolean = false,
    /** File picked for the last restore (shown on the result screens). */
    val restoreFileName: String? = null,
    val languageTag: String = "",
    val providerLanguages: Set<String> = emptySet(),
    val mediaTypes: Set<String> = emptySet(),
)
