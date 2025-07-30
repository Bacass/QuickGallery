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
                    
                    // 터치 드래그 전용 상태 추가
                    var isTouchDragging by remember { mutableStateOf(false) }
                    var dragStartY by remember { mutableStateOf(0f) }
                    var dragCurrentY by remember { mutableStateOf(0f) }
                    
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
                                if (isScrolling && !isTouchDragging) {
                                    // 일반 스크롤 시에는 스크롤바만 표시
                                    showScrollbar = true
                                } else if (!isScrolling && !isTouchDragging) {
                                    // 스크롤이 멈추고 터치 드래그가 아닐 때만 자동 숨김
                                    kotlinx.coroutines.delay(2000)
                                    if (!isScrollbarDragging && !isTouchDragging) {
                                        showScrollbar = false
                                        showDatePopup = false
                                    }
                                }
                            }
                    }
                    
                    // 미디어 리스트가 있을 때만 스크롤바 관련 처리
                    val shouldShowScrollbar = remember(mediaList.size) {
                        mediaList.size > 6 // 6개 이상일 때 스크롤바 표시
                    }
                    
                    // 초기 로드 시 스크롤바 표시
                    LaunchedEffect(mediaList.size) {
                        if (mediaList.isNotEmpty() && shouldShowScrollbar) {
                            showScrollbar = true
                            kotlinx.coroutines.delay(3000) // 3초 표시
                            if (!isScrollbarDragging && !isTouchDragging) {
                                showScrollbar = false
                            }
                        }
                    }
                    
                    // Thumb 높이 계산
                    val baseThumbHeight = 80.dp
                    val minThumbHeight = 40.dp
                    val maxThumbHeight = 120.dp
                    val thumbHeightPx = with(density) {
                        if (mediaList.size > 0) {
                            val ratio = if (totalMediaCount > mediaList.size) {
                                mediaList.size.toFloat() / totalMediaCount
                            } else {
                                0.6f // 기본 비율
                            }
                            (baseThumbHeight.value * ratio.coerceIn(0.3f, 1f))
                                .coerceIn(minThumbHeight.value, maxThumbHeight.value).dp.toPx()
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
                        
                        // 스크롤바 Thumb (디버깅용 - 항상 표시)
                        if (mediaList.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = 10.dp, top = 10.dp, bottom = 10.dp)
                                    .width(24.dp)
                                    .fillMaxHeight()
//                                    .background(Color.Red.copy(alpha = 0.2f)) // 디버깅용 배경
                                    .onGloballyPositioned { coordinates ->
                                        containerHeightPx = coordinates.size.height.toFloat()
                                    }
                                    .pointerInput(Unit) {
                                        // 스크롤바 영역의 모든 터치 이벤트를 가로채서 리스트로 전파되지 않도록 함
                                        detectTapGestures { /* 터치 이벤트 소비 */ }
                                    }
                            ) {
                                // 스크롤바 트랙 (배경)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .width(6.dp)
                                        .fillMaxHeight()
                                        .background(
                                            Color.Black.copy(alpha = 0.4f), 
                                            RoundedCornerShape(3.dp)
                                        )
                                )
                                // 미디어 수 정보 표시 (터치 드래그일 때만)
                                if (isTouchDragging && totalMediaCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .offset(x = (-50).dp)
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
                                
                                // Thumb (터치 영역과 표시 영역 통합)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(y = thumbOffset)
                                        .width(28.dp) // 전체 터치 영역
                                        .height(with(density) { thumbHeightPx.coerceAtLeast(50.dp.toPx()).toDp() })
                                        // .background(Color.Green.copy(alpha = 0.3f)) // 디버깅용 - 터치 영역
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = { offset ->
                                                    // 탭한 위치로 즉시 스크롤
                                                    if (mediaList.isNotEmpty() && containerHeightPx > 0f) {
                                                        val progress = (offset.y / containerHeightPx).coerceIn(0f, 1f)
                                                        val targetIndex = (progress * (mediaList.size - 1).coerceAtLeast(0)).toInt()
                                                            .coerceIn(0, mediaList.size - 1)
                                                        
                                                        println("DEBUG: Tap at offset: $offset, progress: $progress, targetIndex: $targetIndex")
                                                        coroutineScope.launch {
                                                            gridState.animateScrollToItem(targetIndex)
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        .pointerInput(Unit, mediaList.size) {
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    // 현재 스크롤 위치를 기반으로 실제 thumb 위치 계산
                                                    val layoutInfo = gridState.layoutInfo
                                                    val visibleItem = layoutInfo.visibleItemsInfo.firstOrNull()
                                                    val currentScrollProgress = if (visibleItem != null && mediaList.isNotEmpty()) {
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
                                                    
                                                    val safeRange = (containerHeightPx - thumbHeightPx).coerceAtLeast(0f)
                                                    val actualThumbPosition = currentScrollProgress * safeRange
                                                    
                                                    println("DEBUG: Drag started at offset: $offset, actualThumbPosition: $actualThumbPosition, scrollProgress: $currentScrollProgress")
                                                    isScrollbarDragging = true
                                                    isTouchDragging = true
                                                    showScrollbar = true
                                                    showDatePopup = true
                                                    // 실제 thumb 위치를 시작점으로 설정
                                                    dragStartY = actualThumbPosition
                                                    dragCurrentY = actualThumbPosition
                                                },
                                                onDragEnd = {
                                                    println("DEBUG: Drag ended")
                                                    isScrollbarDragging = false
                                                    isTouchDragging = false
                                                    // 드래그 상태 리셋
                                                    dragStartY = 0f
                                                    dragCurrentY = 0f
                                                    coroutineScope.launch {
                                                        kotlinx.coroutines.delay(1500)
                                                        showScrollbar = false
                                                        showDatePopup = false
                                                    }
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    // 드래그 델타를 누적하여 새로운 thumb 위치 계산
                                                    dragCurrentY += dragAmount.y
                                                    
                                                    if (mediaList.isNotEmpty() && containerHeightPx > 0f) {
                                                        val safeRange = (containerHeightPx - thumbHeightPx).coerceAtLeast(0f)
                                                        // dragCurrentY가 이미 실제 thumb 위치
                                                        val clampedPosition = dragCurrentY.coerceIn(0f, safeRange)
                                                        val progress = if (safeRange > 0f) (clampedPosition / safeRange) else 0f
                                                        val targetIndex = (progress * (mediaList.size - 1).coerceAtLeast(0)).toInt()
                                                            .coerceIn(0, mediaList.size - 1)
                                                        
                                                        println("DEBUG: dragAmount: $dragAmount, dragCurrentY: $dragCurrentY, progress: $progress, targetIndex: $targetIndex")
                                                        
                                                        coroutineScope.launch {
                                                            gridState.scrollToItem(targetIndex)
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                ) {
                                    // 실제 보이는 Thumb (우측 정렬)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .width(12.dp)
                                            .fillMaxHeight()
                                            .background(
                                                Color.White.copy(alpha = if (isTouchDragging) 1.0f else 0.9f), 
                                                RoundedCornerShape(6.dp)
                                            )
                                    )
                                }
                            }
                        }

                        // 날짜 팝업 표시 (터치 드래그일 때만)
                        if (isTouchDragging && currentDate.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(
                                        x = (-50).dp,
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