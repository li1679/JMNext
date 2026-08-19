package com.par9uet.jm.ui.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.par9uet.jm.R

/** 底部导航的目标页，NavigationBar 与 NavigationRail 共用 */
private enum class TabDestination(val route: String, val label: String) {
    Home("home", "首页"),
    Collect("collect", "收藏"),
    User("user", "我的"),
}

/** 图标不设 contentDescription：文字标签已提供无障碍名称，重复会被读两遍 */
@Composable
private fun TabDestination.TabIcon() {
    when (this) {
        TabDestination.Home -> Icon(painterResource(R.drawable.home_icon), contentDescription = null)
        TabDestination.Collect -> Icon(Icons.Filled.Bookmark, contentDescription = null)
        TabDestination.User -> Icon(painterResource(R.drawable.person_icon), contentDescription = null)
    }
}

@Composable
private fun currentTabRoute(navController: NavHostController): String? {
    val backStackEntryState by navController.currentBackStackEntryAsState()
    return backStackEntryState?.destination?.route
}

@Composable
fun BottomNavigationBarComponent() {
    val tabNavController = LocalTabNavController.current
    val currentRoute = currentTabRoute(tabNavController)

    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    AnimatedVisibility(visible = currentRoute != "login") {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp
        ) {
            TabDestination.entries.forEach { destination ->
                NavigationBarItem(
                    colors = itemColors,
                    icon = { destination.TabIcon() },
                    label = { Text(destination.label) },
                    selected = currentRoute == destination.route,
                    onClick = {
                        if (currentRoute != destination.route) {
                            tabNavController.navigate(destination.route)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun NavigationRailComponent() {
    val tabNavController = LocalTabNavController.current
    val currentRoute = currentTabRoute(tabNavController)

    val itemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        TabDestination.entries.forEach { destination ->
            NavigationRailItem(
                colors = itemColors,
                icon = { destination.TabIcon() },
                label = { Text(destination.label) },
                selected = currentRoute == destination.route,
                onClick = {
                    if (currentRoute != destination.route) {
                        tabNavController.navigate(destination.route)
                    }
                }
            )
        }
    }
}
