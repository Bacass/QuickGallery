package com.lee.quickgallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lee.quickgallery.ui.components.FolderThumbnail
import com.lee.quickgallery.ui.viewmodel.GalleryViewModel
import com.lee.quickgallery.util.AppPrefs
import com.lee.quickgallery.util.SortType
import com.lee.quickgallery.model.FolderItem
import com.lee.quickgallery.util.VibrationUtil
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyGridState
import org.burnoutcrew.reorderable.reorderable
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onFolderClick: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    viewModel: GalleryViewModel = viewModel()
) {
    val folderList by viewModel.folderList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val hasPermission by viewModel.hasPermission.collectAsState()
    val currentSortType by viewModel.currentSortType.collectAsState()
    
    // MainScreen이 나타날 때마다 정렬 방식 동기화
    DisposableEffect(Unit) {
        // 화면이 나타날 때 정렬 방식 동기화
        viewModel.syncSortType()
        
        onDispose { }
    }
    
    // 자동 새로고침 처리
    LaunchedEffect(Unit) {
        if (AppPrefs.autoRefresh) {
            viewModel.refreshMedia()
        }
    }
    
    // 폴더 목록이 변경될 때마다 순서 초기화
    LaunchedEffect(folderList) {
        if (folderList.isNotEmpty()) {
            viewModel.initializeFolderOrder()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quick Gallery",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshMedia() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "새로고침"
                        )
                    }
                    
                    IconButton(
                        onClick = onSettingsClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "설정"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        // 드래그 모드 상태 관리
        var isDragging by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // 드래그 모드일 때 각 폴더에 개별적으로 드래그 아이콘이 표시됩니다
            when {
                !hasPermission -> {
                    // 권한 없음 상태
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "미디어 접근 권한이 필요합니다",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                isLoading -> {
                    // 로딩 상태
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                errorMessage != null -> {
                    // 오류 상태
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                folderList.isEmpty() -> {
                    // 빈 상태
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "미디어를 찾을 수 없습니다",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                else -> {
                    // 폴더 그리드 (드래그 앤 드롭 지원)
                    var orderedFolderList by remember { mutableStateOf<List<FolderItem>>(emptyList()) }
                    val context = LocalContext.current
                    
                    LaunchedEffect(folderList) {
                        if (folderList.isNotEmpty()) {
                            viewModel.initializeFolderOrder()
                            orderedFolderList = viewModel.getOrderedFolderList()
                        } else {
                            orderedFolderList = emptyList()
                        }
                    }
                    
                    val reorderableState = rememberReorderableLazyGridState(
                        onMove = { from, to ->
                            viewModel.reorderFolders(from.index, to.index)
                            // 순서 변경 후 목록 업데이트
                            orderedFolderList = viewModel.getOrderedFolderList()
                        }
                    )
                    
                    // 드래그 상태는 ReorderableItem의 isDragging 파라미터를 통해 감지
                    // 진동은 detectReorderAfterLongPress에서 처리
                    
                    LazyVerticalGrid(
                        state = reorderableState.gridState,
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.reorderable(reorderableState)
                    ) {
                        items(
                            items = orderedFolderList,
                            key = { it.folderPath }
                        ) { folderItem ->
                            ReorderableItem(
                                reorderableState = reorderableState,
                                key = folderItem.folderPath
                            ) { isDraggingItem ->
                                // 드래그 시작 시 진동 및 상위 상태 업데이트
                                LaunchedEffect(isDraggingItem) {
                                    if (isDraggingItem && !isDragging) {
                                        isDragging = true
                                        VibrationUtil.vibrateForDragMode(context)
                                    } else if (!isDraggingItem && isDragging) {
                                        isDragging = false
                                    }
                                }
                                
                                FolderThumbnail(
                                    folderItem = folderItem,
                                    onClick = { onFolderClick(folderItem.folderPath) },
                                    isDragging = isDragging,
                                    modifier = Modifier.detectReorderAfterLongPress(reorderableState)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 