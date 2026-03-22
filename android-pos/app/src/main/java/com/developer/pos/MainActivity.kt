package com.developer.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import com.developer.pos.ui.navigation.PosApp
import com.developer.pos.ui.theme.DeveloperPosTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DeveloperPosTheme {
                PosApp()
            }
        }
    }
}
