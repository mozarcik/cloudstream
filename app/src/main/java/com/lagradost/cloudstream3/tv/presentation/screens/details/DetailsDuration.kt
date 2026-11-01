package com.lagradost.cloudstream3.tv.presentation.screens.details

/** Runtime in minutes from the mapper's "165min" text; null when it has no usable number. */
internal fun parseDetailsDurationMinutes(raw: String): Int? =
    raw.takeWhile { it.isDigit() }.toIntOrNull()?.takeIf { it > 0 }

/** "45 min" below an hour, "2h" for whole hours, otherwise "2h 45min". */
internal fun formatDetailsDuration(
    minutes: Int,
    minutesOnly: (Int) -> String,
    hoursOnly: (Int) -> String,
    hoursAndMinutes: (Int, Int) -> String,
): String = when {
    minutes < 60 -> minutesOnly(minutes)
    minutes % 60 == 0 -> hoursOnly(minutes / 60)
    else -> hoursAndMinutes(minutes / 60, minutes % 60)
}

/** Localised runtime of [details] ("2h 45min"), or the raw text when it has no usable number. */
@androidx.compose.runtime.Composable
internal fun rememberDetailsRuntimeText(details: com.lagradost.cloudstream3.tv.data.entities.MovieDetails): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    return parseDetailsDurationMinutes(details.duration)?.let { minutes ->
        formatDetailsDuration(
            minutes,
            minutesOnly = { context.getString(com.lagradost.cloudstream3.R.string.duration_format, it) },
            hoursOnly = { context.getString(com.lagradost.cloudstream3.R.string.tv_duration_hours_format, it) },
            hoursAndMinutes = { h, m -> context.getString(com.lagradost.cloudstream3.R.string.tv_episode_duration_hour_min_format, h, m) },
        )
    } ?: details.duration
}
