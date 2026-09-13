package com.claudianapolitano.leyla.feature

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.feature.home.HomeScreen
import com.claudianapolitano.leyla.feature.placeholder.ComingSoonScreen

/** The four tabs, in the order iOS's `TabView` declares them. */
enum class Tab(
    val route: String,
    @param:StringRes val titleRes: Int,
    val icon: ImageVector,
) {
    HOME("home", R.string.tab_home, Icons.Filled.Home),
    GAMES("games", R.string.tab_games, Icons.Filled.AutoAwesome),
    JOURNAL("journal", R.string.tab_journal, Icons.AutoMirrored.Filled.MenuBook),
    SETTINGS("settings", R.string.tab_settings, Icons.Filled.Settings),
}

/**
 * The app shell: four tabs over the warm backdrop. Port of
 * `Us/Features/MainTabView.swift`.
 *
 * iOS presents the post-pairing setup flow and the notifications primer as
 * full-screen covers from here; both land with their own screens.
 */
@Composable
fun MainTabView(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val colors = LeylaTheme.colors

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = colors.solidBackground.copy(alpha = 0.92f),
                tonalElevation = 0.dp,
            ) {
                Tab.entries.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                // Tapping a tab returns to its root and keeps the
                                // other tabs' stacks alive, the way a TabView does.
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.titleRes), style = IOSText.caption2) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Theme.rose,
                            selectedTextColor = Theme.rose,
                            unselectedIconColor = colors.secondary,
                            unselectedTextColor = colors.secondary,
                            indicatorColor = Theme.rose.copy(alpha = 0.14f),
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(innerPadding)
        ) {
            NavHost(navController = navController, startDestination = Tab.HOME.route) {
                composable(Tab.HOME.route) { HomeScreen() }
                composable(Tab.GAMES.route) {
                    ComingSoonScreen(Tab.GAMES.titleRes, Icons.Filled.AutoAwesome, R.string.games_blurb)
                }
                composable(Tab.JOURNAL.route) {
                    ComingSoonScreen(Tab.JOURNAL.titleRes, Icons.AutoMirrored.Filled.MenuBook, R.string.journal_blurb)
                }
                composable(Tab.SETTINGS.route) {
                    ComingSoonScreen(Tab.SETTINGS.titleRes, Icons.Filled.Settings, R.string.settings_blurb)
                }
            }
        }
    }
}
