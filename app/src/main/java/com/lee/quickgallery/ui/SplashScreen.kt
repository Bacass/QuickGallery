package com.lee.quickgallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lee.quickgallery.R
import com.lee.quickgallery.util.PermissionUtil
import kotlinx.coroutines.delay
import android.net.Uri
import android.content.Context

fun hasSAFPermission(context: Context): Boolean {
    val prefs = context.getSharedPreferences("saf", Context.MODE_PRIVATE)
    val uriString = prefs.getString("gallery_folder_uri", null)
    if (uriString != null) {
        val uri = Uri.parse(uriString)
        val perms = context.contentResolver.persistedUriPermissions
        return perms.any { it.uri == uri && it.isReadPermission && it.isWritePermission }
    }
    return false
}

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit,
    onPermissionAlreadyGranted: () -> Unit
) {
    val context = LocalContext.current
    
    LaunchedEffect(key1 = true) {
        delay(1500L) // 1.5초 대기
        
        // 권한이 이미 있는지 확인
        if (PermissionUtil.hasRequiredPermissions(context) && hasSAFPermission(context)) {
            onPermissionAlreadyGranted()
        } else {
            onSplashComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(16.dp))
            
            Text(
                text = "Quick Gallery",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
} 