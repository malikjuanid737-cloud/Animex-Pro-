package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*

object AppRoutes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME_MAIN = "home_main"
    const val WATCH = "watch/{animeId}/{episodeId}"
}

@Composable
fun AppNavigation(
    startDestination: String = AppRoutes.SPLASH
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(AppRoutes.SPLASH) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(AppRoutes.HOME_MAIN) {
                        popUpTo(AppRoutes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(AppRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoutes.LOGIN) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(AppRoutes.HOME_MAIN) {
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(AppRoutes.REGISTER)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(AppRoutes.FORGOT_PASSWORD)
                }
            )
        }

        composable(AppRoutes.REGISTER) {
            RegistrationScreen(
                onNavigateToHome = {
                    navController.navigate(AppRoutes.HOME_MAIN) {
                        popUpTo(AppRoutes.REGISTER) { inclusive = true }
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(AppRoutes.REGISTER) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoutes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }

        composable(AppRoutes.HOME_MAIN) {
            HomeScreenMain(
                onNavigateToWatch = { animeId, episodeId ->
                    navController.navigate("watch/$animeId/$episodeId")
                },
                onLogoutNavigation = {
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(AppRoutes.HOME_MAIN) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = AppRoutes.WATCH,
            arguments = listOf(
                navArgument("animeId") { type = NavType.StringType },
                navArgument("episodeId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val animeId = backStackEntry.arguments?.getString("animeId") ?: "a1"
            val episodeId = backStackEntry.arguments?.getString("episodeId") ?: "ep1"
            
            WatchScreen(
                animeId = animeId,
                episodeId = episodeId,
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToNextEpisode = { nextAnimeId, nextEpisodeId ->
                    navController.navigate("watch/$nextAnimeId/$nextEpisodeId") {
                        popUpTo("watch/$animeId/$episodeId") { inclusive = true }
                    }
                }
            )
        }
    }
}
