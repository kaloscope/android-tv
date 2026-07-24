package org.kaloscope.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.kaloscope.tv.app.KaloscopeApp
import org.kaloscope.tv.app.KaloscopeViewModel
import org.kaloscope.tv.app.MainViewModel
import org.kaloscope.tv.feature.detail.MediaDetailViewModel
import org.kaloscope.tv.feature.library.LibraryViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: KaloscopeViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()
    private val libraryViewModel: LibraryViewModel by viewModels()
    private val detailViewModel: MediaDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KaloscopeApp(
                viewModel = viewModel,
                mainViewModel = mainViewModel,
                libraryViewModel = libraryViewModel,
                detailViewModel = detailViewModel,
            )
        }
    }
}
