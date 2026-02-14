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
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.TvSeriesDetailsScreen
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.TvSeriesDetailsScreenViewModel
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamTheme

class TvSeriesDetailsComposeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val args = arguments
        val savedStateHandle = SavedStateHandle.Companion.createHandle(null, args).apply {
            args?.getString("name")?.takeIf { it.isNotBlank() }?.let { title ->
                set(TvSeriesDetailsScreen.LoadingTitleBundleKey, title)
            }
            args?.getString("poster")?.takeIf { it.isNotBlank() }?.let { poster ->
                set(TvSeriesDetailsScreen.LoadingPosterBundleKey, poster)
            }
            args?.getString("backdrop")?.takeIf { it.isNotBlank() }?.let { backdrop ->
                set(TvSeriesDetailsScreen.LoadingBackdropBundleKey, backdrop)
            }
        }
        val viewModel = ViewModelProvider(
            this,
            TvSeriesDetailsScreenViewModelFactory(
                savedStateHandle = savedStateHandle,
                repository = MovieRepositoryImpl()
            )
        )[TvSeriesDetailsScreenViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CloudStreamTheme {
                    TvSeriesDetailsScreen(
                        tvSeriesDetailsScreenViewModel = viewModel,
                        goToPlayer = { _ ->
                            // TODO: Play button callback
                        },
                        onBackPressed = {
                            findNavController().popBackStack()
                        },
                        refreshScreenWithNewItem = { movie ->
                            // TODO: Recommendations navigation
                        }
                    )
                }
            }
        }
    }
}
