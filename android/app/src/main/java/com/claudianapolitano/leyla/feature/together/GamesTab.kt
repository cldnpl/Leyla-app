package com.claudianapolitano.leyla.feature.together

import android.net.Uri
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.weight
import com.claudianapolitano.leyla.feature.premium.PaywallScreen
import com.claudianapolitano.leyla.feature.premium.PaywallTrigger

/**
 * The Games tab's own navigation stack — iOS wraps this tab in a
 * `NavigationStack`, so the pushes here stay inside the tab and the bottom bar
 * never moves.
 *
 * Every route carries the title its bar should show. The alternative — having
 * each screen publish a title upward once it has loaded — makes the bar flicker
 * through a placeholder on every push, which the iOS build never does because
 * the pushing view already knows the name it's navigating to.
 */
object GamesRoutes {
    const val ROOT = "games/root"
    const val DAILY = "games/daily"
    const val QUIZ_CATEGORIES = "games/quiz"
    const val QUIZ_CATEGORY = "games/quiz/{categoryId}/{title}"
    const val QUIZ_PLAY = "games/quiz/play/{quizId}/{colorKey}/{title}"
    const val HWDYKM_PACKS = "games/hwdykm"
    const val HWDYKM_PLAY = "games/hwdykm/{packId}/{colorKey}/{title}"
    const val DEBATE_PACKS = "games/debate"
    const val DEBATE_PLAY = "games/debate/{packId}/{colorKey}/{title}"
    const val DRAW = "games/draw"
    const val SNAP = "games/snap"
    const val COMING_SOON = "games/soon/{gameId}"
    const val PAYWALL = "games/paywall/{kind}/{title}"

    fun quizCategory(id: String, title: String) = "games/quiz/${enc(id)}/${enc(title)}"
    fun quizPlay(quizId: String, colorKey: String, title: String) =
        "games/quiz/play/${enc(quizId)}/${enc(colorKey)}/${enc(title)}"

    fun hwdykmPlay(packId: String, colorKey: String, title: String) =
        "games/hwdykm/${enc(packId)}/${enc(colorKey)}/${enc(title)}"

    fun debatePlay(packId: String, colorKey: String, title: String) =
        "games/debate/${enc(packId)}/${enc(colorKey)}/${enc(title)}"

    fun comingSoon(gameId: String) = "games/soon/${enc(gameId)}"

    fun paywall(trigger: PaywallTrigger): String = when (trigger) {
        is PaywallTrigger.General -> "games/paywall/general/${enc("-")}"
        is PaywallTrigger.QuizCategory -> "games/paywall/category/${enc(trigger.title)}"
        is PaywallTrigger.Game -> "games/paywall/game/${enc(trigger.title)}"
    }

    /**
     * Titles are free text from the catalog and can carry slashes or spaces,
     * which would otherwise split the route into extra path segments.
     */
    private fun enc(value: String) = Uri.encode(value.ifEmpty { "-" })
}

/**
 * Which title the bar shows for a destination.
 *
 * The carried `title` argument wins whenever a route has one, and only routes
 * without one are named by their pattern. Matching on the pattern first looked
 * equivalent but is not: it makes the bar depend on the exact route string the
 * Navigation library reports back, and a route that fails to match then silently
 * falls through to another branch's name instead of showing the title it was
 * literally handed.
 */
private fun barTitle(route: String?, arguments: android.os.Bundle?): String? {
    arguments?.getString("title")
        ?.takeIf { it.isNotEmpty() && it != "-" }
        ?.let { return it }

    return when (route) {
        GamesRoutes.DAILY -> "Question of the Day"
        GamesRoutes.QUIZ_CATEGORIES -> "Quiz"
        GamesRoutes.HWDYKM_PACKS -> "Know Me"
        GamesRoutes.DEBATE_PACKS -> "Couples Debate"
        GamesRoutes.DRAW -> "Draw Together"
        GamesRoutes.SNAP -> "Snap Hunt"
        GamesRoutes.COMING_SOON -> GameDef.byId(arguments?.getString("gameId").orEmpty())?.title
        // The root draws its own large title; the paywall has its own close button.
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesTab(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    val colors = LeylaTheme.colors
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val title = barTitle(route, backStackEntry?.arguments)
    // The paywall is a presentation, not a push: iOS shows it as a sheet with
    // only its own X, so it gets no bar here either.
    val canGoBack = route != null && route != GamesRoutes.ROOT && route != GamesRoutes.PAYWALL

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            if (title != null || canGoBack) {
                CenterAlignedTopAppBar(
                    title = {
                        if (title != null) {
                            Text(
                                title,
                                style = IOSText.headline.weight(FontWeight.SemiBold),
                                color = colors.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    navigationIcon = {
                        if (canGoBack) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = colors.ink,
                                )
                            }
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
            NavHost(navController = navController, startDestination = GamesRoutes.ROOT) {

                composable(GamesRoutes.ROOT) {
                    TogetherScreen(
                        onOpenDaily = { navController.navigate(GamesRoutes.DAILY) },
                        onOpenQuiz = { navController.navigate(GamesRoutes.QUIZ_CATEGORIES) },
                        onOpenGame = { game ->
                            navController.navigate(
                                when (game.kind) {
                                    GameDef.Kind.HWDYKM -> GamesRoutes.HWDYKM_PACKS
                                    GameDef.Kind.DEBATE -> GamesRoutes.DEBATE_PACKS
                                    GameDef.Kind.DRAW -> GamesRoutes.DRAW
                                    GameDef.Kind.SNAP -> GamesRoutes.SNAP
                                    GameDef.Kind.COMING_SOON -> GamesRoutes.comingSoon(game.id)
                                },
                            )
                        },
                        onLocked = { game ->
                            navController.navigate(GamesRoutes.paywall(PaywallTrigger.Game(game.title)))
                        },
                    )
                }

                composable(GamesRoutes.DAILY) { DailyQuizScreen() }

                composable(GamesRoutes.QUIZ_CATEGORIES) {
                    QuizCategoriesScreen(
                        onOpenCategory = { category ->
                            navController.navigate(GamesRoutes.quizCategory(category.id, category.title))
                        },
                        onLocked = { category ->
                            navController.navigate(
                                GamesRoutes.paywall(PaywallTrigger.QuizCategory(category.title)),
                            )
                        },
                    )
                }

                composable(
                    GamesRoutes.QUIZ_CATEGORY,
                    arguments = listOf(
                        navArgument("categoryId") { type = NavType.StringType },
                        navArgument("title") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val categoryId = entry.arguments?.getString("categoryId").orEmpty()
                    QuizCategoryScreen(
                        categoryId = categoryId,
                        onOpenQuiz = { quizId, colorKey, title ->
                            navController.navigate(GamesRoutes.quizPlay(quizId, colorKey, title))
                        },
                    )
                }

                composable(
                    GamesRoutes.QUIZ_PLAY,
                    arguments = listOf(
                        navArgument("quizId") { type = NavType.StringType },
                        navArgument("colorKey") { type = NavType.StringType },
                        navArgument("title") { type = NavType.StringType },
                    ),
                ) { entry ->
                    QuizPlayScreen(
                        quizId = entry.arguments?.getString("quizId").orEmpty(),
                        colorKey = entry.arguments?.getString("colorKey") ?: "pink",
                    )
                }

                composable(GamesRoutes.HWDYKM_PACKS) {
                    HwdykmPackListScreen(
                        onOpenPack = { pack ->
                            navController.navigate(GamesRoutes.hwdykmPlay(pack.id, pack.colorKey, pack.title))
                        },
                    )
                }

                composable(
                    GamesRoutes.HWDYKM_PLAY,
                    arguments = listOf(
                        navArgument("packId") { type = NavType.StringType },
                        navArgument("colorKey") { type = NavType.StringType },
                        navArgument("title") { type = NavType.StringType },
                    ),
                ) { entry ->
                    HwdykmPlayScreen(
                        packId = entry.arguments?.getString("packId").orEmpty(),
                        colorKey = entry.arguments?.getString("colorKey") ?: "pink",
                    )
                }

                composable(GamesRoutes.DEBATE_PACKS) {
                    DebatePackListScreen(
                        onOpenPack = { pack ->
                            navController.navigate(GamesRoutes.debatePlay(pack.id, pack.colorKey, pack.title))
                        },
                    )
                }

                composable(
                    GamesRoutes.DEBATE_PLAY,
                    arguments = listOf(
                        navArgument("packId") { type = NavType.StringType },
                        navArgument("colorKey") { type = NavType.StringType },
                        navArgument("title") { type = NavType.StringType },
                    ),
                ) { entry ->
                    DebatePlayScreen(
                        packId = entry.arguments?.getString("packId").orEmpty(),
                        colorKey = entry.arguments?.getString("colorKey") ?: "blue",
                    )
                }

                composable(GamesRoutes.DRAW) { DrawTogetherScreen() }

                composable(GamesRoutes.SNAP) { SnapHuntScreen() }

                composable(
                    GamesRoutes.COMING_SOON,
                    arguments = listOf(navArgument("gameId") { type = NavType.StringType }),
                ) { entry ->
                    val game = GameDef.byId(entry.arguments?.getString("gameId").orEmpty())
                    if (game != null) ComingSoonGameScreen(game)
                }

                composable(
                    GamesRoutes.PAYWALL,
                    arguments = listOf(
                        navArgument("kind") { type = NavType.StringType },
                        navArgument("title") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val kind = entry.arguments?.getString("kind").orEmpty()
                    val title = entry.arguments?.getString("title").orEmpty()
                    val trigger = when (kind) {
                        "category" -> PaywallTrigger.QuizCategory(title)
                        "game" -> PaywallTrigger.Game(title)
                        else -> PaywallTrigger.General
                    }
                    PaywallScreen(trigger = trigger, onDismiss = { navController.popBackStack() })
                }
            }
        }
    }
}
