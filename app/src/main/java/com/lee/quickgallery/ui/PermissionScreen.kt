package com.lee.quickgallery.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.lee.quickgallery.R
import android.net.Uri
import android.content.Intent
import androidx.compose.ui.graphics.Color

@Composable
fun PermissionScreen(
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(false) }
    var safGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            permissionGranted = true
            // SAF 권한 체크
            val prefs = context.getSharedPreferences("saf", Context.MODE_PRIVATE)
            val uriString = prefs.getString("gallery_folder_uri", null)
            safGranted = uriString != null
            if (safGranted) {
                onPermissionGranted()
            }
        }
    }

    // SAF 폴더 권한 런처
    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            context.getSharedPreferences("saf", Context.MODE_PRIVATE)
                .edit().putString("gallery_folder_uri", it.toString()).apply()
            safGranted = true
            // SAF 권한까지 모두 있으면 onPermissionGranted 호출
            if (permissionGranted && safGranted) {
                onPermissionGranted()
            }
        }
    }

    val checkAndRequestPermissions = {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            // Android 12 이하
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isEmpty()) {
            permissionGranted = true
            // SAF 권한 체크
            val prefs = context.getSharedPreferences("saf", Context.MODE_PRIVATE)
            val uriString = prefs.getString("gallery_folder_uri", null)
            safGranted = uriString != null
            if (safGranted) {
                onPermissionGranted()
            }
        } else {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "권한이 필요합니다",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "갤러리 앱이 정상적으로 작동하려면\n사진과 영상에 접근할 수 있는 권한이 필요합니다.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    checkAndRequestPermissions()
                    // SAF 권한이 없으면 SAF 런처도 자동 실행
                    val prefs = context.getSharedPreferences("saf", Context.MODE_PRIVATE)
                    val uriString = prefs.getString("gallery_folder_uri", null)
                    if (uriString == null) {
                        safLauncher.launch(null)
                    }
                },
                modifier = Modifier.size(width = 200.dp, height = 48.dp)
            ) {
                Text(
                    text = "권한 허용",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { safLauncher.launch(null) },
                modifier = Modifier.size(width = 200.dp, height = 48.dp)
            ) {
                Text(
                    text = "갤러리 폴더 권한 추가",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (safGranted) {
                Text(
                    text = "갤러리 폴더 권한이 허용되었습니다.",
                    color = Color.Green,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (permissionGranted && !safGranted) {
                Text(
                    text = "갤러리 폴더 권한도 추가로 필요합니다.",
                    color = Color.Red,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
} 