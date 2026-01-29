/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lagradost.cloudstream3.tv.presentation.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.lagradost.cloudstream3.R

enum class Screens(
    private val args: List<String>? = null,
    val isTabItem: Boolean = false,
    val tabIcon: ImageVector? = null,
    val stringRes: Int? = null
) {
    Profile,
    Sources(isTabItem = true, stringRes = R.string.sources),
    Home(isTabItem = true, stringRes = R.string.title_home),
    Library(isTabItem = true, stringRes = R.string.library),
    Downloads(isTabItem = true, stringRes = R.string.title_downloads),
    Settings(isTabItem = true, stringRes = R.string.title_settings),
    Dashboard,
    MovieDetails(args = listOf("url", "apiName")),
    TvSeriesDetails(args = listOf("url", "apiName")),
    MediaDetails(args = listOf("url", "apiName")),
    TvPlayer(args = listOf("url", "apiName", "episodeData"));

    operator fun invoke(): String {
        val argList = StringBuilder()
        args?.let { nnArgs ->
            nnArgs.forEach { arg -> argList.append("/{$arg}") }
        }
        return name + argList
    }

    @Composable
    fun title(): String {
        return stringRes?.let { stringResource(it) } ?: name
    }

    fun withArgs(vararg args: Any): String {
        val destination = StringBuilder()
        args.forEach { arg -> destination.append("/$arg") }
        return name + destination
    }
}
