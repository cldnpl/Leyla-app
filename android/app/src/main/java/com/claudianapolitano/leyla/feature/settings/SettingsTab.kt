package com.claudianapolitano.leyla.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.leylaString
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.weight
import com.claudianapolitano.leyla.feature.premium.PaywallScreen
import com.claudianapolitano.leyla.feature.premium.PaywallTrigger
import com.claudianapolitano.leyla.feature.profile.ProfileEditScreen

/**
 * Settings' own navigation stack — every domain page pushes from here, the way
 * iOS's `NavigationStack` around `SettingsView` pushes them, so they all stay
 * under the tab bar.
 */
object SettingsRoutes {
    const val ROOT = "settings/root"
    const val PROFILE = "settings/profile"
    const val RELATIONSHIP = "settings/relationship"
    const val HEALTH = "settings/health"
    const val LANGUAGE = "settings/language"
    const val PREMIUM = "settings/premium"
    const val PAYWALL = "settings/paywall"
    const val ACCOUNT = "settings/account"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val colors = LeylaTheme.colors
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    val title = when (route) {
        SettingsRoutes.PROFILE -> leylaString(R.string.profile_title)
        SettingsRoutes.RELATIONSHIP -> leylaString(R.string.settings_relationship)
        SettingsRoutes.HEALTH -> leylaString(R.string.health_source)
        SettingsRoutes.LANGUAGE -> leylaString(R.string.settings_language)
        SettingsRoutes.PREMIUM -> leylaString(R.string.settings_premium_title)
        SettingsRoutes.ACCOUNT -> leylaString(R.string.settings_account)
        // The paywall draws its own chrome, as it does when Games opens it.
        SettingsRoutes.PAYWALL -> null
        else -> leylaString(R.string.tab_settings)
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            if (title != null) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(title, style = IOSText.headline.weight(FontWeight.SemiBold), color = colors.ink)
                    },
                    navigationIcon = {
                        if (route != SettingsRoutes.ROOT) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = leylaString(R.string.back),
                                    tint = colors.ink,
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(navController = navController, startDestination = SettingsRoutes.ROOT) {
                composable(SettingsRoutes.ROOT) {
                    SettingsScreen(
                        onOpenProfile = { navController.navigate(SettingsRoutes.PROFILE) },
                        onOpenRelationship = { navController.navigate(SettingsRoutes.RELATIONSHIP) },
                        onOpenHealth = { navController.navigate(SettingsRoutes.HEALTH) },
                        onOpenLanguage = { navController.navigate(SettingsRoutes.LANGUAGE) },
                        onOpenPremium = { navController.navigate(SettingsRoutes.PREMIUM) },
                        onOpenAccount = { navController.navigate(SettingsRoutes.ACCOUNT) },
                    )
                }
                composable(SettingsRoutes.PROFILE) { ProfileEditScreen() }
                composable(SettingsRoutes.RELATIONSHIP) { RelationshipSettingsScreen() }
                composable(SettingsRoutes.HEALTH) { HealthSettingsScreen() }
                composable(SettingsRoutes.LANGUAGE) { LanguagePickerScreen() }
                composable(SettingsRoutes.PREMIUM) {
                    PremiumSettingsScreen(onOpenPaywall = { navController.navigate(SettingsRoutes.PAYWALL) })
                }
                composable(SettingsRoutes.PAYWALL) {
                    PaywallScreen(
                        trigger = PaywallTrigger.General,
                        onDismiss = { navController.popBackStack() },
                    )
                }
                composable(SettingsRoutes.ACCOUNT) { AccountSettingsScreen() }
            }
        }
    }
}
