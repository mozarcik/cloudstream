package com.lagradost.cloudstream3.tv.compat

import com.lagradost.cloudstream3.AutoDownloadMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TvPluginBootstrapExecutorTest {
    @Test
    fun `execute uses update flow before auto download and local load`() = runBlocking {
        val steps = mutableListOf<String>()
        val executor = TvPluginBootstrapExecutor(
            updateOnlinePlugins = { steps += "update-online" },
            loadOnlinePlugins = { steps += "load-online" },
            downloadMissingPlugins = { mode -> steps += "download-${mode.name}" },
            loadLocalPlugins = { steps += "load-local" },
            onFailure = { throwable -> steps += "error:${throwable.message}" },
        )

        executor.execute(
            TvPluginBootstrapConfig(
                autoUpdatePlugins = true,
                autoDownloadMode = AutoDownloadMode.All,
            )
        )

        assertEquals(
            listOf("update-online", "download-All", "load-local"),
            steps,
        )
    }

    @Test
    fun `execute falls back to local plugins even when online step fails`() = runBlocking {
        val steps = mutableListOf<String>()
        val failure = IllegalStateException("network")
        val executor = TvPluginBootstrapExecutor(
            updateOnlinePlugins = { error("update should not run") },
            loadOnlinePlugins = { throw failure },
            downloadMissingPlugins = { mode -> steps += "download-${mode.name}" },
            loadLocalPlugins = { steps += "load-local" },
            onFailure = { throwable -> steps += "error:${throwable.message}" },
        )

        executor.execute(
            TvPluginBootstrapConfig(
                autoUpdatePlugins = false,
                autoDownloadMode = AutoDownloadMode.Disable,
            )
        )

        assertEquals(
            listOf("error:network", "load-local"),
            steps,
        )
    }
}
