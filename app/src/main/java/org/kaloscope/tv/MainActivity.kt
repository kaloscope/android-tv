package org.kaloscope.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.kaloscope.tv.app.KaloscopeApp
import org.kaloscope.tv.app.KaloscopeViewModel
import org.kaloscope.tv.app.MainViewModel
import org.kaloscope.tv.feature.detail.MediaDetailViewModel
import org.kaloscope.tv.feature.library.LibraryViewModel
import org.kaloscope.tv.feature.player.PlayerViewModel
import org.kaloscope.tv.feature.search.SearchViewModel
import org.kaloscope.tv.core.player.PlaybackControllerFactory

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: KaloscopeViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()
    private val libraryViewModel: LibraryViewModel by viewModels()
    private val searchViewModel: SearchViewModel by viewModels()
    private val detailViewModel: MediaDetailViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

    @Inject
    lateinit var playbackControllerFactory: PlaybackControllerFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KaloscopeApp(
                viewModel = viewModel,
                mainViewModel = mainViewModel,
                searchViewModel = searchViewModel,
                libraryViewModel = libraryViewModel,
                detailViewModel = detailViewModel,
                playerViewModel = playerViewModel,
                playbackControllerFactory = playbackControllerFactory,
            )
        }
    }
}
