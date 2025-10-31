package com.example.nexus.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * A data class to represent a single destination in our Bottom Navigation Bar.
 */
data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

/**
 * This is our main, reusable Bottom Navigation Bar Composable.
 * It is completely self-contained and only needs a NavController to do its job.
 */
@Composable
fun BottomNavigationBar(navController: NavController) {
    // This list defines the items that will appear in our navigation bar.
    val items = listOf(
        NavigationItem("Chats", Icons.Default.Chat, "home"),
        NavigationItem("Discover", Icons.Default.Search, "discovery"),
        NavigationItem("Settings", Icons.Default.Settings, "settings")
    )

    NavigationBar {
        // This gets the current screen's route. We use this to highlight the correct icon.
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // We loop through our list of items and create a NavigationBarItem for each one.
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        // This is a crucial piece of navigation logic.
                        // It ensures that when you press an icon, you don't just stack screens
                        // on top of each other. It always takes you back to the main level.
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when re-selecting the same item
                        launchSingleTop = true
                        // Restore state when re-selecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    }
}