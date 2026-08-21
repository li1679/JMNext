package com.par9uet.jm.ui.feature.home

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.par9uet.jm.ui.feature.home.HomeScreen
import com.par9uet.jm.navigation.LocalMainNavController
import com.par9uet.jm.ui.feature.user.UserCollectComicScreen
import com.par9uet.jm.ui.feature.user.UserScreen
import com.par9uet.jm.data.storage.LocalSettingManager
import com.par9uet.jm.domain.store.UserManager
import org.koin.compose.getKoin

@Composable
fun TabScreen(
    tabName: String,
    userManager: UserManager = getKoin().get(),
    localSettingManager: LocalSettingManager = getKoin().get()
) {
    val tabNavController = rememberNavController()
    val mainNavController = LocalMainNavController.current
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isLogin by userManager.isLoginState.collectAsStateWithLifecycle(false)
    val localSetting by localSettingManager.localSettingState.collectAsStateWithLifecycle()
    CompositionLocalProvider(
        LocalTabNavController provides tabNavController,
    ) {
        BoxWithConstraints {
            val useNavigationRail = maxWidth >= 700.dp
            Scaffold(
                bottomBar = {
                    if (!useNavigationRail) {
                        BottomNavigationBarComponent()
                    }
                },
                topBar = {
                    TopBarComponent()
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    if (useNavigationRail) {
                        NavigationRailComponent()
                    }
                    NavHost(
                        modifier = Modifier.weight(1f),
                        navController = tabNavController,
                        startDestination = tabName,
                        // 与主 NavHost 一致：默认转场会让退出页面多停留 700ms
                        // 并继续参与命中测试，快速切 Tab 时会点到旧页面上的控件
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None }
                    ) {
                        composable("home") {
                            HomeScreen()
                        }
                        composable("user") {
                            UserScreen()
                        }
                        composable("collect") {
                            LaunchedEffect(isLogin) {
                                if (!isLogin) {
                                    mainNavController.navigate("login")
                                }
                            }
                            if (isLogin) {
                                UserCollectComicScreen(useScaffold = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

val LocalTabNavController = staticCompositionLocalOf<NavHostController> {
    error("none")
}
