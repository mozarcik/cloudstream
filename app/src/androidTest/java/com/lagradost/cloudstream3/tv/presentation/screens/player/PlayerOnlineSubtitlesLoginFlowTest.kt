package com.lagradost.cloudstream3.tv.presentation.screens.player

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.subtitles.AbstractSubtitleEntities
import com.lagradost.cloudstream3.syncproviders.AccountManager
import com.lagradost.cloudstream3.syncproviders.AuthData
import com.lagradost.cloudstream3.syncproviders.AuthLoginRequirement
import com.lagradost.cloudstream3.syncproviders.AuthLoginResponse
import com.lagradost.cloudstream3.syncproviders.AuthToken
import com.lagradost.cloudstream3.syncproviders.AuthUser
import com.lagradost.cloudstream3.syncproviders.SubtitleAPI
import com.lagradost.cloudstream3.syncproviders.SubtitleRepo
import com.lagradost.cloudstream3.tv.presentation.common.MenuListSidePanel
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelContentNavigationDirection
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.PlayerOnlineSubtitlesController
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPanelItemAction
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerSubtitlePanelNavigationDirection
import com.lagradost.cloudstream3.tv.presentation.screens.settings.account.OpenSubtitlesAccountScreen
import com.lagradost.cloudstream3.tv.presentation.screens.settings.account.OpenSubtitlesAccountViewModel
import com.lagradost.cloudstream3.tv.presentation.screens.settings.account.ProviderAccountLoginButtonTag
import com.lagradost.cloudstream3.tv.presentation.screens.settings.account.ProviderAccountPasswordFieldTag
import com.lagradost.cloudstream3.tv.presentation.screens.settings.account.ProviderAccountUsernameFieldTag
import com.lagradost.cloudstream3.ui.player.SubtitleData
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerOnlineSubtitlesLoginFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val fakeApi = FakeOpenSubtitlesApi()
    private val fakeRepo = SubtitleRepo(fakeApi)
    private lateinit var originalSubtitleProviders: Array<SubtitleRepo>
    private var hadOriginalAccounts = false
    private var originalAccounts: Array<AuthData>? = null
    private var hadOriginalAccountId = false
    private var originalAccountId: Int? = null

    @Before
    fun setUp() {
        originalSubtitleProviders = AccountManager.subtitleProviders.copyOf()
        hadOriginalAccounts = AccountManager.cachedAccounts.containsKey(FakeOpenSubtitlesApi.IdPrefix)
        originalAccounts = AccountManager.cachedAccounts[FakeOpenSubtitlesApi.IdPrefix]?.copyOf()
        hadOriginalAccountId = AccountManager.cachedAccountIds.containsKey(FakeOpenSubtitlesApi.IdPrefix)
        originalAccountId = AccountManager.cachedAccountIds[FakeOpenSubtitlesApi.IdPrefix]

        AccountManager.subtitleProviders.indices.forEach { index ->
            AccountManager.subtitleProviders[index] = fakeRepo
        }
        AccountManager.cachedAccounts[FakeOpenSubtitlesApi.IdPrefix] = emptyArray()
        AccountManager.cachedAccountIds[FakeOpenSubtitlesApi.IdPrefix] = AccountManager.NONE_ID
    }

    @After
    fun tearDown() {
        originalSubtitleProviders.indices.forEach { index ->
            AccountManager.subtitleProviders[index] = originalSubtitleProviders[index]
        }

        if (hadOriginalAccounts) {
            AccountManager.cachedAccounts[FakeOpenSubtitlesApi.IdPrefix] = originalAccounts ?: emptyArray()
        } else {
            AccountManager.cachedAccounts.remove(FakeOpenSubtitlesApi.IdPrefix)
        }

        if (hadOriginalAccountId) {
            AccountManager.cachedAccountIds[FakeOpenSubtitlesApi.IdPrefix] =
                originalAccountId ?: AccountManager.NONE_ID
        } else {
            AccountManager.cachedAccountIds.remove(FakeOpenSubtitlesApi.IdPrefix)
        }
    }

    @Test
    fun loginAfterLeavingPlayer_allowsSubtitleDownload_withoutRestart_andShowsMissingLoginMessage() {
        val query = "SubtitleLoginFlow_${System.nanoTime()}"
        val firstResultTag = FakeOpenSubtitlesApi.resultTagFor(query = query, index = 0)
        val expectedLoginRequiredMessage = composeRule.activity.getString(
            R.string.tv_player_online_subtitles_login_required,
        )
        val debugTag = "player_online_subtitles_login_flow_debug"
        val harnessScreenState = mutableStateOf(HarnessScreen.Player)
        val playerSessionIdState = mutableIntStateOf(0)
        val accountViewModel = OpenSubtitlesAccountViewModel(fakeRepo)

        composeRule.setContent {
            MaterialTheme {
                PlayerOnlineSubtitlesLoginFlowHarness(
                    repo = fakeRepo,
                    accountViewModel = accountViewModel,
                    query = query,
                    debugTag = debugTag,
                    firstResultTag = firstResultTag,
                    loadRequestCount = { fakeApi.loadRequests.get() },
                    screenState = harnessScreenState,
                    playerSessionIdState = playerSessionIdState,
                )
            }
        }

        waitUntilNodeExists(firstResultTag)
        composeRule.onNodeWithTag(SelectFirstResultButtonTag)
            .performSemanticsAction(SemanticsActions.OnClick)

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.textValueForNode(debugTag).contains("error=$expectedLoginRequiredMessage")
        }
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText(expectedLoginRequiredMessage)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.textValueForNode(debugTag).contains("downloaded=0")
        }
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.textValueForNode(debugTag).contains("loadRequests=0")
        }

        composeRule.runOnIdle {
            harnessScreenState.value = HarnessScreen.Settings
        }
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.textValueForNode(debugTag).contains("screen=Settings")
        }
        waitUntilNodeExists(ProviderAccountUsernameFieldTag, useUnmergedTree = true)
        waitUntilNodeExists(ProviderAccountPasswordFieldTag, useUnmergedTree = true)
        composeRule.runOnIdle {
            accountViewModel.updateUsername("codex_user")
            accountViewModel.updatePassword("codex_pass")
            accountViewModel.login(context = composeRule.activity)
        }

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.textValueForNode(debugTag).contains("accounts=1")
        }

        composeRule.runOnIdle {
            playerSessionIdState.intValue += 1
            harnessScreenState.value = HarnessScreen.Player
        }
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.textValueForNode(debugTag).contains("screen=Player")
        }
        waitUntilNodeExists(firstResultTag)
        composeRule.onNodeWithTag(SelectFirstResultButtonTag)
            .performSemanticsAction(SemanticsActions.OnClick)

        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.textValueForNode(debugTag).contains("downloaded=1")
        }
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.textValueForNode(debugTag).contains("loadRequests=1")
        }
        composeRule.runOnIdle {
            assertEquals(1, fakeApi.loadRequests.get())
        }
    }

    private fun waitUntilNodeExists(
        tag: String,
        timeoutMillis: Long = 5_000L,
        useUnmergedTree: Boolean = false,
    ) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = useUnmergedTree)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }
}

@Composable
private fun PlayerOnlineSubtitlesLoginFlowHarness(
    repo: SubtitleRepo,
    accountViewModel: OpenSubtitlesAccountViewModel,
    query: String,
    debugTag: String,
    firstResultTag: String,
    loadRequestCount: () -> Int,
    screenState: MutableState<HarnessScreen>,
    playerSessionIdState: MutableIntState,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val accountState by accountViewModel.uiState.collectAsState()
    val screen by screenState
    val playerSessionId = playerSessionIdState.intValue
    var refreshVersion by remember { mutableIntStateOf(0) }
    var downloadedSubtitles by remember { mutableStateOf<List<SubtitleData>>(emptyList()) }

    val controller = remember(playerSessionId, repo, coroutineScope, context, query) {
        PlayerOnlineSubtitlesController(
            coroutineScope = coroutineScope,
            stringResolver = { resId, fallback ->
                runCatching { context.getString(resId) }.getOrElse { fallback }
            },
            defaultQueryProvider = { query },
            createSearchRequest = { searchQuery, languageTag ->
                AbstractSubtitleEntities.SubtitleSearch(
                    query = searchQuery,
                    lang = languageTag,
                )
            },
            onVisibleUiRefreshRequested = {
                refreshVersion += 1
            },
            onSubtitlesDownloaded = { subtitles ->
                downloadedSubtitles = subtitles
                refreshVersion += 1
            },
        )
    }

    LaunchedEffect(playerSessionId, controller) {
        controller.openOnlineSubtitlesPanel()
        refreshVersion += 1
    }

    val panelContent = remember(playerSessionId, refreshVersion) {
        controller.buildPanelContent()
    }
    val debugLine = "screen=${screen.name} accounts=${accountState.accounts.size} " +
        "showLoginForm=${accountState.showLoginForm} downloaded=${downloadedSubtitles.size} " +
        "loadRequests=${loadRequestCount()} status=${controller.state.status} " +
        "error=${controller.state.errorMessage ?: "<none>"}"

    LaunchedEffect(debugLine) {
        Log.d(TestLogTag, debugLine)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (screen) {
            HarnessScreen.Player -> {
                Button(
                    onClick = { screenState.value = HarnessScreen.Settings },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(OpenSettingsButtonTag),
                ) {
                    Text(text = "Open settings")
                }

                Button(
                    onClick = {
                        Log.d(TestLogTag, "select first result button clicked resultId=$firstResultTag")
                        controller.selectOnlineSubtitleResult(firstResultTag)
                        refreshVersion += 1
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(SelectFirstResultButtonTag),
                ) {
                    Text(text = "Select first result")
                }

                MenuListSidePanel(
                    visible = true,
                    onCloseRequested = {},
                    title = context.getString(R.string.player_load_subtitles_online),
                    items = panelContent.items,
                    panelTestTag = PlayerPanelTag,
                    initialFocusedItemId = panelContent.initialFocusedItemId,
                    closeOnLeftPress = false,
                    closeOnFocusExit = false,
                    enableContentAnimation = false,
                    enableItemAnimations = false,
                    contentNavigationDirection = when (panelContent.direction) {
                        TvPlayerSubtitlePanelNavigationDirection.Forward -> SidePanelContentNavigationDirection.Forward
                        TvPlayerSubtitlePanelNavigationDirection.Backward -> SidePanelContentNavigationDirection.Backward
                    },
                    onActionTokenClick = { token ->
                        when (val action = token as? TvPlayerPanelItemAction) {
                            is TvPlayerPanelItemAction.SelectOnlineSubtitleResult -> {
                                controller.selectOnlineSubtitleResult(action.resultId)
                            }

                            TvPlayerPanelItemAction.RetryOnlineSubtitlesSearch -> {
                                controller.retrySearch()
                            }

                            else -> Unit
                        }
                        refreshVersion += 1
                    },
                )
            }

            HarnessScreen.Settings -> {
                Button(
                    onClick = {
                        playerSessionIdState.intValue += 1
                        screenState.value = HarnessScreen.Player
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(BackToPlayerButtonTag),
                ) {
                    Text(text = "Back to player")
                }

                OpenSubtitlesAccountScreen(
                    stateFlow = accountViewModel.uiState,
                    viewModel = accountViewModel,
                    providerName = FakeOpenSubtitlesApi.ProviderName,
                    createAccountUrl = null,
                    isPreview = false,
                    onBack = {},
                    onAccountChanged = {
                        refreshVersion += 1
                    },
                    onOpenCreateAccount = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Text(
            text = debugLine,
            modifier = Modifier.testTag(debugTag),
        )
    }
}

private enum class HarnessScreen {
    Player,
    Settings,
}

private class FakeOpenSubtitlesApi : SubtitleAPI() {
    val loadRequests = AtomicInteger(0)

    override val name: String = ProviderName
    override val idPrefix: String = IdPrefix
    override val hasInApp: Boolean = true
    override val inAppLoginRequirement: AuthLoginRequirement =
        AuthLoginRequirement(username = true, password = true)

    override suspend fun user(token: AuthToken?): AuthUser? {
        val username = token?.payload ?: return null
        return AuthUser(
            id = username.hashCode(),
            name = username,
        )
    }

    override suspend fun login(form: AuthLoginResponse): AuthToken? {
        val username = form.username?.takeIf { it.isNotBlank() } ?: return null
        val password = form.password?.takeIf { it.isNotBlank() } ?: return null
        Log.d(TestLogTag, "fake login username=$username")
        return AuthToken(
            accessToken = "token_${username}_${password.hashCode()}",
            payload = username,
        )
    }

    override suspend fun search(
        auth: AuthData?,
        query: AbstractSubtitleEntities.SubtitleSearch,
    ): List<AbstractSubtitleEntities.SubtitleEntity> {
        val searchQuery = query.query.trim()
        Log.d(TestLogTag, "fake search query=$searchQuery auth=${auth != null}")
        return listOf(
            AbstractSubtitleEntities.SubtitleEntity(
                idPrefix = idPrefix,
                name = subtitleTitle(searchQuery),
                lang = "en",
                data = subtitleData(searchQuery),
                type = TvType.Movie,
                source = name,
            ),
        )
    }

    override suspend fun load(
        auth: AuthData?,
        subtitle: AbstractSubtitleEntities.SubtitleEntity,
    ): String? {
        loadRequests.incrementAndGet()
        Log.d(TestLogTag, "fake load subtitle=${subtitle.data} auth=${auth != null}")
        if (auth == null) return null
        return "https://example.com/${subtitle.data}.srt"
    }

    companion object {
        const val IdPrefix = "opensubtitles"
        const val ProviderName = "OpenSubtitles"

        fun subtitleTitle(query: String): String = "Subtitle $query"

        fun subtitleData(query: String): String = "file_${query.hashCode()}"

        fun resultTagFor(query: String, index: Int): String {
            val idSuffix = buildString {
                append(IdPrefix)
                append('_')
                append(subtitleData(query))
                append('_')
                append(subtitleTitle(query))
                append('_')
                append("en")
                append('_')
                append(ProviderName)
                append('_')
                append(index)
            }
            return "subtitle_online_result_${idSuffix.hashCode()}"
        }
    }
}

private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.textValueForNode(tag: String): String {
    return onNodeWithTag(tag)
        .fetchSemanticsNode()
        .config[SemanticsProperties.Text]
        .joinToString(separator = "") { it.text }
}

private const val OpenSettingsButtonTag = "player_online_subtitles_open_settings"
private const val BackToPlayerButtonTag = "player_online_subtitles_back_to_player"
private const val PlayerPanelTag = "player_online_subtitles_panel"
private const val SelectFirstResultButtonTag = "player_online_subtitles_select_first_result"
private const val TestLogTag = "PlayerOnlineSubsTest"
