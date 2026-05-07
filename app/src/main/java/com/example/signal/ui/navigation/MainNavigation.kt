package com.example.signal.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.signal.ui.dashboard.DashboardScreen
import com.example.signal.ui.settings.SettingsScreen
import com.example.signal.ui.taskboard.TaskBoardScreen

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val iconSelected: ImageVector,
    val label: String
) {
    object Tasks : BottomNavItem(
        "tasks",
        Icons.Outlined.Inbox,
        Icons.Outlined.Inbox,
        "Inbox"
    )
    object Dashboard : BottomNavItem(
        "dashboard",
        Icons.Outlined.BarChart,
        Icons.Outlined.BarChart,
        "Insights"
    )
    object Settings : BottomNavItem(
        "settings",
        Icons.Outlined.Settings,
        Icons.Outlined.Settings,
        "Settings"
    )
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Tasks,
        BottomNavItem.Dashboard,
        BottomNavItem.Settings
    )
    val cs = MaterialTheme.colorScheme

    Scaffold(
        containerColor = cs.background,
        bottomBar = {
            NavigationBar(
                containerColor  = cs.surface,
                tonalElevation  = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick  = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.iconSelected else item.icon,
                                contentDescription = item.label
                            )
                        },
                        label  = { Text(item.label, style = MaterialTheme.typography.labelMedium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = cs.primary,
                            selectedTextColor   = cs.primary,
                            unselectedIconColor = cs.onSurfaceVariant,
                            unselectedTextColor = cs.onSurfaceVariant,
                            indicatorColor      = cs.primaryContainer
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController    = navController,
            startDestination = BottomNavItem.Tasks.route,
            modifier         = Modifier.padding(paddingValues)
        ) {
            composable(BottomNavItem.Tasks.route)     { TaskBoardScreen() }
            composable(BottomNavItem.Dashboard.route) { DashboardScreen() }
            composable(BottomNavItem.Settings.route)  { SettingsScreen()  }
        }
    }
}
