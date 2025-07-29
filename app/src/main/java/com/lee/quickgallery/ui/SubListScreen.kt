package com.lee.quickgallery.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lee.quickgallery.ui.components.MediaThumbnail
import com.lee.quickgallery.ui.viewmodel.GalleryViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubListScreen(
    folderPath: String,
    onBackClick: () -> Unit = {},
    onMediaClick: (String) -> Unit = {},
    viewModel: GalleryViewModel = viewModel()
) {
    val mediaList by viewModel.mediaList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val hasPermission by viewModel.hasPermission.collectAsState()
    
    // 폴더 경로에서 폴더명 추출
    val folderName = folderPath.substringAfterLast("/", folderPath)
    
    // Back key 처리
    BackHandler {
        onBackClick()
    }
    
    // 화면 진입 시 해당 폴더의 미디어 로드
    LaunchedEffect(folderPath) {
        viewModel.loadMediaByFolder(folderPath)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = folderName,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadMediaByFolder(folderPath) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "새로고침"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
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
                
                mediaList.isEmpty() -> {
                    // 빈 상태
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "이 폴더에 미디어가 없습니다",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                else -> {
                    // 미디어 그리드 (3열) with 스크롤바
                    val gridState = rememberLazyGridState()
                    val coroutineScope = rememberCoroutineScope()
                    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()) }
                    
                    // 스크롤바 표시 상태
                    var showScrollbar by remember { mutableStateOf(false) }
                    var isScrollbarDragging by remember { mutableStateOf(false) }
                    
                    // 스크롤바 상호작용 소스
                    val scrollbarInteractionSource = remember { MutableInteractionSource() }
                    val isScrollbarPressed by scrollbarInteractionSource.collectIsPressedAsState()
                    
                    // 현재 보이는 아이템의 날짜 계산
                    val currentDate by remember {
                        derivedStateOf {
                            val layoutInfo = gridState.layoutInfo
                            val visibleItems = layoutInfo.visibleItemsInfo
                            if (visibleItems.isNotEmpty()) {
                                val firstVisibleIndex = visibleItems.first().index
                                if (firstVisibleIndex < mediaList.size) {
                                    val mediaItem = mediaList[firstVisibleIndex]
                                    val date = Date(mediaItem.dateAdded * 1000)
                                    dateFormat.format(date)
                                } else {
                                    ""
                                }
                            } else {
                                ""
                            }
                        }
                    }
                    
                    // 스크롤바는 항상 표시
                    showScrollbar = true
                    
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(
                                items = mediaList,
                                key = { it.id }
                            ) { mediaItem ->
                                MediaThumbnail(
                                    mediaItem = mediaItem,
                                    onClick = { onMediaClick(mediaItem.uri.toString()) }
                                )
                            }
                        }
                        
                        // 주소록 스타일 스크롤바
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 8.dp, top = 16.dp, bottom = 16.dp)
                                .width(20.dp)
                                .fillMaxHeight()
                        ) {
                            // 스크롤바 배경 (항상 표시)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Color.Gray.copy(alpha = 0.3f),
                                        RoundedCornerShape(10.dp)
                                    )
                            )
                            
                            // 스크롤바 썸네일 (현재 위치 표시)
                            val scrollProgress by remember {
                                derivedStateOf {
                                    val layoutInfo = gridState.layoutInfo
                                    val totalItems = mediaList.size
                                    val visibleItems = layoutInfo.visibleItemsInfo
                                    
                                    if (totalItems > 0 && visibleItems.isNotEmpty()) {
                                        val firstVisibleIndex = visibleItems.first().index
                                        (firstVisibleIndex.toFloat() / totalItems).coerceIn(0f, 1f)
                                    } else {
                                        0f
                                    }
                                }
                            }
                            
                            val thumbHeight = 60.dp
                            val scrollbarHeight = gridState.layoutInfo.viewportEndOffset - gridState.layoutInfo.viewportStartOffset - 32.dp.value // top, bottom padding 고려
                            val maxThumbOffset = scrollbarHeight - thumbHeight.value
                            
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(y = (scrollProgress * maxThumbOffset).dp)
                                    .width(20.dp)
                                    .height(thumbHeight)
                                    .background(
                                        Color.White.copy(alpha = 0.7f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { 
                                                isScrollbarDragging = true
                                                showScrollbar = true
                                            },
                                            onDragEnd = {
                                                isScrollbarDragging = false
                                                // 1.5초 후에 숨김
                                                coroutineScope.launch {
                                                    kotlinx.coroutines.delay(1500)
                                                    showScrollbar = false
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                
                                                // 드래그 위치에 따라 스크롤 위치 계산 (더 부드럽게)
                                                val totalItems = mediaList.size
                                                val scrollbarHeight = gridState.layoutInfo.viewportEndOffset - gridState.layoutInfo.viewportStartOffset - 32f
                                                val thumbHeight = 60f
                                                val maxThumbOffset = scrollbarHeight - thumbHeight
                                                
                                                val currentThumbOffset = scrollProgress * maxThumbOffset
                                                val newThumbOffset = (currentThumbOffset + dragAmount.y * 0.5f).coerceIn(0f, maxThumbOffset) // 드래그 감도 조절
                                                val newScrollProgress = newThumbOffset / maxThumbOffset
                                                
                                                coroutineScope.launch {
                                                    val targetIndex = (newScrollProgress * totalItems).toInt()
                                                        .coerceIn(0, totalItems - 1)
                                                    gridState.scrollToItem(targetIndex)
                                                }
                                            }
                                        )
                                    }
                            )
                        }
                        
                        // 날짜 표시 (스크롤바 드래그 시에만 표시) - 스크롤바 외부에 배치
                        if (isScrollbarDragging && currentDate.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .offset(x = (-30).dp)
                                    .wrapContentSize()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.8f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = currentDate,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 