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
                    
                    // 스크롤바 표시 상태
                    var showScrollbar by remember { mutableStateOf(false) }
                    var isScrollbarDragging by remember { mutableStateOf(false) }
                    var showDatePopup by remember { mutableStateOf(false) }
                    
                    // 스크롤바 상호작용 소스
                    val scrollbarInteractionSource = remember { MutableInteractionSource() }
                    val isScrollbarPressed by scrollbarInteractionSource.collectIsPressedAsState()
                    
                    // 스크롤 상태 감지 - 스크롤 중일 때 날짜 팝업 표시
                    LaunchedEffect(gridState) {
                        snapshotFlow { gridState.isScrollInProgress }
                            .collect { isScrolling ->
                                if (isScrolling) {
                                    showDatePopup = true
                                    showScrollbar = true
                                } else {
                                    // 스크롤이 끝나면 1.5초 후에 팝업과 스크롤바 숨김
                                    kotlinx.coroutines.delay(1500)
                                    if (!isScrollbarDragging) {
                                        showDatePopup = false
                                        showScrollbar = false
                                    }
                                }
                            }
                    }
                    
                    // thumb percentage에 해당하는 이미지의 날짜 계산
                    val currentDate by remember {
                        derivedStateOf {
                            val layoutInfo = gridState.layoutInfo
                            val visibleItems = layoutInfo.visibleItemsInfo
                            
                            if (visibleItems.isNotEmpty() && mediaList.isNotEmpty()) {
                                val firstVisibleIndex = visibleItems.first().index
                                val scrollableRange = mediaList.size - 1
                                
                                // 스크롤 진행률 계산
                                val scrollProgress = if (scrollableRange > 0) {
                                    (firstVisibleIndex.toFloat() / scrollableRange).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                                
                                // 진행률에 해당하는 이미지 인덱스 계산
                                val targetIndex = (scrollProgress * (mediaList.size - 1)).toInt()
                                    .coerceIn(0, mediaList.size - 1)
                                
                                if (targetIndex < mediaList.size) {
                                    val mediaItem = mediaList[targetIndex]
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
                    
                    // 스크롤바 관련 변수들을 상위 스코프로 이동
                    val density = LocalDensity.current
                    var containerHeightPx by remember { mutableStateOf(0) }
                    
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
                        
                        // 주소록 스타일 스크롤바 - 실제 크기 측정 방식
                        
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 10.dp, top = 16.dp, bottom = 16.dp)
                                .width(10.dp)
                                .fillMaxHeight()
                                .onGloballyPositioned { coordinates ->
                                    containerHeightPx = coordinates.size.height
                                }
                        ) {
                            // 실제 측정된 컨테이너 높이 (dp 단위로 변환)
                            val containerHeightDp = with(density) { containerHeightPx.toDp().value }
                            // 미디어 수 정보 표시 (스크롤 중이거나 드래그 중일 때)
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

                            
                            // 스크롤바 Thumb (현재 위치 표시) - 실제 로드된 미디어 기준으로 계산
                            val scrollProgress by remember {
                                derivedStateOf {
                                    val layoutInfo = gridState.layoutInfo
                                    val visibleItems = layoutInfo.visibleItemsInfo
                                    
                                    if (visibleItems.isNotEmpty() && mediaList.isNotEmpty()) {
                                        val firstVisibleIndex = visibleItems.first().index
                                        val scrollableRange = mediaList.size - 1
                                        
                                        if (scrollableRange > 0) {
                                            // 실제 로드된 미디어 범위 기준으로 진행률 계산
                                            (firstVisibleIndex.toFloat() / scrollableRange).coerceIn(0f, 1f)
                                        } else {
                                            0f
                                        }
                                    } else {
                                        0f
                                    }
                                }
                            }
                            
                            // 스크롤바 Thumb 크기 계산
                            val baseThumbHeight = 60.dp
                            
                            // 전체 미디어 수를 기반으로 Thumb 크기 조정
                            val thumbHeight = if (totalMediaCount > 0) {
                                val ratio = mediaList.size.toFloat() / totalMediaCount
                                (baseThumbHeight.value * ratio.coerceIn(0.2f, 1f)).dp.coerceAtMost(100.dp)
                            } else {
                                baseThumbHeight
                            }
                            
                            // 실제 측정된 컨테이너 높이 기반으로 안전한 범위 계산
                            val safeContainerHeight = containerHeightDp.coerceAtLeast(100f) // 최소 100dp 보장
                            
                            // 절대적으로 안전한 스크롤 범위 (컨테이너의 90%만 사용하여 확실한 안전 마진)
                            val absoluteSafeRange = (safeContainerHeight * 0.95f - thumbHeight.value).coerceAtLeast(0f)
                            
                            // Thumb 위치 - 절대적으로 안전한 범위 내에서만 이동
                            val thumbOffset = if (absoluteSafeRange > 0f && containerHeightPx > 0) {
                                (scrollProgress * absoluteSafeRange).coerceIn(0f, absoluteSafeRange)
                            } else {
                                0f
                            }
                            
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(y = thumbOffset.dp) // 실제 리스트 높이 기반으로 계산된 안전한 위치
                                    .width(10.dp)
                                    .height(thumbHeight)
                                    .background(
                                        Color.White.copy(alpha = 0.7f),
                                        RoundedCornerShape(5.dp)
                                    )
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { 
                                                isScrollbarDragging = true
                                                showScrollbar = true
                                                showDatePopup = true
                                            },
                                            onDragEnd = {
                                                isScrollbarDragging = false
                                                // 1.5초 후에 숨김
                                                coroutineScope.launch {
                                                    kotlinx.coroutines.delay(1500)
                                                    showScrollbar = false
                                                    showDatePopup = false
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                
                                                // 드래그 위치에 따라 스크롤 위치 계산 - 실제 로드된 미디어 기준
                                                val scrollableRange = mediaList.size - 1
                                                
                                                // 실제 측정된 컨테이너 크기 사용 (절대적으로 안전)
                                                val safeContainerHeight = containerHeightDp.coerceAtLeast(100f)
                                                val baseThumbHeight = 60f
                                                
                                                // 전체 미디어 수를 기반으로 Thumb 크기 조정
                                                val thumbHeight = if (totalMediaCount > 0) {
                                                    val ratio = mediaList.size.toFloat() / totalMediaCount
                                                    (baseThumbHeight * ratio.coerceIn(0.2f, 1f)).coerceAtMost(100f)
                                                } else {
                                                    baseThumbHeight
                                                }
                                                
                                                // 절대적으로 안전한 범위 (90% 사용)
                                                val absoluteSafeRange = (safeContainerHeight * 0.95f - thumbHeight).coerceAtLeast(0f)
                                                
                                                // 드래그 계산 - 절대적으로 안전한 범위 내에서만
                                                val currentOffset = scrollProgress * absoluteSafeRange
                                                val newOffset = (currentOffset + dragAmount.y * 0.5f).coerceIn(0f, absoluteSafeRange)
                                                val newScrollProgress = if (absoluteSafeRange > 0f) (newOffset / absoluteSafeRange).coerceIn(0f, 1f) else 0f
                                                
                                                coroutineScope.launch {
                                                    if (scrollableRange > 0) {
                                                        val targetIndex = (newScrollProgress * scrollableRange).toInt()
                                                            .coerceIn(0, scrollableRange)
                                                        // 실제 로드된 아이템 수를 초과하지 않도록 제한
                                                        val safeTargetIndex = targetIndex.coerceIn(0, mediaList.size - 1)
                                                        gridState.scrollToItem(safeTargetIndex)
                                                    }
                                                }
                                            }
                                        )
                                    }
                            )
                        }
                        
                        // 날짜 표시 (스크롤 중이거나 드래그 중일 때 표시) - thumb 위치에 맞춰서 배치
                        if ((showDatePopup || isScrollbarDragging) && currentDate.isNotEmpty()) {
                            // thumb 위치 계산 (위에서 계산한 것과 동일한 로직)
                            val layoutInfo = gridState.layoutInfo
                            val visibleItems = layoutInfo.visibleItemsInfo
                            val scrollProgress = if (visibleItems.isNotEmpty() && mediaList.isNotEmpty()) {
                                val firstVisibleIndex = visibleItems.first().index
                                val scrollableRange = mediaList.size - 1
                                if (scrollableRange > 0) {
                                    (firstVisibleIndex.toFloat() / scrollableRange).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                            } else {
                                0f
                            }
                            
                            // 컨테이너 높이와 thumb 높이 계산
                            val containerHeightDp = with(density) { containerHeightPx.toDp().value }
                            val safeContainerHeight = containerHeightDp.coerceAtLeast(100f)
                            val baseThumbHeight = 60f
                            val thumbHeight = if (totalMediaCount > 0) {
                                val ratio = mediaList.size.toFloat() / totalMediaCount
                                (baseThumbHeight * ratio.coerceIn(0.2f, 1f)).coerceAtMost(100f)
                            } else {
                                baseThumbHeight
                            }
                            
                            // thumb의 Y축 위치 계산
                            val absoluteSafeRange = (safeContainerHeight * 1f - thumbHeight).coerceAtLeast(0f)
                            val thumbYPosition = if (absoluteSafeRange > 0f && containerHeightPx > 0) {
                                (scrollProgress * absoluteSafeRange).coerceIn(0f, absoluteSafeRange)
                            } else {
                                0f
                            }
                            
                            // thumb 중앙에 맞춰서 날짜 팝업 위치 조정
                            val datePopupYOffset = thumbYPosition + (thumbHeight / 2f) - 12f // 팝업 높이의 절반만큼 위로 조정
                            
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-80).dp, y = datePopupYOffset.dp)
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