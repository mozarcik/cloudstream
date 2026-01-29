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
import com.lagradost.cloudstream3.tv.presentation.screens.media.MediaDetailsScreen
import com.lagradost.cloudstream3.tv.presentation.screens.media.MediaDetailsScreenViewModel
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamTheme

class MediaDetailsComposeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val savedStateHandle = SavedStateHandle.Companion.createHandle(null, arguments)
        val viewModel = ViewModelProvider(
            this,
            MediaDetailsScreenViewModelFactory(
                savedStateHandle = savedStateHandle,
                repository = MovieRepositoryImpl()
            )
        )[MediaDetailsScreenViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CloudStreamTheme {
                    MediaDetailsScreen(
                        mediaDetailsScreenViewModel = viewModel,
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
