package com.claudianapolitano.leyla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import com.claudianapolitano.leyla.core.Session
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.feature.MainTabView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LeylaTheme {
                // Sign-in, pairing and onboarding are routed by `Session.state`
                // on iOS's RootView; until those screens are ported the app
                // opens straight on the tab shell.
                LaunchedEffect(Unit) { Session.bootstrap() }
                MainTabView()
            }
        }
    }
}
