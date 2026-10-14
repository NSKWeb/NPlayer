package com.example.nplayer.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.nplayer.ui.detail.DetailScreen
import com.example.nplayer.ui.favourites.FavouritesScreen
import com.example.nplayer.ui.folders.FoldersScreen
import com.example.nplayer.ui.home.HomeScreen
import com.example.nplayer.ui.player.PlayerScreen
import com.example.nplayer.ui.search.SearchScreen
import com.example.nplayer.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val FOLDERS = "folders"
    const val FAVOURITES = "favourites"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{itemKey}"
    const val PLAYER = "player/{itemKey}"

    // Item keys are `content://` URIs, so they must be encoded to survive as a
    // single path segment and decoded on the way back out.
    fun detail(itemKey: String) = "detail/${Uri.encode(itemKey)}"
    fun player(itemKey: String) = "player/${Uri.encode(itemKey)}"

    fun decodeItemKey(raw: String?): String = Uri.decode(raw.orEmpty())
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.SEARCH, "Search", Icons.Filled.Search),
    Tab(Routes.FOLDERS, "Folders", Icons.Filled.Folder),
    Tab(Routes.FAVOURITES, "Favourites", Icons.Filled.Favorite),
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
                HomeScreen(onOpenDetail = { navController.navigate(Routes.detail(it)) })
            }
            composable(Routes.SEARCH) {
                SearchScreen(onOpenDetail = { navController.navigate(Routes.detail(it)) })
            }
            composable(Routes.FOLDERS) {
                FoldersScreen(onOpenDetail = { navController.navigate(Routes.detail(it)) })
            }
            composable(Routes.FAVOURITES) {
                FavouritesScreen(onOpenDetail = { navController.navigate(Routes.detail(it)) })
            }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.DETAIL) { entry ->
                DetailScreen(
                    itemKey = Routes.decodeItemKey(entry.arguments?.getString("itemKey")),
                    onBack = { navController.popBackStack() },
                    onPlay = { navController.navigate(Routes.player(it)) },
                )
            }
            composable(Routes.PLAYER) { entry ->
                PlayerScreen(
                    itemKey = Routes.decodeItemKey(entry.arguments?.getString("itemKey")),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
