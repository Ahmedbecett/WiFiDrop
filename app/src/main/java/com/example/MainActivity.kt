package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.screens.FileManagerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReceiveScreen
import com.example.ui.screens.SendFilesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StorageScreen
import com.example.ui.screens.TransferHistoryScreen
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.WiFiDropTheme
import com.example.util.AdManager
import com.example.util.AppLanguage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AdManager.initialize(this)
        setContent {
            val viewModel: MainViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val currentLang by viewModel.currentLanguage.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()

            val layoutDirection = if (currentLang.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                WiFiDropTheme(darkTheme = isDarkTheme) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (currentScreen != Screen.SEND && currentScreen != Screen.RECEIVE) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                                    tonalElevation = 8.dp,
                                    modifier = Modifier.testTag("main_bottom_nav")
                                ) {
                                    NavigationBarItem(
                                        selected = currentScreen == Screen.HOME,
                                        onClick = { viewModel.navigateTo(Screen.HOME) },
                                        icon = {
                                            Icon(
                                                imageVector = if (currentScreen == Screen.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                                contentDescription = AppLanguage.getString("nav_home")
                                            )
                                        },
                                        label = { Text(AppLanguage.getString("nav_home")) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = BrandCyan,
                                            selectedTextColor = BrandCyan,
                                            indicatorColor = BrandCyan.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier.testTag("nav_item_home")
                                    )

                                    NavigationBarItem(
                                        selected = currentScreen == Screen.FILES,
                                        onClick = { viewModel.navigateTo(Screen.FILES) },
                                        icon = {
                                            Icon(
                                                imageVector = if (currentScreen == Screen.FILES) Icons.Filled.Folder else Icons.Outlined.Folder,
                                                contentDescription = AppLanguage.getString("nav_files")
                                            )
                                        },
                                        label = { Text(AppLanguage.getString("nav_files")) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = BrandCyan,
                                            selectedTextColor = BrandCyan,
                                            indicatorColor = BrandCyan.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier.testTag("nav_item_files")
                                    )

                                    NavigationBarItem(
                                        selected = currentScreen == Screen.HISTORY,
                                        onClick = { viewModel.navigateTo(Screen.HISTORY) },
                                        icon = {
                                            Icon(
                                                imageVector = if (currentScreen == Screen.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                                                contentDescription = AppLanguage.getString("nav_history")
                                            )
                                        },
                                        label = { Text(AppLanguage.getString("nav_history")) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = BrandCyan,
                                            selectedTextColor = BrandCyan,
                                            indicatorColor = BrandCyan.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier.testTag("nav_item_history")
                                    )

                                    NavigationBarItem(
                                        selected = currentScreen == Screen.STORAGE,
                                        onClick = { viewModel.navigateTo(Screen.STORAGE) },
                                        icon = {
                                            Icon(
                                                imageVector = if (currentScreen == Screen.STORAGE) Icons.Filled.Storage else Icons.Outlined.Storage,
                                                contentDescription = AppLanguage.getString("nav_storage")
                                            )
                                        },
                                        label = { Text(AppLanguage.getString("nav_storage")) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = BrandCyan,
                                            selectedTextColor = BrandCyan,
                                            indicatorColor = BrandCyan.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier.testTag("nav_item_storage")
                                    )

                                    NavigationBarItem(
                                        selected = currentScreen == Screen.SETTINGS,
                                        onClick = { viewModel.navigateTo(Screen.SETTINGS) },
                                        icon = {
                                            Icon(
                                                imageVector = if (currentScreen == Screen.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                                contentDescription = AppLanguage.getString("nav_settings")
                                            )
                                        },
                                        label = { Text(AppLanguage.getString("nav_settings")) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = BrandCyan,
                                            selectedTextColor = BrandCyan,
                                            indicatorColor = BrandCyan.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier.testTag("nav_item_settings")
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            label = "screen_transition"
                        ) { screen ->
                            when (screen) {
                                Screen.HOME -> HomeScreen(viewModel = viewModel)
                                Screen.SEND -> SendFilesScreen(viewModel = viewModel)
                                Screen.RECEIVE -> ReceiveScreen(viewModel = viewModel)
                                Screen.FILES -> FileManagerScreen(viewModel = viewModel)
                                Screen.HISTORY -> TransferHistoryScreen(viewModel = viewModel)
                                Screen.STORAGE -> StorageScreen(viewModel = viewModel)
                                Screen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
