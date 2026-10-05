package com.claudianapolitano.leyla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.claudianapolitano.leyla.core.LanguageManager
import com.claudianapolitano.leyla.core.Session
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.feature.RootScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val language by LanguageManager.current.collectAsStateWithLifecycle()

            LeylaTheme {
                LaunchedEffect(Unit) { Session.bootstrap() }
                // The cached table is already in place from startup; this is
                // the quiet refresh behind it.
                LaunchedEffect(Unit) { LanguageManager.refresh() }

                // Arabic, Urdu and Persian mirror the whole layout. Providing
                // it here rather than in the manifest is what lets the choice
                // apply without a relaunch.
                CompositionLocalProvider(
                    LocalLayoutDirection provides
                        if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                ) {
                    RootScreen()
                }
            }
        }
    }
}
