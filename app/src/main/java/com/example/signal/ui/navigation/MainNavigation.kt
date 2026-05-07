package com.example.signal.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.signal.ui.dashboard.DashboardScreen
import com.example.signal.ui.settings.SettingsScreen
import com.example.signal.ui.taskboard.TaskBoardScreen

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Tasks     : BottomNavItem("tasks",     Icons.Default.List,       "Tasks")
    object Dashboard : BottomNavItem("dashboard", Icons.Default.BarChart,   "Stats")
    object Settings  : BottomNavItem("settings",  Icons.Default.Settings,   "Settings")
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val items = listOf(BottomNavItem.Tasks, BottomNavItem.Dashboard, BottomNavItem.Settings)

    Scaffold(
        containerColor = Color(0xFF0D0D1A),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF12122A),
                tonalElevation = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF6C63FF),
                            selectedTextColor = Color(0xFF6C63FF),
                            unselectedIconColor = Color.White.copy(alpha = 0.4f),
                            unselectedTextColor = Color.White.copy(alpha = 0.4f),
                            indicatorColor = Color(0xFF6C63FF).copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Tasks.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(BottomNavItem.Tasks.route)     { TaskBoardScreen() }
            composable(BottomNavItem.Dashboard.route) { DashboardScreen() }
            composable(BottomNavItem.Settings.route)  { SettingsScreen() }
        }
    }
}
