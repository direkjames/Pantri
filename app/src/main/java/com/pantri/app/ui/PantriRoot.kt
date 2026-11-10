package com.pantri.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pantri.app.ui.components.FloatingNav
import com.pantri.app.ui.components.LocalHaptics
import com.pantri.app.ui.components.Tabs
import com.pantri.app.ui.screens.BudgetScreen
import com.pantri.app.ui.screens.HomeScreen
import com.pantri.app.ui.screens.ListScreen
import com.pantri.app.ui.screens.PlanScreen
import com.pantri.app.ui.screens.RecipeDetailScreen
import com.pantri.app.ui.screens.RecipeEditorScreen
import com.pantri.app.ui.screens.RecipesScreen
import com.pantri.app.ui.screens.SettingsScreen
import com.pantri.app.ui.screens.StoresScreen
import com.pantri.app.ui.theme.pantri
import androidx.compose.material3.MaterialTheme

val LocalSnack = staticCompositionLocalOf<SnackbarHostState> { error("No SnackbarHostState provided") }

private val tabRoutes = Tabs.map { it.route }.toSet()

@Composable
fun PantriRoot(haptics: Boolean) {
    val nav = rememberNavController()
    val snack = remember { SnackbarHostState() }
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val showNav = route in tabRoutes
    LaunchedEffect(Unit) { UiErrors.events.collect { snack.showSnackbar(it) } }

    CompositionLocalProvider(LocalHaptics provides haptics, LocalSnack provides snack) {
        // Phones fill the screen; tablets and landscape get a centred column so nothing stretches.
        Box(Modifier.fillMaxSize().background(MaterialTheme.pantri.bg), contentAlignment = Alignment.TopCenter) {
          Box(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
            NavHost(
                nav, "home", Modifier.fillMaxSize(),
                enterTransition = { fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 24 } },
                exitTransition = { fadeOut(tween(140)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(160)) + slideOutVertically(tween(220)) { it / 24 } },
            ) {
                composable("home") {
                    HomeScreen(
                        onOpenList = { nav.navigate("list/$it") },
                        onNavigate = { nav.goTab(it) },
                        onSettings = { nav.navigate("settings") },
                        onStores = { nav.navigate("stores") },
                    )
                }
                composable("stores") { StoresScreen(onBack = { nav.popBackStack() }) }
                composable("recipes") { RecipesScreen(onOpen = { nav.navigate("recipe/$it") }, onNew = { nav.navigate("recipeEdit/0") }) }
                composable("plan") { PlanScreen(onNewRecipe = { nav.navigate("recipeEdit/0") }, onOpenList = { nav.navigate("list/$it") }) }
                composable("budget") { BudgetScreen() }
                composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
                composable("list/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                    ListScreen(it.arguments!!.getLong("id"), onBack = { nav.popBackStack() })
                }
                composable("recipe/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                    val id = it.arguments!!.getLong("id")
                    RecipeDetailScreen(id, onBack = { nav.popBackStack() }, onEdit = { nav.navigate("recipeEdit/$id") }, onOpenList = { l -> nav.navigate("list/$l") })
                }
                composable("recipeEdit/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                    val id = it.arguments!!.getLong("id")
                    RecipeEditorScreen(id, onBack = { nav.popBackStack() }, onSaved = { saved ->
                        if (id == 0L) nav.navigate("recipe/$saved") { popUpTo("recipeEdit/0") { inclusive = true } } else nav.popBackStack()
                    })
                }
            }
            AnimatedVisibility(
                showNav, Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(tween(260)) { it } + fadeIn(), exit = slideOutVertically(tween(200)) { it } + fadeOut(),
            ) { FloatingNav(route, { nav.goTab(it) }) }
            SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = if (showNav) 96.dp else 150.dp))
          }
        }
    }
}

private fun androidx.navigation.NavHostController.goTab(route: String) = navigate(route) {
    popUpTo("home") { saveState = true }
    launchSingleTop = true
    restoreState = true
}
