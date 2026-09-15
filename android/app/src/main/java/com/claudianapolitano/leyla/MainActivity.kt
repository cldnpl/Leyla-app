package com.claudianapolitano.leyla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import com.claudianapolitano.leyla.core.Session
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.feature.RootScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LeylaTheme {
                LaunchedEffect(Unit) { Session.bootstrap() }
                RootScreen()
            }
        }
    }
}
