package com.lee.quickgallery.ui


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lee.quickgallery.ui.components.FullScreenImage
import com.lee.quickgallery.ui.viewmodel.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    initialMediaUri: String,
    folderPath: String,
    onBackClick: () -> Unit = {},
    viewModel: GalleryViewModel = viewModel()
) {
    val mediaList by viewModel.mediaList.collectAsState()
    
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

@Composable
private fun ZoomableImage(
    data: Any,
    modifier: Modifier = Modifier,
    isCurrentPage: Boolean = true,
    onZoomChanged: (Boolean) -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var pointerCount by remember { mutableStateOf(0) }
    
    // 상수들을 companion object로 이동
    val minScale = 1f
    val maxScale = 5f
    val zoomScale = 2f
    
    // 페이지가 변경되면 줌 상태 초기화
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            scale = minScale
            offset = Offset.Zero
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
                    // 포인터 개수 추적
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                PointerEventType.Press -> {
                                    pointerCount = event.changes.size
                                }
                                PointerEventType.Release -> {
                                    pointerCount = 0
                                }
                                PointerEventType.Move -> {
                                    pointerCount = event.changes.size
                                }
                                else -> {}
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    // 핀치줌과 팬 제스처 처리 (두 손가락일 때만)
                    detectTransformGestures(
                        panZoomLock = true
                    ) { centroid, pan, zoom, _ ->
                        if (isCurrentPage && pointerCount >= 2) {
                            val newScale = (scale * zoom).coerceIn(minScale, maxScale)
                            
                            if (newScale != scale) {
                                // 핀치줌 시 중심점 기준으로 스케일링
                                val newOffset = if (newScale > minScale) {
                                    // 확대 시 중심점을 기준으로 오프셋 조절
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    Offset(
                                        x = (offset.x + (centerX - centroid.x) * (zoom - 1)) * (newScale / scale),
                                        y = (offset.y + (centerY - centroid.y) * (zoom - 1)) * (newScale / scale)
                                    )
                                } else {
                                    // 최소 스케일로 돌아갈 때는 중앙으로
                                    Offset.Zero
                                }
                                
                                scale = newScale
                                offset = newOffset
                            } else if (scale > minScale) {
                                // 팬 제스처 처리 (확대된 상태에서만)
                                val maxOffsetX = (size.width * (scale - 1)) / 2f
                                val maxOffsetY = (size.height * (scale - 1)) / 2f
                                
                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    // 더블 탭 제스처 처리 (한 손가락일 때만)
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            if (isCurrentPage && pointerCount <= 1) {
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

 