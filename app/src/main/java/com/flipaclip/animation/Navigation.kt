package com.flipaclip.animation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flipaclip.animation.ui.home.HomeScreen
import com.flipaclip.animation.ui.studio.StudioScreen
import com.flipaclip.animation.ui.viewmodel.ExportViewModel
import com.flipaclip.animation.ui.viewmodel.HomeViewModel
import com.flipaclip.animation.ui.viewmodel.StudioViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Studio : Screen("studio/{projectId}") {
        fun createRoute(projectId: String) = "studio/$projectId"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()
    val studioViewModel: StudioViewModel = viewModel()
    val exportViewModel: ExportViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onOpenProject = { projectId ->
                    navController.navigate(Screen.Studio.createRoute(projectId))
                },
                onExportProject = { projectId ->
                    navController.navigate(Screen.Studio.createRoute(projectId))
                }
            )
        }

        composable(
            route = Screen.Studio.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            StudioScreen(
                projectId = projectId,
                studioViewModel = studioViewModel,
                exportViewModel = exportViewModel,
                onBackToHome = {
                    homeViewModel.refreshProjects()
                    navController.popBackStack()
                }
            )
        }
    }
}
