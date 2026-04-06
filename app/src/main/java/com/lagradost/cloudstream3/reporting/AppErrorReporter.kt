package com.lagradost.cloudstream3.reporting

import android.app.Application
import com.lagradost.cloudstream3.BuildConfig
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.R
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.android.core.SentryAndroid

private const val CommitHashFallback = "unknown"
private const val FlushTimeoutMs = 2_000L

data class HandledAppErrorContext(
    val screen: String,
    val uiMessage: String,
    val eventMessage: String,
    val providerName: String? = null,
    val itemTitle: String? = null,
    val mediaType: String? = null,
)

object AppErrorReporter {
    fun init(application: Application) {
        if (BuildConfig.SENTRY_DSN.isBlank()) return

        runCatching {
            SentryAndroid.init(application) { options ->
                options.setDsn(BuildConfig.SENTRY_DSN)
                options.setDebug(BuildConfig.DEBUG)
                options.setRelease(
                    "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
                )
                options.setEnvironment(
                    buildString {
                        append(BuildConfig.FLAVOR.ifBlank { "default" })
                        append('-')
                        append(if (BuildConfig.DEBUG) "debug" else "release")
                    }
                )
                options.setSendDefaultPii(false)
                options.setEnableAutoSessionTracking(false)
                options.setEnableActivityLifecycleBreadcrumbs(false)
                options.setEnableAppLifecycleBreadcrumbs(false)
                options.setEnableSystemEventBreadcrumbs(false)
                options.setEnableNetworkEventBreadcrumbs(false)
                options.setEnableUserInteractionBreadcrumbs(false)
                options.setAttachScreenshot(false)
                options.setAttachViewHierarchy(false)
            }
        }
    }

    fun reportHandled(
        context: HandledAppErrorContext,
        throwable: Throwable? = null,
    ) {
        if (BuildConfig.SENTRY_DSN.isBlank()) return

        runCatching {
            Sentry.withScope { scope ->
                scope.level = SentryLevel.ERROR
                scope.setTag("screen", context.screen)
                scope.setTag("build_type", if (BuildConfig.DEBUG) "debug" else "release")
                scope.setTag("flavor", BuildConfig.FLAVOR.ifBlank { "default" })
                scope.setTag("commit_hash", resolveCommitHash())
                context.providerName?.takeIf { it.isNotBlank() }?.let { provider ->
                    scope.setTag("provider", provider)
                }
                context.mediaType?.takeIf { it.isNotBlank() }?.let { mediaType ->
                    scope.setTag("media_type", mediaType)
                }
                context.itemTitle?.takeIf { it.isNotBlank() }?.let { title ->
                    scope.setExtra("item_title", title)
                }
                scope.setExtra("ui_message", context.uiMessage)

                if (throwable != null) {
                    Sentry.captureException(throwable)
                } else {
                    Sentry.captureMessage(context.eventMessage, SentryLevel.ERROR)
                }
            }
        }
    }

    fun reportFatal(
        throwable: Throwable,
        loadingExtension: String?,
    ) {
        if (BuildConfig.SENTRY_DSN.isBlank()) return

        runCatching {
            Sentry.withScope { scope ->
                scope.level = SentryLevel.FATAL
                scope.setTag("screen", "app_uncaught")
                scope.setTag("build_type", if (BuildConfig.DEBUG) "debug" else "release")
                scope.setTag("flavor", BuildConfig.FLAVOR.ifBlank { "default" })
                scope.setTag("commit_hash", resolveCommitHash())
                loadingExtension?.takeIf { it.isNotBlank() }?.let { extension ->
                    scope.setExtra("loading_extension", extension)
                }
                Sentry.captureException(throwable)
            }
            Sentry.flush(FlushTimeoutMs)
        }
    }

    private fun resolveCommitHash(): String {
        return CloudStreamApp.context
            ?.getString(R.string.commit_hash)
            ?.takeIf { it.isNotBlank() }
            ?: CommitHashFallback
    }
}
