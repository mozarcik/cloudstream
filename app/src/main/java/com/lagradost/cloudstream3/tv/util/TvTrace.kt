package com.lagradost.cloudstream3.tv.util

import android.os.Build
import android.os.Trace

internal inline fun <T> tvTraceSection(
    sectionName: String,
    block: () -> T,
): T {
    Trace.beginSection(sectionName)
    return try {
        block()
    } finally {
        Trace.endSection()
    }
}

internal suspend inline fun <T> tvTraceAsyncSection(
    sectionName: String,
    cookie: Int,
    crossinline block: suspend () -> T,
): T {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Trace.beginAsyncSection(sectionName, cookie)
    }
    return try {
        block()
    } finally {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Trace.endAsyncSection(sectionName, cookie)
        }
    }
}
