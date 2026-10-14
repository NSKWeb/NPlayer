package com.example.nplayer.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.fragment.app.FragmentActivity
import com.example.nplayer.ui.detail.DetailScreen
import com.example.nplayer.ui.favourites.FavouritesScreen
import com.example.nplayer.ui.folders.FoldersScreen
import com.example.nplayer.ui.home.HomeScreen
import com.example.nplayer.ui.player.PlayerScreen
import com.example.nplayer.ui.playlists.PlaylistDetailScreen
import com.example.nplayer.ui.playlists.PlaylistsScreen
import com.example.nplayer.ui.privatefolder.PrivateFolderScreen
import com.example.nplayer.ui.promptBiometric
import com.example.nplayer.ui.search.SearchScreen
import com.example.nplayer.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val FOLDERS = "folders"
    const val FAVOURITES = "favourites"
    const val PLAYLISTS = "playlists"
    const val PLAYLIST_DETAIL = "playlist/{playlistId}"
    const val PRIVATE_FOLDER = "private"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{itemKey}"
    // `playlist` is an optional query argument: present when the player was opened
    // from a playlist, absent when it was opened from anywhere else.
    const val PLAYER = "player/{itemKey}?playlist={playlist}"

    // Item keys are `content://` URIs, so they must be encoded to survive as a
    // single path segment and decoded on the way back out.
    fun detail(itemKey: String) = "detail/${Uri.encode(itemKey)}"
    fun player(itemKey: String) = "player/${Uri.encode(itemKey)}"

    /** Plays a playlist as a queue, starting at [itemKey]. */
    fun playerInPlaylist(itemKey: String, playlistId: String) =
        "player/${Uri.encode(itemKey)}?playlist=${Uri.encode(playlistId)}"

    fun playlistDetail(playlistId: String) = "playlist/${Uri.encode(playlistId)}"

    fun decodeItemKey(raw: String?): String = Uri.decode(raw.orEmpty())
}

/** A destination in the bottom bar, with a filled icon when selected. */
private data class Tab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    Tab(Routes.SEARCH, "Search", Icons.Filled.Search, Icons.Outlined.Search),
    Tab(Routes.FOLDERS, "Folders", Icons.Filled.Folder, Icons.Outlined.FolderOpen),
    Tab(Routes.FAVOURITES, "Favourites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    Tab(Routes.PLAYLISTS, "Playlists", Icons.AutoMirrored.Filled.QueueMusic, Icons.AutoMirrored.Outlined.QueueMusic),
    Tab(Routes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
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
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    tabs.forEach { tab ->
                        val selected = backStackEntry?.destination?.hierarchy
                            ?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) tab.selectedIcon else tab.icon,
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
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
            composable(Routes.PLAYLISTS) {
                PlaylistsScreen(onOpenPlaylist = { navController.navigate(Routes.playlistDetail(it)) })
            }
            composable(Routes.PRIVATE_FOLDER) {
                val activity = LocalContext.current as? FragmentActivity
                var unlocked by remember { mutableStateOf(false) }
                PrivateFolderScreen(
                    onBack = { navController.popBackStack() },
                    onPlayItem = { navController.navigate(Routes.player(it)) },
                    unlocked = unlocked,
                    onUnlock = {
                        // Falls back to direct access where biometrics are unavailable.
                        if (activity != null) promptBiometric(activity) { unlocked = true }
                        else unlocked = true
                    },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onOpenPrivateFolder = { navController.navigate(Routes.PRIVATE_FOLDER) })
            }
            composable(Routes.DETAIL) { entry ->
                DetailScreen(
                    itemKey = Routes.decodeItemKey(entry.arguments?.getString("itemKey")),
                    onBack = { navController.popBackStack() },
                    onPlay = { navController.navigate(Routes.player(it)) },
                )
            }
            composable(
                route = Routes.PLAYER,
                arguments = listOf(androidx.navigation.navArgument("playlist") { nullable = true }),
            ) { entry ->
                PlayerScreen(
                    itemKey = Routes.decodeItemKey(entry.arguments?.getString("itemKey")),
                    onBack = { navController.popBackStack() },
                    queuePlaylistId = entry.arguments?.getString("playlist")?.let(Routes::decodeItemKey),
                )
            }
            composable(Routes.PLAYLIST_DETAIL) { entry ->
                PlaylistDetailScreen(
                    playlistId = Routes.decodeItemKey(entry.arguments?.getString("playlistId")),
                    onBack = { navController.popBackStack() },
                    onPlayItem = { itemKey ->
                        val playlistId = Routes.decodeItemKey(entry.arguments?.getString("playlistId"))
                        navController.navigate(Routes.playerInPlaylist(itemKey, playlistId))
                    },
                )
            }
        }
    }
}
