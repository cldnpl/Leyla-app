package com.claudianapolitano.leyla.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.leylaString
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.weight
import com.claudianapolitano.leyla.feature.cycle.CycleDetailScreen
import com.claudianapolitano.leyla.feature.map.PartnerMapScreen
import com.claudianapolitano.leyla.feature.profile.ProfileEditScreen

/**
 * Home's own navigation stack — the map, the profile editor and the cycle
 * screen all push from here, the way iOS's `NavigationStack` around `HomeView`
 * pushes them.
 *
 * Home itself draws its own chrome (logo, widget pill, profile button), so the
 * bar only appears once something is pushed on top of it.
 */
object HomeRoutes {
    const val ROOT = "home/root"
    const val MAP = "home/map"
    const val PROFILE = "home/profile"
    const val CYCLE = "home/cycle"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTab(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    val colors = LeylaTheme.colors
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    var showWidgetGuide by remember { mutableStateOf(false) }

    val title = when (route) {
        HomeRoutes.MAP -> leylaString(R.string.map_title)
        HomeRoutes.PROFILE -> leylaString(R.string.profile_title)
        HomeRoutes.CYCLE -> leylaString(R.string.cycle_title)
        else -> null
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            if (title != null) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            title,
                            style = IOSText.headline.weight(FontWeight.SemiBold),
                            color = colors.ink,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.ink,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                )
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(navController = navController, startDestination = HomeRoutes.ROOT) {
                composable(HomeRoutes.ROOT) {
                    HomeScreen(
                        onAddWidget = { showWidgetGuide = true },
                        onEditProfile = { navController.navigate(HomeRoutes.PROFILE) },
                        onOpenMap = { navController.navigate(HomeRoutes.MAP) },
                        onOpenCycle = { navController.navigate(HomeRoutes.CYCLE) },
                    )
                }
                composable(HomeRoutes.MAP) { PartnerMapScreen() }
                composable(HomeRoutes.PROFILE) { ProfileEditScreen() }
                composable(HomeRoutes.CYCLE) { CycleDetailScreen() }
            }
        }
    }

    if (showWidgetGuide) {
        // iOS plays a recorded walkthrough of adding the widget. There is no
        // Android widget to add yet, so this says exactly that instead of
        // walking someone through a home-screen flow that would find nothing.
        ModalBottomSheet(
            onDismissRequest = { showWidgetGuide = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = colors.solidBackground,
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    leylaString(R.string.widget_title),
                    style = IOSText.title2.weight(FontWeight.Bold),
                    color = colors.ink,
                )
                Text(
                    leylaString(R.string.widget_coming_soon_body),
                    style = IOSText.subheadline,
                    color = colors.secondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Text(
                    leylaString(R.string.coming_soon),
                    style = IOSText.caption.weight(FontWeight.Bold),
                    color = Theme.coral,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
