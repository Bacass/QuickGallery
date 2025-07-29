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
import com.lee.quickgallery.ui.MainScreen
import com.lee.quickgallery.ui.PermissionScreen
import com.lee.quickgallery.ui.SplashScreen
import com.lee.quickgallery.ui.theme.QuickGalleryTheme

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
            MainScreen()
        }
    }
}

sealed class Screen {
    object Splash : Screen()
    object Permission : Screen()
    object Main : Screen()
}