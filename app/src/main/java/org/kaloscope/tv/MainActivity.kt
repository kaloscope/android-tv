package org.kaloscope.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.kaloscope.tv.app.KaloscopeApp
import org.kaloscope.tv.app.KaloscopeViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: KaloscopeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KaloscopeApp(viewModel)
        }
    }
}
