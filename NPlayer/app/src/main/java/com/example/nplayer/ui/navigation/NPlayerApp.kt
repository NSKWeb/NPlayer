package com.example.nplayer.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.nplayer.ui.auth.AuthScreen
import com.example.nplayer.ui.auth.AuthViewModel
import com.example.nplayer.ui.detail.DetailScreen
import com.example.nplayer.ui.downloads.DownloadsScreen
import com.example.nplayer.ui.home.HomeScreen
import com.example.nplayer.ui.player.PlayerScreen
import com.example.nplayer.ui.search.SearchScreen
import com.example.nplayer.ui.settings.SettingsScreen
import com.example.nplayer.ui.settings.SettingsViewModel

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val DOWNLOADS = "downloads"
    const val SETTINGS = "settings"
    const val AUTH = "auth"
    const val DETAIL = "detail/{itemId}"
    const val PLAYER = "player/{itemId}"

    fun detail(id: String) = "detail/$id"
    fun player(id: String) = "player/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.SEARCH, "Search", Icons.Filled.Search),
    Tab(Routes.DOWNLOADS, "Downloads", Icons.Filled.Download),
    Tab(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
)

@Composable
fun NPlayerApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showTabs = tabs.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showTabs) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = backStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenDetail = { navController.navigate(Routes.detail(it)) },
                    onRequireAuth = { navController.navigate(Routes.AUTH) },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(onOpenDetail = { navController.navigate(Routes.detail(it)) })
            }
            composable(Routes.DOWNLOADS) { DownloadsScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onSignOut = {
                        navController.navigate(Routes.AUTH) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.AUTH) {
                AuthScreen(onAuthenticated = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                })
            }
            composable(Routes.DETAIL) { entry ->
                val itemId = entry.arguments?.getString("itemId").orEmpty()
                DetailScreen(
                    itemId = itemId,
                    onBack = { navController.popBackStack() },
                    onPlay = { navController.navigate(Routes.player(it)) },
                )
            }
            composable(Routes.PLAYER) { entry ->
                val itemId = entry.arguments?.getString("itemId").orEmpty()
                PlayerScreen(itemId = itemId, onBack = { navController.popBackStack() })
            }
        }
    }
}
