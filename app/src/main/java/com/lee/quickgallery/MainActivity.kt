package com.lee.quickgallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lee.quickgallery.ui.MainScreen
import com.lee.quickgallery.ui.PermissionScreen
import com.lee.quickgallery.ui.SettingScreen
import com.lee.quickgallery.ui.SplashScreen
import com.lee.quickgallery.ui.SubListScreen
import com.lee.quickgallery.ui.theme.QuickGalleryTheme
import com.lee.quickgallery.ui.viewmodel.GalleryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuickGalleryTheme {
                QuickGalleryApp()
            }
        }
    }
}

@Composable
fun QuickGalleryApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    var selectedFolderPath by remember { mutableStateOf<String?>(null) }
    
    // 공유 ViewModel 인스턴스
    val sharedViewModel: GalleryViewModel = viewModel()
    
    when (currentScreen) {
        Screen.Splash -> {
            SplashScreen(
                onSplashComplete = {
                    currentScreen = Screen.Permission
                },
                onPermissionAlreadyGranted = {
                    currentScreen = Screen.Main
                }
            )
        }
        Screen.Permission -> {
            PermissionScreen(
                onPermissionGranted = {
                    currentScreen = Screen.Main
                }
            )
        }
        Screen.Main -> {
            MainScreen(
                onFolderClick = { folderPath ->
                    selectedFolderPath = folderPath
                    currentScreen = Screen.SubList
                },
                onSettingsClick = {
                    currentScreen = Screen.Settings
                },
                viewModel = sharedViewModel
            )
        }
        Screen.SubList -> {
            selectedFolderPath?.let { folderPath ->
                SubListScreen(
                    folderPath = folderPath,
                    onBackClick = {
                        currentScreen = Screen.Main
                    },
                    onMediaClick = { mediaUri ->
                        // 미디어 클릭 시 처리 (예: 상세 보기, 공유 등)
                    },
                    viewModel = sharedViewModel
                )
            }
        }
        Screen.Settings -> {
            SettingScreen(
                onBackClick = {
                    currentScreen = Screen.Main
                },
                viewModel = sharedViewModel
            )
        }
    }
}

sealed class Screen {
    object Splash : Screen()
    object Permission : Screen()
    object Main : Screen()
    object SubList : Screen()
    object Settings : Screen()
}