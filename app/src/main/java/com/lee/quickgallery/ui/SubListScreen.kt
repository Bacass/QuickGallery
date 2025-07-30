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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lee.quickgallery.ui.components.MediaThumbnail
import com.lee.quickgallery.ui.viewmodel.GalleryViewModel
import com.lee.quickgallery.util.AppPrefs
import com.lee.quickgallery.util.SortType
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
    val totalMediaCount by viewModel.totalMediaCount.collectAsState()
    
    // 현재 정렬 방식을 State로 관리
    var currentSortType by remember { mutableStateOf(SortType.fromString(AppPrefs.mediaSortType)) }
    
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
    
    // 정렬 방식 변경 감지 및 새로고침
    LaunchedEffect(Unit) {
        while (true) {
            val newSortType = SortType.fromString(AppPrefs.mediaSortType)
            if (currentSortType != newSortType) {
                currentSortType = newSortType
                viewModel.loadMediaByFolder(folderPath, forceRefresh = true)
            }
            kotlinx.coroutines.delay(100) // 100ms마다 체크
        }
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                    val density = LocalDensity.current
                    
                    // 스크롤바 표시 상태
                    var showScrollbar by remember { mutableStateOf(false) }
                    var isScrollbarDragging by remember { mutableStateOf(false) }
                    var showDatePopup by remember { mutableStateOf(false) }
                    var containerHeightPx by remember { mutableStateOf(0f) }
                    
                    // 스크롤 진행도 계산 (example.kt 방식)
                    val scrollProgress by remember {
                        derivedStateOf {
                            val layoutInfo = gridState.layoutInfo
                            val visibleItem = layoutInfo.visibleItemsInfo.firstOrNull()
                            if (visibleItem != null && mediaList.isNotEmpty()) {
                                val totalItems = mediaList.size
                                val lastIndex = totalItems - layoutInfo.visibleItemsInfo.size
                                if (lastIndex > 0) {
                                    (visibleItem.index.toFloat() / lastIndex).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                            } else {
                                0f
                            }
                        }
                    }
                    
                    // 현재 날짜 계산
                    val currentDate by remember {
                        derivedStateOf {
                            if (mediaList.isNotEmpty()) {
                                val targetIndex = (scrollProgress * (mediaList.size - 1)).toInt()
                                    .coerceIn(0, mediaList.size - 1)
                                val mediaItem = mediaList[targetIndex]
                                val date = Date(mediaItem.dateAdded * 1000)
                                dateFormat.format(date)
                            } else {
                                ""
                            }
                        }
                    }
                    
                    // 스크롤 상태 감지
                    LaunchedEffect(gridState) {
                        snapshotFlow { gridState.isScrollInProgress }
                            .collect { isScrolling ->
                                if (isScrolling) {
                                    showDatePopup = true
                                    showScrollbar = true
                                } else {
                                    kotlinx.coroutines.delay(1500)
                                    if (!isScrollbarDragging) {
                                        showDatePopup = false
                                        showScrollbar = false
                                    }
                                }
                            }
                    }
                    
                    // Thumb 높이 계산
                    val baseThumbHeight = 60.dp
                    val thumbHeightPx = with(density) {
                        if (totalMediaCount > 0) {
                            val ratio = mediaList.size.toFloat() / totalMediaCount
                            (baseThumbHeight.value * ratio.coerceIn(0.2f, 1f)).coerceAtMost(100f).dp.toPx()
                        } else {
                            baseThumbHeight.toPx()
                        }
                    }
                    
                    // Thumb 위치 계산
                    val absoluteSafeRangePx = (containerHeightPx - thumbHeightPx).coerceAtLeast(0f)
                    val thumbOffsetPx = (scrollProgress * absoluteSafeRangePx).coerceIn(0f, absoluteSafeRangePx)
                    val thumbOffset = with(density) { thumbOffsetPx.toDp() }
                    
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            state = gridState,
                            modifier = Modifier.fillMaxSize(),
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
                        
                        // 스크롤바 Thumb
                        if (showScrollbar || isScrollbarDragging) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = 10.dp, top = 10.dp, bottom = 16.dp)
                                    .width(10.dp)
                                    .fillMaxHeight()
                                    .onGloballyPositioned { coordinates ->
                                        containerHeightPx = coordinates.size.height.toFloat()
                                    }
                            ) {
                                // 미디어 수 정보 표시
                                if ((showDatePopup || isScrollbarDragging) && totalMediaCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .offset(x = (-80).dp)
                                            .wrapContentSize()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Black.copy(alpha = 0.8f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${mediaList.size}/${totalMediaCount}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                
                                // Thumb
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .offset(y = thumbOffset)
                                        .width(10.dp)
                                        .height(with(density) { thumbHeightPx.toDp() })
                                        .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(5.dp))
                                        .pointerInput(Unit) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    isScrollbarDragging = true
                                                    showScrollbar = true
                                                    showDatePopup = true
                                                },
                                                onDragEnd = {
                                                    isScrollbarDragging = false
                                                    coroutineScope.launch {
                                                        kotlinx.coroutines.delay(1500)
                                                        showScrollbar = false
                                                        showDatePopup = false
                                                    }
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    
                                                    val scrollableRange = mediaList.size - 1
                                                    val newOffsetPx = (thumbOffsetPx + dragAmount.y * 0.5f)
                                                        .coerceIn(0f, absoluteSafeRangePx)
                                                    val newProgress = if (absoluteSafeRangePx > 0f)
                                                        (newOffsetPx / absoluteSafeRangePx).coerceIn(0f, 1f)
                                                    else 0f
                                                    
                                                    coroutineScope.launch {
                                                        if (scrollableRange > 0) {
                                                            val targetIndex = (newProgress * scrollableRange.toFloat()).toInt()
                                                                .coerceIn(0, scrollableRange)
                                                            gridState.scrollToItem(targetIndex)
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                )
                            }
                        }

                        // 날짜 팝업 표시
                        if ((showDatePopup || isScrollbarDragging) && currentDate.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(
                                        x = (-30).dp,
                                        y = thumbOffset + with(density) { (thumbHeightPx / 2).toDp() } - 10.dp // 중앙 정렬 + 미세 조정
                                    )
                                    .wrapContentSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.85f))
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