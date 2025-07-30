package com.lee.quickgallery.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lee.quickgallery.ui.components.FullScreenImage
import com.lee.quickgallery.ui.viewmodel.GalleryViewModel
import kotlin.math.sqrt

// PointerEvent 확장 함수들
private fun PointerEvent.calculateZoom(): Float {
    if (changes.size < 2) return 1f
    
    val currentDistance = calculateDistance(changes[0].position, changes[1].position)
    val previousDistance = calculateDistance(changes[0].previousPosition, changes[1].previousPosition)
    
    return if (previousDistance > 0f) currentDistance / previousDistance else 1f
}

private fun PointerEvent.calculatePan(): Offset {
    if (changes.size < 2) return Offset.Zero
    
    val currentCentroid = calculateCentroid()
    val previousCentroid = Offset(
        x = (changes[0].previousPosition.x + changes[1].previousPosition.x) / 2f,
        y = (changes[0].previousPosition.y + changes[1].previousPosition.y) / 2f
    )
    
    return currentCentroid - previousCentroid
}

private fun PointerEvent.calculateCentroid(): Offset {
    if (changes.isEmpty()) return Offset.Zero
    
    val sumX = changes.sumOf { it.position.x.toDouble() }
    val sumY = changes.sumOf { it.position.y.toDouble() }
    
    return Offset(
        x = (sumX / changes.size).toFloat(),
        y = (sumY / changes.size).toFloat()
    )
}

private fun calculateDistance(point1: Offset, point2: Offset): Float {
    val dx = point1.x - point2.x
    val dy = point1.y - point2.y
    return sqrt(dx * dx + dy * dy)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    initialMediaUri: String,
    folderPath: String,
    onBackClick: () -> Unit = {},
    viewModel: GalleryViewModel = viewModel()
) {
    val mediaList by viewModel.mediaList.collectAsState()
    val context = LocalContext.current
    
    // 현재 미디어의 인덱스 찾기
    val initialIndex = remember(initialMediaUri, mediaList) {
        mediaList.indexOfFirst { it.uri.toString() == initialMediaUri }.coerceAtLeast(0)
    }
    
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { mediaList.size }
    )
    
    // 전역 줌 상태 추적 (현재 페이지만)
    var isZoomed by remember { mutableStateOf(false) }
    
    // Back key 처리
    BackHandler {
        onBackClick()
    }
    
    // 폴더의 미디어 로드
    LaunchedEffect(folderPath) {
        if (mediaList.isEmpty()) {
            viewModel.loadMediaByFolder(folderPath)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (mediaList.isNotEmpty()) "${pagerState.currentPage + 1} / ${mediaList.size}" else "0 / 0",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (mediaList.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = !isZoomed // 확대되지 않았을 때만 스와이프 허용
                ) { page ->
                    val mediaItem = mediaList[page]
                    
                    // 비디오 파일인 경우 썸네일과 재생 버튼 표시
                    if (mediaItem.mimeType.startsWith("video/")) {
                        VideoThumbnailView(
                            mediaItem = mediaItem,
                            onPlayClick = {
                                // 외부 영상 플레이어 앱으로 연결
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(mediaItem.uri, mediaItem.mimeType)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // 영상 플레이어 앱이 없는 경우 기본 앱 선택 다이얼로그 표시
                                    val chooserIntent = Intent.createChooser(intent, "영상 재생")
                                    context.startActivity(chooserIntent)
                                }
                            }
                        )
                    } else {
                        // 이미지 파일인 경우 기존 ZoomableImage 사용
                        ZoomableImage(
                            data = mediaItem.uri,
                            modifier = Modifier.fillMaxSize(),
                            isCurrentPage = page == pagerState.currentPage,
                            onZoomChanged = { zoomed ->
                                isZoomed = zoomed
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoThumbnailView(
    mediaItem: com.lee.quickgallery.util.MediaItem,
    onPlayClick: () -> Unit
) {
    val context = LocalContext.current
    val mediaStoreUtil = remember { com.lee.quickgallery.util.MediaStoreUtil(context) }
    var thumbnailUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    // 비디오 썸네일 로딩
    LaunchedEffect(mediaItem.id) {
        if (mediaItem.mimeType.startsWith("video/")) {
            thumbnailUri = mediaStoreUtil.getVideoThumbnail(mediaItem)
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 비디오 썸네일 표시
        FullScreenImage(
            data = thumbnailUri ?: mediaItem.uri,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        
        // 재생 버튼 오버레이
        Card(
            modifier = Modifier
                .size(80.dp)
                .clickable { onPlayClick() },
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "영상 재생",
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun ZoomableImage(
    data: Any,
    modifier: Modifier = Modifier,
    isCurrentPage: Boolean = true,
    onZoomChanged: (Boolean) -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var lastPanPosition by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    
    // 상수들
    val minScale = 1f
    val maxScale = 3f
    val zoomScale = 2f
    
    // 페이지가 변경되면 줌 상태 초기화
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            scale = minScale
            offset = Offset.Zero
            lastPanPosition = Offset.Zero
            isDragging = false
            onZoomChanged(false)
        }
    }
    
    // 줌 상태 알림
    LaunchedEffect(scale) {
        onZoomChanged(scale > minScale)
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        FullScreenImage(
            data = data,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .pointerInput(Unit) {
                    // 통합된 제스처 처리
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            
                            if (!isCurrentPage) continue
                            
                            val touchPoints = event.changes
                            
                            when {
                                // 두 손가락 핀치 줌 처리
                                touchPoints.size >= 2 -> {
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()
                                    val centroid = event.calculateCentroid()
                                    
                                    val newScale = (scale * zoomChange).coerceIn(minScale, maxScale)
                                    
                                    if (newScale > minScale) {
                                        scale = newScale
                                        
                                        // 팬 처리 개선 - 더 빠른 반응
                                        val maxOffsetX = (size.width * (scale - 1)) / 2f
                                        val maxOffsetY = (size.height * (scale - 1)) / 2f
                                        val oldOffset = offset
                                        offset = Offset(
                                            x = (offset.x + panChange.x).coerceIn(-maxOffsetX, maxOffsetX),
                                            y = (offset.y + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
                                        )
                                        

                                    } else {
                                        scale = minScale
                                        offset = Offset.Zero
                                    }
                                    
                                    // 핀치줌 중에는 드래그 상태 리셋
                                    isDragging = false
                                }
                                
                                // 한 손가락 드래그 처리 (줌 상태에서만)
                                touchPoints.size == 1 && scale > minScale -> {
                                    val change = touchPoints[0]
                                    
                                    when (event.type) {
                                        PointerEventType.Press -> {
                                            lastPanPosition = change.position
                                            isDragging = true
                                        }
                                        PointerEventType.Move -> {
                                            if (isDragging) {
                                                val currentPosition = change.position
                                                val previousPosition = change.previousPosition
                                                val panChange = currentPosition - previousPosition
                                                
                                                if (panChange != Offset.Zero) {
                                                    // 손가락 움직임과 1:1 비율로 드래그 (속도 개선)
                                                    val newOffsetX = offset.x + panChange.x
                                                    val newOffsetY = offset.y + panChange.y
                                                    
                                                    // 이미지 경계를 벗어나지 않도록 제한
                                                    val maxOffsetX = (size.width * (scale - 1)) / 2f
                                                    val maxOffsetY = (size.height * (scale - 1)) / 2f
                                                    
                                                    offset = Offset(
                                                        x = newOffsetX.coerceIn(-maxOffsetX, maxOffsetX),
                                                        y = newOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
                                                    )
                                                }
                                                
                                                // lastPanPosition 업데이트를 Move 이벤트 처리 후에 수행
                                                lastPanPosition = currentPosition
                                            }
                                        }
                                        PointerEventType.Release -> {
                                            isDragging = false
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    // 한 손가락 드래그 처리 (줌 상태에서만)
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            
                            if (!isCurrentPage) continue
                            
                            val touchPoints = event.changes
                            
                            if (touchPoints.size == 1 && scale > minScale) {
                                val change = touchPoints[0]
                                
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        lastPanPosition = change.position
                                        isDragging = true
                                    }
                                    PointerEventType.Move -> {
                                        if (isDragging) {
                                            val currentPosition = change.position
                                            val panChange = currentPosition - lastPanPosition
                                            lastPanPosition = currentPosition
                                            
                                            if (panChange != Offset.Zero) {
                                                // 손가락 움직임과 1:1 비율로 드래그 (속도 개선)
                                                val newOffsetX = offset.x + panChange.x
                                                val newOffsetY = offset.y + panChange.y
                                                
                                                // 이미지 경계를 벗어나지 않도록 제한
                                                val maxOffsetX = (size.width * (scale - 1)) / 2f
                                                val maxOffsetY = (size.height * (scale - 1)) / 2f
                                                
                                                offset = Offset(
                                                    x = newOffsetX.coerceIn(-maxOffsetX, maxOffsetX),
                                                    y = newOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
                                                )
                                            }
                                        }
                                    }
                                    PointerEventType.Release -> {
                                        isDragging = false
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    // 더블 탭 제스처 처리 (한 손가락일 때만)
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            if (isCurrentPage && !isDragging) {
                                if (scale > minScale) {
                                    // 줌 아웃
                                    scale = minScale
                                    offset = Offset.Zero
                                } else {
                                    // 줌 인 (탭한 위치를 중심으로)
                                    scale = zoomScale
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    offset = Offset(
                                        x = (centerX - tapOffset.x) * (scale - 1),
                                        y = (centerY - tapOffset.y) * (scale - 1)
                                    )
                                }
                            }
                        }
                    )
                },
            contentScale = ContentScale.Fit
        )
    }
}

 