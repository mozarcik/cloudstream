package com.lagradost.cloudstream3.syncproviders

import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.subtitles.AbstractSubtitleEntities
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleRepoTest {
    @Test
    fun `resource does not cache empty subtitle payloads`() {
        runBlocking {
            val prefix = "subtitle_repo_cache_${System.nanoTime()}"
            val api = FakeCachingSubtitleApi(prefix)
            val repo = SubtitleRepo(api)
            val subtitle = AbstractSubtitleEntities.SubtitleEntity(
                idPrefix = prefix,
                name = "Test subtitle",
                lang = "en",
                data = "subtitle_${System.nanoTime()}",
                type = TvType.Movie,
                source = "Fake subtitle provider",
            )

            val beforeLogin = repo.resource(subtitle).getOrThrow()
            assertTrue(beforeLogin.getSubtitles().isEmpty())
            assertEquals(1, api.loadRequests.get())

            val loginSucceeded = repo.login(
                AuthLoginResponse(
                    password = "secret",
                    username = "codex",
                    email = null,
                    server = null,
                ),
            )
            assertTrue(loginSucceeded)

            val afterLogin = repo.resource(subtitle).getOrThrow()
            assertEquals(2, api.loadRequests.get())
            assertEquals(1, afterLogin.getSubtitles().size)

            AccountManager.cachedAccounts.remove(prefix)
            AccountManager.cachedAccountIds.remove(prefix)
        }
    }
}

private class FakeCachingSubtitleApi(
    private val prefix: String,
) : SubtitleAPI() {
    val loadRequests = AtomicInteger(0)

    override val name: String = "Fake subtitle provider"
    override val idPrefix: String = prefix

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
        return AuthToken(
            accessToken = "token_${password.hashCode()}",
            payload = username,
        )
    }

    override suspend fun search(
        auth: AuthData?,
        query: AbstractSubtitleEntities.SubtitleSearch,
    ): List<AbstractSubtitleEntities.SubtitleEntity> = emptyList()

    override suspend fun load(
        auth: AuthData?,
        subtitle: AbstractSubtitleEntities.SubtitleEntity,
    ): String? {
        loadRequests.incrementAndGet()
        if (auth == null) return null
        return "https://example.com/${subtitle.data}.srt"
    }
}
