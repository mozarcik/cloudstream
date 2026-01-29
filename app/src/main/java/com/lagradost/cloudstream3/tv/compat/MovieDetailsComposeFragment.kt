package com.lagradost.cloudstream3.tv.compat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.lagradost.cloudstream3.tv.data.repositories.MovieRepositoryImpl
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsScreen
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsScreenViewModel
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamTheme

/**
 * Fragment hosting Compose MovieDetailsScreen.
 * Receives url and apiName arguments to load movie details from CloudStream API.
 */
class MovieDetailsComposeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Create ViewModel manually (without Hilt)
        val savedStateHandle = SavedStateHandle.Companion.createHandle(null, arguments)
        val viewModel = ViewModelProvider(
            this,
            MovieDetailsScreenViewModelFactory(
                savedStateHandle = savedStateHandle,
                repository = MovieRepositoryImpl()
            )
        )[MovieDetailsScreenViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CloudStreamTheme {
                    MovieDetailsScreen(
                        movieDetailsScreenViewModel = viewModel,
                        goToMoviePlayer = {
                            // MVP v1: Play button is disabled, but callback is here for future
                        },
                        onBackPressed = {
                            findNavController().popBackStack()
                        },
                        refreshScreenWithNewMovie = { movie ->
                            // MVP v1: Not implemented, placeholder for recommendations
                        }
                    )
                }
            }
        }
    }
}